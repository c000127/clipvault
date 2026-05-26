package com.clipvault.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import com.clipvault.app.ui.theme.BentoAsymmetricCardShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clipvault.app.data.local.entity.Tag

/**
 * A hierarchical tag tree selector composable.
 * Displays tags in a tree structure with expand/collapse and multi-select checkboxes.
 *
 * @param allTags List of all available tags
 * @param selectedTagIds Set of currently selected tag IDs
 * @param tagPaths Map from tag ID to its full hierarchical path
 * @param onTagToggle Called when a tag is selected or deselected
 */
@Composable
fun TagTreeSelector(
    allTags: List<Tag>,
    selectedTagIds: Set<Long>,
    tagPaths: Map<Long, String>,
    onTagToggle: (tagId: Long, selected: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Track expanded status
    val expandedIds = remember(allTags, selectedTagIds) {
        val map = mutableStateMapOf<Long, Boolean>()
        val parentMap = allTags.associateBy { it.id }
        for (selectedId in selectedTagIds) {
            val tag = allTags.find { it.id == selectedId } ?: continue
            var currentParentId = tag.parentId
            while (currentParentId != null) {
                map[currentParentId] = true
                currentParentId = parentMap[currentParentId]?.parentId
            }
        }
        map
    }

    // Build children mapping
    val childrenMap = remember(allTags) {
        val map = mutableMapOf<Long?, MutableList<Tag>>()
        allTags.forEach { tag ->
            map.getOrPut(tag.parentId) { mutableListOf() }.add(tag)
        }
        map
    }

    // Helper to check if a tag matches search or has any descendant matching search
    fun matchesSearch(tag: Tag): Boolean {
        if (searchQuery.isBlank()) return true
        if (tag.name.contains(searchQuery, ignoreCase = true)) return true
        val children = childrenMap[tag.id] ?: emptyList()
        return children.any { matchesSearch(it) }
    }

    // Helper to get total selected descendants count
    fun getSelectedDescendantsCount(tagId: Long): Int {
        val children = childrenMap[tagId] ?: emptyList()
        var count = 0
        children.forEach { child ->
            if (selectedTagIds.contains(child.id)) {
                count++
            }
            count += getSelectedDescendantsCount(child.id)
        }
        return count
    }

    // Helper to check if any child is selected (for parent highlighting)
    fun isAnyDescendantSelected(tagId: Long): Boolean {
        val children = childrenMap[tagId] ?: emptyList()
        return children.any { child ->
            selectedTagIds.contains(child.id) || isAnyDescendantSelected(child.id)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Search bar inside selector
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search tags...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = MaterialTheme.shapes.medium
        )

        val rootTags = childrenMap[null] ?: emptyList()
        val filteredRootTags = remember(rootTags, searchQuery) {
            rootTags.filter { matchesSearch(it) }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            filteredRootTags.forEach { tag ->
                TagTreeNode(
                    tag = tag,
                    childrenMap = childrenMap,
                    selectedTagIds = selectedTagIds,
                    expandedIds = expandedIds,
                    onExpandToggle = { tagId ->
                        expandedIds[tagId] = !(expandedIds[tagId] ?: false)
                    },
                    onTagToggle = onTagToggle,
                    getSelectedDescendantsCount = ::getSelectedDescendantsCount,
                    isAnyDescendantSelected = ::isAnyDescendantSelected,
                    matchesSearch = ::matchesSearch,
                    depth = 0
                )
            }
        }
    }
}

@Composable
private fun TagTreeNode(
    tag: Tag,
    childrenMap: Map<Long?, List<Tag>>,
    selectedTagIds: Set<Long>,
    expandedIds: androidx.compose.runtime.snapshots.SnapshotStateMap<Long, Boolean>,
    onExpandToggle: (Long) -> Unit,
    onTagToggle: (tagId: Long, selected: Boolean) -> Unit,
    getSelectedDescendantsCount: (Long) -> Int,
    isAnyDescendantSelected: (Long) -> Boolean,
    matchesSearch: (Tag) -> Boolean,
    depth: Int
) {
    val children = childrenMap[tag.id] ?: emptyList()
    val filteredChildren = children.filter { matchesSearch(it) }
    val hasChildren = filteredChildren.isNotEmpty()
    val isExpanded = expandedIds[tag.id] ?: false
    val isSelected = selectedTagIds.contains(tag.id)
    
    // Parent highlight background if descendant is selected
    val isParentHighlighted = !isSelected && isAnyDescendantSelected(tag.id)
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        isParentHighlighted -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 12).dp)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vertical lines for hierarchy
            repeat(depth) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .align(Alignment.Center)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )
                }
            }

            Surface(
                color = backgroundColor,
                shape = BentoAsymmetricCardShape,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp, horizontal = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTagToggle(tag.id, !isSelected) }
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Expand/collapse icon
                    if (hasChildren) {
                        IconButton(
                            onClick = { onExpandToggle(tag.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked -> onTagToggle(tag.id, checked) }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    val selectedDescendants = getSelectedDescendantsCount(tag.id)
                    val labelText = if (selectedDescendants > 0 && !isExpanded) {
                        "${tag.name} ($selectedDescendants)"
                    } else {
                        tag.name
                    }

                    Text(
                        text = labelText,
                        style = if (depth == 0) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Render children if expanded
        if (hasChildren && isExpanded) {
            filteredChildren.forEach { child ->
                TagTreeNode(
                    tag = child,
                    childrenMap = childrenMap,
                    selectedTagIds = selectedTagIds,
                    expandedIds = expandedIds,
                    onExpandToggle = onExpandToggle,
                    onTagToggle = onTagToggle,
                    getSelectedDescendantsCount = getSelectedDescendantsCount,
                    isAnyDescendantSelected = isAnyDescendantSelected,
                    matchesSearch = matchesSearch,
                    depth = depth + 1
                )
            }
        }
    }
}
