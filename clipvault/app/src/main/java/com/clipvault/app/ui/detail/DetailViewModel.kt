package com.clipvault.app.ui.detail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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
import org.jsoup.Jsoup
import javax.inject.Inject

sealed interface FetchState {
    data object Idle : FetchState
    data object Loading : FetchState
    data class Success(val content: String) : FetchState
    data class Error(val message: String) : FetchState
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val clipItemRepository: ClipItemRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val itemId: Long = savedStateHandle["id"] ?: 0L

    private val _item = MutableStateFlow<ClipItem?>(null)
    val item: StateFlow<ClipItem?> = _item.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    private val _allTags = MutableStateFlow<List<Tag>>(emptyList())
    val allTags: StateFlow<List<Tag>> = _allTags.asStateFlow()

    private val _fetchState = MutableStateFlow<FetchState>(FetchState.Idle)
    val fetchState: StateFlow<FetchState> = _fetchState.asStateFlow()

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    init {
        loadItem()
        loadAllTags()
    }

    private fun loadItem() {
        viewModelScope.launch {
            clipItemRepository.getById(itemId).collect { clipItem ->
                _item.value = clipItem
                clipItem?.let {
                    clipItemRepository.getTagsForItem(itemId).collect { tagList ->
                        _tags.value = tagList
                    }
                }
            }
        }
    }

    private fun loadAllTags() {
        viewModelScope.launch {
            tagRepository.getAllTags().collect {
                _allTags.value = it
            }
        }
    }

    fun updateNote(note: String) {
        viewModelScope.launch {
            _item.value?.let { current ->
                clipItemRepository.update(current.copy(note = note, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun addTag(tagId: Long) {
        viewModelScope.launch {
            clipItemRepository.addTagToItem(itemId, tagId)
        }
    }

    fun removeTag(tagId: Long) {
        viewModelScope.launch {
            clipItemRepository.removeTagFromItem(itemId, tagId)
        }
    }

    fun fetchLinkContent() {
        val currentItem = _item.value ?: return
        if (currentItem.type != "link") return

        viewModelScope.launch {
            _fetchState.value = FetchState.Loading
            try {
                val content = withContext(Dispatchers.IO) {
                    val url = currentItem.content
                    val doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                        .timeout(15000)
                        .maxBodySize(2 * 1024 * 1024) // 2MB
                        .followRedirects(true)
                        .ignoreHttpErrors(true)
                        .get()

                    // Try article/main/role=main first
                    val mainContent = doc.select("article, main, [role=main]").text()
                    val text = if (mainContent.length >= 50) mainContent else doc.body().text()

                    if (text.length < 50) {
                        "该页面可能需要 JavaScript 渲染，AI 分析将仅基于 URL"
                    } else {
                        text
                    }
                }
                _fetchState.value = FetchState.Success(content)
                // Save to database
                _item.value?.let { current ->
                    clipItemRepository.update(current.copy(fetchedContent = content, updatedAt = System.currentTimeMillis()))
                }
            } catch (e: java.net.SocketTimeoutException) {
                _fetchState.value = FetchState.Error("页面加载超时")
            } catch (e: org.jsoup.HttpStatusException) {
                _fetchState.value = FetchState.Error("无法访问该页面 (HTTP ${e.statusCode})")
            } catch (e: org.jsoup.UnsupportedMimeTypeException) {
                _fetchState.value = FetchState.Error("该链接不是网页")
            } catch (e: java.io.IOException) {
                _fetchState.value = FetchState.Error("网络错误")
            }
        }
    }

    fun setupPlayer() {
        val currentItem = _item.value ?: return
        if (currentItem.type != "media") return

        val mediaItem = MediaItem.fromUri(currentItem.content)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    fun play() {
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun deleteItem() {
        viewModelScope.launch {
            _item.value?.let { clipItemRepository.delete(it) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }
}
