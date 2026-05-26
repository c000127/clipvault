package com.clipvault.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clipvault.app.data.local.entity.Tag
import com.clipvault.app.ui.theme.ExpressiveBottomSheetShape
import com.clipvault.app.ui.theme.PillShape

private data class FilterTagNode(
    val tag: Tag,
    val children: List<FilterTagNode>,
    val isExpanded: Boolean = false,
    val depth: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagFilterSheet(
    tags: List<Tag>,
    selectedTagIds: Set<Long>,
    onTagToggle: (Long) -> Unit,
    onClearFilter: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var expandedIds by remember { mutableStateOf(emptySet<Long>()) }

    // Helper to build hierarchy
    fun buildTree(allTags: List<Tag>, parentId: Long?, depth: Int): List<FilterTagNode> {
        return allTags
            .filter { it.parentId == parentId }
            .sortedBy { it.name }
            .map { tag ->
                FilterTagNode(
                    tag = tag,
                    children = buildTree(allTags, tag.id, depth + 1),
                    isExpanded = expandedIds.contains(tag.id),
                    depth = depth
                )
            }
    }

    val tagTree = remember(tags, expandedIds) {
        buildTree(tags, null, 0)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = ExpressiveBottomSheetShape
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Filter by Tags",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Tag list
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
            ) {
                if (tagTree.isEmpty()) {
                    item {
                        Text(
                            text = "No tags available. Add tags to start filtering.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    fun androidx.compose.foundation.lazy.LazyListScope.renderNode(node: FilterTagNode) {
                        item(key = node.tag.id) {
                            val isSelected = selectedTagIds.contains(node.tag.id)
                            val depth = node.depth
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (node.children.isNotEmpty()) {
                                            expandedIds = if (expandedIds.contains(node.tag.id)) {
                                                expandedIds - node.tag.id
                                            } else {
                                                expandedIds + node.tag.id
                                            }
                                        }
                                    }
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

                                // Toggle Expand icon
                                if (node.children.isNotEmpty()) {
                                    Icon(
                                        imageVector = if (node.isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight,
                                        contentDescription = "Expand",
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable {
                                                expandedIds = if (expandedIds.contains(node.tag.id)) {
                                                    expandedIds - node.tag.id
                                                } else {
                                                    expandedIds + node.tag.id
                                                }
                                            },
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(24.dp))
                                }

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onTagToggle(node.tag.id) }
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Icon(
                                    imageVector = Icons.Default.Label,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = node.tag.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (node.isExpanded) {
                            node.children.forEach { child ->
                                renderNode(child)
                            }
                        }
                    }

                    tagTree.forEach { node ->
                        renderNode(node)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(
                    onClick = {
                        onClearFilter()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Filter")
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = PillShape
                ) {
                    Text("Apply")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

