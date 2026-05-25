package com.clipvault.app.ui.detail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.clipvault.app.data.local.entity.ClipItem
import com.clipvault.app.data.local.AppDatabase
import com.clipvault.app.data.local.entity.Tag
import com.clipvault.app.data.repository.ClipItemRepository
import com.clipvault.app.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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

sealed interface AiState {
    data object Idle : AiState
    data object Loading : AiState
    data class Success(val summary: String, val suggestedTags: List<String>) : AiState
    data class Error(val message: String) : AiState
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val clipItemRepository: ClipItemRepository,
    private val tagRepository: TagRepository,
    private val aiProviderRepository: com.clipvault.app.data.repository.AiProviderRepository,
    private val aiService: com.clipvault.app.data.remote.AiService
) : ViewModel() {

    private val itemId: Long = savedStateHandle["id"] ?: 0L

    private val _item = MutableStateFlow<ClipItem?>(null)
    val item: StateFlow<ClipItem?> = _item.asStateFlow()
 
    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()
 
    private val _allTags = MutableStateFlow<List<Tag>>(emptyList())
    val allTags: StateFlow<List<Tag>> = _allTags.asStateFlow()

    private val _tagPaths = MutableStateFlow<Map<Long, String>>(emptyMap())
    val tagPaths: StateFlow<Map<Long, String>> = _tagPaths.asStateFlow()
 
    private val _fetchState = MutableStateFlow<FetchState>(FetchState.Idle)
    val fetchState: StateFlow<FetchState> = _fetchState.asStateFlow()
 
    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()

    private val _playingUri = MutableStateFlow<String?>(null)
    val playingUri: StateFlow<String?> = _playingUri.asStateFlow()
 
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
 
    init {
        loadItem()
        loadAllTags()
    }
 
    /**
     * Load item and its tags using independent coroutines.
     * Previously used nested collect which blocked the outer Flow.
     */
    private fun loadItem() {
        // Load item separately
        viewModelScope.launch {
            clipItemRepository.getById(itemId)
                .catch { /* swallow DB errors to prevent crash */ }
                .collectLatest { clipItem ->
                    _item.value = clipItem
                }
        }
        // Load tags for this item independently
        viewModelScope.launch {
            clipItemRepository.getTagsForItem(itemId)
                .catch { _tags.value = emptyList() }
                .collectLatest { tagList ->
                    _tags.value = tagList
                }
        }
    }
 
    private fun loadAllTags() {
        viewModelScope.launch {
            tagRepository.getAllTags()
                .catch { _allTags.value = emptyList() }
                .collectLatest { tagsList ->
                    _allTags.value = tagsList
                    _tagPaths.value = calculateTagPaths(tagsList)
                }
        }
    }

    private fun calculateTagPaths(allTagsList: List<Tag>): Map<Long, String> {
        val tagMap = allTagsList.associateBy { it.id }
        val paths = mutableMapOf<Long, String>()
        for (tag in allTagsList) {
            val path = mutableListOf<String>()
            var current: Tag? = tag
            var safety = 50
            while (current != null && safety-- > 0) {
                path.add(current.name)
                current = tagMap[current.parentId]
            }
            paths[tag.id] = path.reversed().joinToString("/")
        }
        return paths
    }
 
    fun addTag(tagId: Long) {
        viewModelScope.launch {
            clipItemRepository.addTagToItem(itemId, tagId)
            // Flow will auto-update _tags via getTagsForItem
        }
    }
 
    fun removeTag(tagId: Long) {
        viewModelScope.launch {
            clipItemRepository.removeTagFromItem(itemId, tagId)
            // Flow will auto-update _tags via getTagsForItem
        }
    }
 
    fun fetchLinkContent(url: String) {
        viewModelScope.launch {
            _fetchState.value = FetchState.Loading
            try {
                val content = withContext(Dispatchers.IO) {
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
            } catch (e: Exception) {
                _fetchState.value = FetchState.Error("未知错误: ${e.message}")
            }
        }
    }
 
    fun playMedia(filePath: String) {
        _playingUri.value = filePath
        val mediaItem = MediaItem.fromUri(filePath)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
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
 
    fun analyzeContent() {
        val currentItem = _item.value ?: return
        viewModelScope.launch {
            _aiState.value = AiState.Loading
            
            val activeProvider = aiProviderRepository.getActiveProviderWithApiKey()
            if (activeProvider == null) {
                _aiState.value = AiState.Error("未配置或未激活 AI 服务，请到设置页面配置")
                return@launch
            }
            if (activeProvider.apiKey.isBlank()) {
                _aiState.value = AiState.Error("AI 服务密钥 (API Key) 为空")
                return@launch
            }
 
            // Decide content to send
            val firstLink = currentItem.attachments.firstOrNull { it.type == "link" }
            val firstImage = currentItem.attachments.firstOrNull { it.type == "image" }
            
            val contentToSend = when {
                firstLink != null -> {
                    if (currentItem.fetchedContent.isNotBlank()) {
                        "URL: ${firstLink.filePath}\nFetched content:\n${currentItem.fetchedContent}"
                    } else {
                        firstLink.filePath
                    }
                }
                else -> currentItem.content
            }
 
            val result = aiService.analyze(
                provider = activeProvider,
                apiKey = activeProvider.apiKey,
                content = contentToSend,
                contentType = if (firstLink != null) "link" else if (firstImage != null) "image" else "text",
                imagePath = firstImage?.filePath
            )
 
            _aiState.value = when (result) {
                is com.clipvault.app.data.remote.AiResult.Success -> AiState.Success(result.summary, result.suggestedTags)
                is com.clipvault.app.data.remote.AiResult.Error -> AiState.Error(result.message)
            }
        }
    }
 
    fun clearAiState() {
        _aiState.value = AiState.Idle
    }
 
    fun applyAiResult(summary: String, selectedTags: List<String>) {
        val currentItem = _item.value ?: return
        viewModelScope.launch {
            val currentContent = currentItem.content
            val updatedContent = if (currentContent.isBlank()) {
                "---\n🤖 AI 总结:\n$summary"
            } else {
                "$currentContent\n\n---\n🤖 AI 总结:\n$summary"
            }
 
            database.withTransaction {
                clipItemRepository.update(currentItem.copy(content = updatedContent, updatedAt = System.currentTimeMillis()))
 
                // Associate tags
                val existingTags = tagRepository.getAllTagsOnce().toMutableList()
                for (tagName in selectedTags) {
                    val tagId = getOrCreateTagHierarchy(tagName, existingTags)
                    if (tagId != null) {
                        clipItemRepository.addTagToItem(itemId, tagId)
                    }
                }
            }
        }
    }
 
    private suspend fun getOrCreateTagHierarchy(tagPath: String, existingTags: MutableList<Tag>): Long? {
        val segments = tagPath.split('/')
        var currentParentId: Long? = null
        var lastTagId: Long? = null
 
        for (segment in segments) {
            val name = segment.trim()
            if (name.isBlank()) continue
 
            // Find match
            val match = existingTags.find { it.name.equals(name, ignoreCase = true) && it.parentId == currentParentId }
            if (match != null) {
                currentParentId = match.id
                lastTagId = match.id
            } else {
                val newTag = Tag(name = name, parentId = currentParentId)
                val newId = tagRepository.insert(newTag)
                val createdTag = newTag.copy(id = newId)
                existingTags.add(createdTag)
                currentParentId = newId
                lastTagId = newId
            }
        }
        return lastTagId
    }
 
    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }
}
