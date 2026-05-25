package com.clipvault.app.ui.tagmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clipvault.app.data.local.entity.Tag
import com.clipvault.app.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TagNode(
    val tag: Tag,
    val children: List<TagNode>,
    val isExpanded: Boolean = false,
    val depth: Int = 0
)

@HiltViewModel
class TagManagerViewModel @Inject constructor(
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _tagTree = MutableStateFlow<List<TagNode>>(emptyList())
    val tagTree: StateFlow<List<TagNode>> = _tagTree.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<Long>>(emptySet())

    private val _editDialogState = MutableStateFlow<EditDialogState>(EditDialogState.Hidden)
    val editDialogState: StateFlow<EditDialogState> = _editDialogState.asStateFlow()

    private val _deleteDialogState = MutableStateFlow<DeleteDialogState>(DeleteDialogState.Hidden)
    val deleteDialogState: StateFlow<DeleteDialogState> = _deleteDialogState.asStateFlow()

    private val _moveDialogState = MutableStateFlow<MoveDialogState>(MoveDialogState.Hidden)
    val moveDialogState: StateFlow<MoveDialogState> = _moveDialogState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadTags()
    }

    private fun loadTags() {
        viewModelScope.launch {
            tagRepository.getAllTags().collect { tags ->
                _tagTree.value = buildTree(tags, null, 0)
            }
        }
    }

    private fun buildTree(allTags: List<Tag>, parentId: Long?, depth: Int): List<TagNode> {
        return allTags
            .filter { it.parentId == parentId }
            .sortedBy { it.name }
            .map { tag ->
                TagNode(
                    tag = tag,
                    children = buildTree(allTags, tag.id, depth + 1),
                    isExpanded = _expandedIds.value.contains(tag.id),
                    depth = depth
                )
            }
    }

    fun toggleExpand(tagId: Long) {
        _expandedIds.value = if (_expandedIds.value.contains(tagId)) {
            _expandedIds.value - tagId
        } else {
            _expandedIds.value + tagId
        }
        // Rebuild tree with new expanded state
        viewModelScope.launch {
            val tags = tagRepository.getAllTagsOnce()
            _tagTree.value = buildTree(tags, null, 0)
        }
    }

    fun showCreateDialog(parentId: Long?) {
        _editDialogState.value = EditDialogState.Create(parentId = parentId)
    }

    fun showRenameDialog(tag: Tag) {
        _editDialogState.value = EditDialogState.Rename(tag = tag)
    }

    fun hideEditDialog() {
        _editDialogState.value = EditDialogState.Hidden
    }

    fun createTag(name: String, parentId: Long?) {
        viewModelScope.launch {
            tagRepository.insert(Tag(name = name, parentId = parentId))
            _message.value = "Tag created"
        }
    }

    fun renameTag(tagId: Long, newName: String) {
        viewModelScope.launch {
            tagRepository.getByIdOnce(tagId)?.let { tag ->
                tagRepository.update(tag.copy(name = newName))
                _message.value = "Tag renamed"
            }
        }
    }

    fun showDeleteDialog(tag: Tag) {
        viewModelScope.launch {
            val childCount = tagRepository.getChildrenOnce(tag.id).size
            val itemCount = tagRepository.getItemCountForTag(tag.id)
            _deleteDialogState.value = DeleteDialogState(
                tag = tag,
                childCount = childCount,
                itemCount = itemCount
            )
        }
    }

    fun hideDeleteDialog() {
        _deleteDialogState.value = DeleteDialogState.Hidden
    }

    fun confirmDelete(tagId: Long) {
        viewModelScope.launch {
            tagRepository.deleteTagWithReparenting(tagId)
            _message.value = "Tag deleted"
            hideDeleteDialog()
        }
    }

    fun showMoveDialog(tag: Tag) {
        viewModelScope.launch {
            val allTags = tagRepository.getAllTagsOnce()
            _moveDialogState.value = MoveDialogState(
                tag = tag,
                availableParents = allTags.filter { it.id != tag.id }
            )
        }
    }

    fun hideMoveDialog() {
        _moveDialogState.value = MoveDialogState.Hidden
    }

    fun moveTag(tagId: Long, newParentId: Long?) {
        viewModelScope.launch {
            val result = tagRepository.moveTag(tagId, newParentId)
            if (result.isSuccess) {
                _message.value = "Tag moved"
            } else {
                _message.value = "Cannot move tag: ${result.exceptionOrNull()?.message}"
            }
            hideMoveDialog()
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

sealed interface EditDialogState {
    data object Hidden : EditDialogState
    data class Create(val parentId: Long?) : EditDialogState
    data class Rename(val tag: Tag) : EditDialogState
}

data class DeleteDialogState(
    val tag: Tag,
    val childCount: Int,
    val itemCount: Int
) {
    companion object {
        val Hidden = DeleteDialogState(Tag(name = ""), 0, 0)
    }
}

data class MoveDialogState(
    val tag: Tag,
    val availableParents: List<Tag>
) {
    companion object {
        val Hidden = MoveDialogState(Tag(name = ""), emptyList())
    }
}
