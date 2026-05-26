package com.clipvault.app.ui.newitem
 
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clipvault.app.data.local.entity.ClipItem
import com.clipvault.app.data.local.entity.Tag
import com.clipvault.app.data.local.entity.ContentAttachment
import com.clipvault.app.data.repository.ClipItemRepository
import com.clipvault.app.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
 
    private val _attachments = MutableStateFlow<List<ContentAttachment>>(emptyList())
    val attachments: StateFlow<List<ContentAttachment>> = _attachments.asStateFlow()
 
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
        android.util.Log.d("NewItemVM", "init start: content empty=${_content.value.isEmpty()}, tags empty=${_allTags.value.isEmpty()}")
        try {
            viewModelScope.launch {
                tagRepository.getAllTags()
                    .catch { e ->
                        android.util.Log.e("NewItemVM", "load tags failed", e)
                        _allTags.value = emptyList()
                    }
                    .collect {
                        _allTags.value = it
                        android.util.Log.d("NewItemVM", "loaded ${it.size} tags")
                    }
            }
        } catch (e: Exception) {
            android.util.Log.e("NewItemVM", "init crash", e)
        }
    }
 
    fun setContent(text: String) {
        _content.value = text
        // URL 自动检测逻辑
        if (text.isNotBlank() && (text.startsWith("http://") || text.startsWith("https://")) && !text.contains(" ")) {
            autoFetchUrl(text)
        }
    }

    private var lastFetchedUrl: String? = null
    
    private fun autoFetchUrl(url: String) {
        if (url == lastFetchedUrl) return
        lastFetchedUrl = url
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val doc = org.jsoup.Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .timeout(10000)
                    .get()
                
                val title = doc.title()
                val mainContent = doc.select("article, main, [role=main]").text()
                val text = if (mainContent.length >= 50) mainContent else doc.body().text()
                
                withContext(Dispatchers.Main) {
                    if (title.isNotBlank()) {
                        // 自动将链接转化为附件
                        val existingLink = _attachments.value.any { it.type == "link" && it.filePath == url }
                        if (!existingLink) {
                            val linkAttachment = ContentAttachment(
                                itemId = 0L,
                                type = "link",
                                filePath = url,
                                thumbnailPath = "",
                                orderIndex = _attachments.value.size
                            )
                            _attachments.value = _attachments.value + linkAttachment
                        }
                        
                        // 如果内容框只有 URL，追加标题
                        if (_content.value == url) {
                            _content.value = "$title\n\n$url"
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("NewItemVM", "Auto fetch failed", e)
            }
        }
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
                val type = when {
                    mimeType?.startsWith("image/") == true -> "image"
                    mimeType?.startsWith("video/") == true -> "media"
                    mimeType?.startsWith("audio/") == true -> "media"
                    else -> "file"
                }
                val newAttachment = ContentAttachment(
                    itemId = 0L,
                    type = type,
                    filePath = fileName,
                    thumbnailPath = if (type == "image") fileName else "",
                    orderIndex = _attachments.value.size
                )
                _attachments.value = _attachments.value + newAttachment
            } catch (e: Exception) {
                _errorMessage.value = "Failed to copy file: ${e.message}"
            }
        }
    }

    fun removeAttachment(index: Int) {
        val list = _attachments.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _attachments.value = list.mapIndexed { idx, attachment ->
                attachment.copy(orderIndex = idx)
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
        val currentAttachments = _attachments.value
 
        if (currentContent.isBlank() && currentAttachments.isEmpty()) {
            _errorMessage.value = "Content or attachments cannot be empty"
            return
        }
 
        _isSaving.value = true
 
        viewModelScope.launch {
            try {
                val firstImage = currentAttachments.firstOrNull { it.type == "image" }
                val thumbnail = firstImage?.filePath ?: ""

                val item = ClipItem(
                    type = "mixed",
                    content = currentContent,
                    thumbnailPath = thumbnail,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
 
                clipItemRepository.insertWithTagsAndAttachments(item, _selectedTags.value, currentAttachments)
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
