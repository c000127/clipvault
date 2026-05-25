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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val clipItemRepository: ClipItemRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTagId = MutableStateFlow<Long?>(null)
    val selectedTagId: StateFlow<Long?> = _selectedTagId.asStateFlow()

    private val _allTags = MutableStateFlow<List<Tag>>(emptyList())
    val allTags: StateFlow<List<Tag>> = _allTags.asStateFlow()

    val items: Flow<PagingData<ClipItem>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                Pager(
                    config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                    pagingSourceFactory = { clipItemRepository.getAllPaged() }
                ).flow
            } else {
                Pager(
                    config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                    pagingSourceFactory = { clipItemRepository.search(query) }
                ).flow
            }
        }
        .cachedIn(viewModelScope)

    val filteredItems: Flow<PagingData<ClipItem>> = _selectedTagId
        .flatMapLatest { tagId ->
            if (tagId == null) {
                Pager(
                    config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                    pagingSourceFactory = { clipItemRepository.getAllPaged() }
                ).flow
            } else {
                Pager(
                    config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                    pagingSourceFactory = { clipItemRepository.getItemsByTagWithChildren(tagId) }
                ).flow
            }
        }
        .cachedIn(viewModelScope)

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

    fun onTagSelected(tagId: Long?) {
        _selectedTagId.value = tagId
    }

    fun deleteItems(ids: List<Long>) {
        viewModelScope.launch {
            clipItemRepository.deleteByIds(ids)
        }
    }
}
