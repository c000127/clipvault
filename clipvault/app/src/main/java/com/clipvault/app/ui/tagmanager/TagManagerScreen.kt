package com.clipvault.app.ui.tagmanager

import com.clipvault.app.data.local.entity.Tag
import com.clipvault.app.ui.theme.BentoAsymmetricCardShape
import com.clipvault.app.ui.theme.PillShape
import com.clipvault.app.ui.theme.ExpressiveBottomSheetShape
// [自适应] 导入自适应布局工具
import com.clipvault.app.ui.adaptive.rememberAdaptiveTokens
// [动效] SharedTransition 支持
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun TagManagerScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
    viewModel: TagManagerViewModel = hiltViewModel()
) {
    val tagTree by viewModel.tagTree.collectAsState()
    val editDialogState by viewModel.editDialogState.collectAsState()
    val deleteDialogState by viewModel.deleteDialogState.collectAsState()
    val moveDialogState by viewModel.moveDialogState.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var activeContextTag by remember { mutableStateOf<Tag?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }

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
                title = if (state.parentId == null) "Create Root Tag" else "Create Sub-tag",
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
                title = "Rename Identity",
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
            onConfirm = { viewModel.confirmDelete(deleteDialogState.tag.id) },
            onDismiss = { viewModel.hideDeleteDialog() }
        )
    }

    // Move dialog
    val moveState = moveDialogState
    if (moveState.tag.id != 0L && moveState != MoveDialogState.Hidden) {
        MoveTagDialog(
            availableParents = moveState.availableParents,
            onSelect = { newParentId ->
                viewModel.moveTag(moveState.tag.id, newParentId)
            },
            onDismiss = { viewModel.hideMoveDialog() }
        )
    }

    // Context Menu Bottom Sheet (M3 Standards)
    if (showContextMenu && activeContextTag != null) {
        ModalBottomSheet(
            onDismissRequest = { showContextMenu = false },
            shape = ExpressiveBottomSheetShape,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = activeContextTag?.name ?: "",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                ListItem(
                    headlineContent = { Text("Add Sub-tag") },
                    leadingContent = { Icon(Icons.Default.Add, null) },
                    modifier = Modifier.clickable { 
                        showContextMenu = false
                        viewModel.showCreateDialog(activeContextTag?.id) 
                    }
                )
                ListItem(
                    headlineContent = { Text("Rename") },
                    leadingContent = { Icon(Icons.Default.Edit, null) },
                    modifier = Modifier.clickable { 
                        showContextMenu = false
                        activeContextTag?.let { viewModel.showRenameDialog(it) }
                    }
                )
                ListItem(
                    headlineContent = { Text("Move to...") },
                    leadingContent = { Icon(Icons.Default.DriveFileMove, null) },
                    modifier = Modifier.clickable { 
                        showContextMenu = false
                        activeContextTag?.let { viewModel.showMoveDialog(it) }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ListItem(
                    headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { 
                        showContextMenu = false
                        activeContextTag?.let { viewModel.showDeleteDialog(it) }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Identities") },
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
                shape = PillShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Root Tag")
            }
        }
    ) { innerPadding ->
        // [自适应] 大屏模式下限制内容最大宽度并居中
        val tokens = rememberAdaptiveTokens()
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
        if (tagTree.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.Label, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text("No identities defined", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (tokens.contentMaxWidth != Dp.Unspecified)
                            Modifier.widthIn(max = tokens.contentMaxWidth)
                        else Modifier
                    )
                    .padding(horizontal = tokens.pageHorizontal),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                tagTree.forEach { node ->
                    renderTagNode(
                        node = node,
                        onToggleExpand = { viewModel.toggleExpand(it) },
                        onShowMenu = { 
                            activeContextTag = it
                            showContextMenu = true
                        }
                    )
                }
            }
        }
        } // [自适应] Box wrapper for centered content on large screens
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.renderTagNode(
    node: TagNode,
    onToggleExpand: (Long) -> Unit,
    onShowMenu: (Tag) -> Unit
) {
    item(key = node.tag.id) {
        TagRow(
            node = node,
            onToggleExpand = { onToggleExpand(node.tag.id) },
            onShowMenu = { onShowMenu(node.tag) }
        )
    }
    if (node.isExpanded) {
        node.children.forEach { child ->
            renderTagNode(
                node = child,
                onToggleExpand = onToggleExpand,
                onShowMenu = onShowMenu
            )
        }
    }
}

@Composable
private fun TagRow(
    node: TagNode,
    onToggleExpand: () -> Unit,
    onShowMenu: () -> Unit
) {
    val depth = node.depth
    val isRoot = depth == 0
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hierarchy lines (minimalist)
        if (!isRoot) {
            Box(modifier = Modifier.width(12.dp).fillMaxHeight()) {
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().align(Alignment.Center).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
            }
        }

        ElevatedCard(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp, horizontal = 4.dp),
            shape = BentoAsymmetricCardShape,
            colors = CardDefaults.elevatedCardColors(
                containerColor = when {
                    isRoot -> MaterialTheme.colorScheme.surfaceContainerHigh
                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                }
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isRoot) 1.dp else 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hierarchical Indicator
                if (node.children.isNotEmpty()) {
                    Icon(
                        imageVector = if (node.isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = node.tag.name,
                    style = if (isRoot) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Single Contextual Menu Access (Prevents visual overload)
                IconButton(onClick = onShowMenu, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", modifier = Modifier.size(20.dp))
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
                label = { Text("Identity Name") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
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
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Archive Identity") },
        text = {
            Column {
                Text("Archive \"$tagName\"? Linked memories will lose this association.")
                if (childCount > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "$childCount nested identities will be re-assigned.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep it")
            }
        }
    )
}

@Composable
private fun MoveTagDialog(
    availableParents: List<Tag>,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move Identity") },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                LazyColumn {
                    item {
                        ListItem(
                            headlineContent = { Text("Root Level") },
                            leadingContent = { Icon(Icons.Default.Upload, null) },
                            modifier = Modifier.clickable { onSelect(null) }
                        )
                    }
                    items(availableParents) { tag ->
                        ListItem(
                            headlineContent = { Text(tag.name) },
                            leadingContent = { Icon(Icons.Default.SubdirectoryArrowRight, null) },
                            modifier = Modifier.clickable { onSelect(tag.id) }
                        )
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
