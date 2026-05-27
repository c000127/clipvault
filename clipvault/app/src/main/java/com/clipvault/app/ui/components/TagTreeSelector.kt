package com.clipvault.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import com.clipvault.app.ui.theme.BentoAsymmetricCardShape
import com.clipvault.app.ui.theme.PillShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clipvault.app.data.local.entity.Tag

@Composable
fun TagTreeSelector(
    allTags: List<Tag>,
    selectedTagIds: Set<Long>,
    tagPaths: Map<Long, String>,
    onTagToggle: (tagId: Long, selected: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    
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

    val childrenMap = remember(allTags) {
        val map = mutableMapOf<Long?, MutableList<Tag>>()
        allTags.forEach { tag ->
            map.getOrPut(tag.parentId) { mutableListOf() }.add(tag)
        }
        map
    }

    fun matchesSearch(tag: Tag): Boolean {
        if (searchQuery.isBlank()) return true
        if (tag.name.contains(searchQuery, ignoreCase = true)) return true
        val children = childrenMap[tag.id] ?: emptyList()
        return children.any { matchesSearch(it) }
    }

    fun isAnyDescendantSelected(tagId: Long): Boolean {
        val children = childrenMap[tagId] ?: emptyList()
        return children.any { child ->
            selectedTagIds.contains(child.id) || isAnyDescendantSelected(child.id)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar with large corners
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter identity tags...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.Transparent,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )

        val rootTags = (childrenMap[null] ?: emptyList()).filter { matchesSearch(it) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            rootTags.forEach { tag ->
                TagTreeNode(
                    tag = tag,
                    childrenMap = childrenMap,
                    selectedTagIds = selectedTagIds,
                    expandedIds = expandedIds,
                    onExpandToggle = { id -> expandedIds[id] = !(expandedIds[id] ?: false) },
                    onTagToggle = onTagToggle,
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
    isAnyDescendantSelected: (Long) -> Boolean,
    matchesSearch: (Tag) -> Boolean,
    depth: Int
) {
    val children = (childrenMap[tag.id] ?: emptyList()).filter { matchesSearch(it) }
    val hasChildren = children.isNotEmpty()
    val isExpanded = expandedIds[tag.id] ?: false
    val isSelected = selectedTagIds.contains(tag.id)
    val hasSelectedDescendant = isAnyDescendantSelected(tag.id)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 16).dp)
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minimalist hierarchy guide
            if (depth > 0) {
                Box(modifier = Modifier.width(12.dp).fillMaxHeight()) {
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().align(Alignment.Center).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                }
            }

            Surface(
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    hasSelectedDescendant -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                    else -> Color.Transparent
                },
                shape = PillShape,
                modifier = Modifier.weight(1f).padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onTagToggle(tag.id, !isSelected) }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasChildren) {
                        IconButton(onClick = { onExpandToggle(tag.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(24.dp))
                    }

                    Text(
                        text = tag.name,
                        style = if (isSelected) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )

                    if (isSelected) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (hasChildren && isExpanded) {
            children.forEach { child ->
                TagTreeNode(child, childrenMap, selectedTagIds, expandedIds, onExpandToggle, onTagToggle, isAnyDescendantSelected, matchesSearch, depth + 1)
            }
        }
    }
}
