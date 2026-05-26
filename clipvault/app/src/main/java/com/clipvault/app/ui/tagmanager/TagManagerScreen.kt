package com.clipvault.app.ui.tagmanager

import com.clipvault.app.data.local.entity.Tag
import com.clipvault.app.ui.theme.BentoAsymmetricCardShape
import com.clipvault.app.ui.theme.PillShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagerScreen(
    onBack: () -> Unit,
    viewModel: TagManagerViewModel = hiltViewModel()
) {
    val tagTree by viewModel.tagTree.collectAsState()
    val editDialogState by viewModel.editDialogState.collectAsState()
    val deleteDialogState by viewModel.deleteDialogState.collectAsState()
    val moveDialogState by viewModel.moveDialogState.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Edit/Create dialog
    when (val state = editDialogState) {
        is EditDialogState.Create -> {
            TagEditDialog(
                title = "Create Tag",
                initialName = "",
                onConfirm = { name ->
                    viewModel.createTag(name, state.parentId)
                    viewModel.hideEditDialog()
                },
                onDismiss = { viewModel.hideEditDialog() }
            )
        }
        is EditDialogState.Rename -> {
            TagEditDialog(
                title = "Rename Tag",
                initialName = state.tag.name,
                onConfirm = { name ->
                    viewModel.renameTag(state.tag.id, name)
                    viewModel.hideEditDialog()
                },
                onDismiss = { viewModel.hideEditDialog() }
            )
        }
        is EditDialogState.Hidden -> {}
    }

    // Delete dialog
    if (deleteDialogState.tag.id != 0L && deleteDialogState != DeleteDialogState.Hidden) {
        DeleteConfirmDialog(
            tagName = deleteDialogState.tag.name,
            childCount = deleteDialogState.childCount,
            itemCount = deleteDialogState.itemCount,
            onConfirm = { viewModel.confirmDelete(deleteDialogState.tag.id) },
            onDismiss = { viewModel.hideDeleteDialog() }
        )
    }

    // Move dialog
    val moveState = moveDialogState
    if (moveState.tag.id != 0L && moveState != MoveDialogState.Hidden) {
        MoveTagDialog(
            tagName = moveState.tag.name,
            availableParents = moveState.availableParents,
            onSelect = { newParentId ->
                viewModel.moveTag(moveState.tag.id, newParentId)
            },
            onDismiss = { viewModel.hideMoveDialog() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Tag Manager") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog(null) },
                shape = PillShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Tag")
            }
        }
    ) { innerPadding ->
        if (tagTree.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No tags yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Tap + to create your first tag",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                tagTree.forEach { node ->
                    renderTagNode(
                        node = node,
                        onToggleExpand = { viewModel.toggleExpand(it) },
                        onAddChild = { viewModel.showCreateDialog(it) },
                        onRename = { viewModel.showRenameDialog(it) },
                        onDelete = { viewModel.showDeleteDialog(it) },
                        onMove = { viewModel.showMoveDialog(it) }
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.renderTagNode(
    node: TagNode,
    onToggleExpand: (Long) -> Unit,
    onAddChild: (Long) -> Unit,
    onRename: (Tag) -> Unit,
    onDelete: (Tag) -> Unit,
    onMove: (Tag) -> Unit
) {
    item(key = node.tag.id) {
        TagRow(
            node = node,
            onToggleExpand = { onToggleExpand(node.tag.id) },
            onAddChild = { onAddChild(node.tag.id) },
            onRename = { onRename(node.tag) },
            onDelete = { onDelete(node.tag) },
            onMove = { onMove(node.tag) }
        )
    }
    if (node.isExpanded) {
        node.children.forEach { child ->
            renderTagNode(
                node = child,
                onToggleExpand = onToggleExpand,
                onAddChild = onAddChild,
                onRename = onRename,
                onDelete = onDelete,
                onMove = onMove
            )
        }
    }
}

@Composable
private fun TagRow(
    node: TagNode,
    onToggleExpand: () -> Unit,
    onAddChild: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit
) {
    val depth = node.depth
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 12).dp) // Reduced base indentation to fit lines
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical lines for hierarchy
        repeat(depth) { d ->
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .fillMaxHeight()
            ) {
                // Vertical line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .align(Alignment.Center)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
        }

        ElevatedCard(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp, horizontal = 4.dp),
            shape = BentoAsymmetricCardShape,
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (node.children.isNotEmpty() && node.isExpanded)
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand/collapse icon
                if (node.children.isNotEmpty()) {
                    Icon(
                        imageVector = if (node.isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight,
                        contentDescription = if (node.isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Tag name
                Text(
                    text = node.tag.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    color = if (node.children.isNotEmpty() && node.isExpanded)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                // Action buttons
                IconButton(onClick = onAddChild, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add child", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DriveFileMove, contentDescription = "Move", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun TagEditDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tag name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteConfirmDialog(
    tagName: String,
    childCount: Int,
    itemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Tag") },
        text = {
            Column {
                Text("Are you sure you want to delete \"$tagName\"?")
                if (childCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "$childCount child tag(s) will be moved up one level.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (itemCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$itemCount item association(s) will be removed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MoveTagDialog(
    tagName: String,
    availableParents: List<Tag>,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move \"$tagName\"") },
        text = {
            LazyColumn {
                item {
                    TextButton(
                        onClick = { onSelect(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Move to root level")
                    }
                }
                items(availableParents) { tag ->
                    TextButton(
                        onClick = { onSelect(tag.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(tag.name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
