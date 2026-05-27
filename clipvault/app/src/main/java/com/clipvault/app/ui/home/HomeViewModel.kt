package com.clipvault.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.clipvault.app.data.local.entity.ClipItem
import com.clipvault.app.data.local.entity.Tag
import com.clipvault.app.data.repository.ClipItemRepository
import com.clipvault.app.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.map
import androidx.paging.map
import android.content.Context
import android.content.ClipboardManager
import dagger.hilt.android.qualifiers.ApplicationContext

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clipItemRepository: ClipItemRepository,
    private val tagRepository: TagRepository
) : ViewModel() {
 
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _clipboardSuggestion = MutableStateFlow<String?>(null)
    val clipboardSuggestion: StateFlow<String?> = _clipboardSuggestion.asStateFlow()

    private var lastCheckedClip: String? = null
 
    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds: StateFlow<Set<Long>> = _selectedTagIds.asStateFlow()

    private val _selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItemIds: StateFlow<Set<Long>> = _selectedItemIds.asStateFlow()
 
    private val _allTags = MutableStateFlow<List<Tag>>(emptyList())
    val allTags: StateFlow<List<Tag>> = _allTags.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)
 
    val items: Flow<PagingData<ClipItem>> = combine(_searchQuery, _selectedTagIds, _refreshTrigger) { query, tagIds, _ ->
        Pair(query, tagIds)
    }
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { (query, tagIds) ->
            val flow = if (query.isBlank() && tagIds.isEmpty()) {
                Pager(
                    config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                    pagingSourceFactory = { clipItemRepository.getAllPaged() }
                ).flow
            } else if (query.isBlank() && tagIds.isNotEmpty()) {
                Pager(
                    config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                    pagingSourceFactory = { clipItemRepository.getItemsByTagsWithChildren(tagIds.toList()) }
                ).flow
            } else if (query.isNotBlank() && tagIds.isEmpty()) {
                Pager(
                    config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                    pagingSourceFactory = { clipItemRepository.search(query) }
                ).flow
            } else {
                Pager(
                    config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                    pagingSourceFactory = { clipItemRepository.getItemsByTagsAndSearchWithChildren(tagIds.toList(), query) }
                ).flow
            }
            flow.map { pagingData ->
                pagingData.map { item ->
                    item.apply {
                        attachments = clipItemRepository.getAttachmentsForItemOnce(item.id)
                        tags = clipItemRepository.getTagsForItemOnce(item.id)
                    }
                }
            }
        }
        .cachedIn(viewModelScope)

    fun refresh() {
        _refreshTrigger.value = _refreshTrigger.value + 1
        checkClipboard()
    }

    fun checkClipboard() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip?.getItemAt(0)?.text?.toString()
        if (!clip.isNullOrBlank() && clip != lastCheckedClip) {
            lastCheckedClip = clip
            viewModelScope.launch {
                val recent = clipItemRepository.getRecent(50)
                if (recent.none { it.content == clip }) {
                    _clipboardSuggestion.value = clip
                }
            }
        }
    }

    fun dismissSuggestion() {
        _clipboardSuggestion.value = null
    }

    fun saveSuggestion() {
        val text = _clipboardSuggestion.value ?: return
        _clipboardSuggestion.value = null
        viewModelScope.launch {
            val item = ClipItem(
                content = text,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            clipItemRepository.insertWithTagsAndAttachments(item, emptyList(), emptyList())
            refresh()
        }
    }

    init {
        loadTags()
    }

    private fun loadTags() {
        viewModelScope.launch {
            tagRepository.getAllTags()
                .catch { _allTags.value = emptyList() }
                .collectLatest { tags ->
                    _allTags.value = tags
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleTagSelection(tagId: Long) {
        _selectedTagIds.value = if (_selectedTagIds.value.contains(tagId)) {
            _selectedTagIds.value - tagId
        } else {
            _selectedTagIds.value + tagId
        }
    }

    fun clearTagSelection() {
        _selectedTagIds.value = emptySet()
    }

    fun toggleItemSelection(id: Long) {
        _selectedItemIds.value = if (_selectedItemIds.value.contains(id)) {
            _selectedItemIds.value - id
        } else {
            _selectedItemIds.value + id
        }
    }

    fun clearItemSelection() {
        _selectedItemIds.value = emptySet()
    }

    fun deleteSelectedItems() {
        val ids = _selectedItemIds.value.toList()
        viewModelScope.launch {
            clipItemRepository.deleteByIds(ids)
            _selectedItemIds.value = emptySet()
            refresh()
        }
    }
}
