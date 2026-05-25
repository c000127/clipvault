package com.clipvault.app.ui.newitem

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clipvault.app.data.local.entity.ClipItem
import com.clipvault.app.data.local.entity.Tag
import com.clipvault.app.data.repository.ClipItemRepository
import com.clipvault.app.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NewItemViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clipItemRepository: ClipItemRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _type = MutableStateFlow("text")
    val type: StateFlow<String> = _type.asStateFlow()

    private val _filePath = MutableStateFlow("")
    val filePath: StateFlow<String> = _filePath.asStateFlow()

    private val _selectedTags = MutableStateFlow<List<Long>>(emptyList())
    val selectedTags: StateFlow<List<Long>> = _selectedTags.asStateFlow()

    private val _allTags = MutableStateFlow<List<Tag>>(emptyList())
    val allTags: StateFlow<List<Tag>> = _allTags.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            tagRepository.getAllTags().collect {
                _allTags.value = it
            }
        }
    }

    fun setContent(text: String) {
        _content.value = text
    }

    fun setType(type: String) {
        _type.value = type
    }

    fun toggleTag(tagId: Long) {
        _selectedTags.value = if (_selectedTags.value.contains(tagId)) {
            _selectedTags.value.filter { it != tagId }
        } else {
            _selectedTags.value + tagId
        }
    }

    fun createTag(name: String, parentId: Long?) {
        viewModelScope.launch {
            tagRepository.insert(Tag(name = name, parentId = parentId))
        }
    }

    fun copyUriAndSetType(uri: Uri, mimeType: String?) {
        viewModelScope.launch {
            try {
                val fileName = copyUriToPrivateStorage(uri, mimeType)
                _filePath.value = fileName

                _type.value = when {
                    mimeType?.startsWith("image/") == true -> "image"
                    mimeType?.startsWith("video/") == true -> "media"
                    mimeType?.startsWith("audio/") == true -> "media"
                    else -> "text"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to copy file: ${e.message}"
            }
        }
    }

    private suspend fun copyUriToPrivateStorage(uri: Uri, mimeType: String?): String {
        return withContext(Dispatchers.IO) {
            val clipsDir = File(context.filesDir, "clips")
            if (!clipsDir.exists()) clipsDir.mkdirs()

            val ext = getExtensionFromMimeType(mimeType) ?: "bin"
            val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.$ext"
            val destFile = File(clipsDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            destFile.absolutePath
        }
    }

    private fun getExtensionFromMimeType(mimeType: String?): String? {
        return when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "video/mp4" -> "mp4"
            "video/webm" -> "webm"
            "audio/mpeg" -> "mp3"
            "audio/ogg" -> "ogg"
            "audio/wav" -> "wav"
            else -> null
        }
    }

    fun save() {
        val currentContent = _content.value
        val currentType = _type.value
        val currentFilePath = _filePath.value

        if (currentContent.isBlank() && currentType == "text") {
            _errorMessage.value = "Content cannot be empty"
            return
        }

        _isSaving.value = true

        viewModelScope.launch {
            try {
                val item = ClipItem(
                    type = currentType,
                    content = if (currentType == "text") currentContent else currentFilePath.ifBlank { currentContent },
                    note = "",
                    thumbnailPath = if (currentType == "image") currentFilePath else "",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                clipItemRepository.insertWithTags(item, _selectedTags.value)
                _saved.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
