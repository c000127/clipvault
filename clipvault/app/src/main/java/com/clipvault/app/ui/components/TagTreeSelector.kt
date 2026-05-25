package com.clipvault.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowRight
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
    // Build parent->children map
    val childrenMap = remember(allTags) {
        val map = mutableMapOf<Long?, MutableList<Tag>>()
        allTags.forEach { tag ->
            map.getOrPut(tag.parentId) { mutableListOf() }.add(tag)
        }
        map
    }

    // Track which tags are expanded
    val expandedIds = remember { mutableStateOf(setOf<Long>()) }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Render top-level tags (parentId == null)
        val rootTags = childrenMap[null] ?: emptyList()
        rootTags.forEach { tag ->
            TagTreeNode(
                tag = tag,
                childrenMap = childrenMap,
                selectedTagIds = selectedTagIds,
                expandedIds = expandedIds.value,
                onExpandToggle = { tagId ->
                    expandedIds.value = if (expandedIds.value.contains(tagId)) {
                        expandedIds.value - tagId
                    } else {
                        expandedIds.value + tagId
                    }
                },
                onTagToggle = onTagToggle,
                depth = 0
            )
        }
    }
}

@Composable
private fun TagTreeNode(
    tag: Tag,
    childrenMap: Map<Long?, List<Tag>>,
    selectedTagIds: Set<Long>,
    expandedIds: Set<Long>,
    onExpandToggle: (Long) -> Unit,
    onTagToggle: (tagId: Long, selected: Boolean) -> Unit,
    depth: Int
) {
    val children = childrenMap[tag.id] ?: emptyList()
    val hasChildren = children.isNotEmpty()
    val isExpanded = expandedIds.contains(tag.id)
    val isSelected = selectedTagIds.contains(tag.id)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTagToggle(tag.id, !isSelected) }
            .padding(start = (depth * 16).dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expand/collapse icon
        if (hasChildren) {
            IconButton(
                onClick = { onExpandToggle(tag.id) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = { checked -> onTagToggle(tag.id, checked) }
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = tag.name,
            style = if (depth == 0) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }

    // Render children if expanded
    if (hasChildren && isExpanded) {
        children.forEach { child ->
            TagTreeNode(
                tag = child,
                childrenMap = childrenMap,
                selectedTagIds = selectedTagIds,
                expandedIds = expandedIds,
                onExpandToggle = onExpandToggle,
                onTagToggle = onTagToggle,
                depth = depth + 1
            )
        }
    }
}
