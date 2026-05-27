package com.clipvault.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.clipvault.app.ui.theme.BentoAsymmetricCardShape
import com.clipvault.app.ui.theme.PillShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.clipvault.app.data.local.entity.ClipItem
import com.clipvault.app.data.local.entity.Tag
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import com.clipvault.app.ui.theme.ClipVaultMotion
import com.clipvault.app.ui.theme.BentoAsymmetricCardShape
import androidx.compose.material3.ElevatedCard

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun HomeScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onItemClick: (Long) -> Unit,
    onNewItem: () -> Unit,
    onTagManager: () -> Unit = {},
    onSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTagIds by viewModel.selectedTagIds.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val clipboardSuggestion by viewModel.clipboardSuggestion.collectAsState()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()
    var showTagFilter by remember { mutableStateOf(false) }
    val pagingItems = viewModel.items.collectAsLazyPagingItems()
 
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showTagFilter) {
        TagFilterSheet(
            tags = allTags,
            selectedTagIds = selectedTagIds,
            onTagToggle = { tagId ->
                viewModel.toggleTagSelection(tagId)
            },
            onClearFilter = {
                viewModel.clearTagSelection()
            },
            onDismiss = { showTagFilter = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectedItemIds.isNotEmpty()) {
                        Text("${selectedItemIds.size} selected")
                    } else {
                        Text("ClipVault")
                    }
                },
                navigationIcon = {
                    if (selectedItemIds.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearItemSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                actions = {
                    if (selectedItemIds.isEmpty()) {
                        IconButton(onClick = { showTagFilter = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = if (selectedTagIds.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onTagManager) {
                            Icon(Icons.Default.Label, contentDescription = "Tags")
                        }
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedItemIds.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                androidx.compose.material3.BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.primary,
                    tonalElevation = 8.dp,
                    actions = {
                        IconButton(onClick = { viewModel.deleteSelectedItems() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Batch Actions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    android.util.Log.d("HomeScreen", "navigate to New")
                    onNewItem()
                },
                shape = CircleShape // pill shape for FAB
            ) {
                Icon(Icons.Default.Add, contentDescription = "New")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val dbInitFailed = remember { com.clipvault.app.ClipVaultApplication.dbInitFailed }
            val dbInitError = remember { com.clipvault.app.ClipVaultApplication.dbInitErrorMessage }

            if (dbInitFailed) {
                Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Close,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "数据库异常 (Database Failed)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "数据已在原始文件中受到保护，未丢失。错误原因：${dbInitError ?: "Unknown database error"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Search bar (pill container)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 0.dp
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search clips...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledBorderColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            }

            // Selected tags chips under search bar
            val selectedTags = remember(allTags, selectedTagIds) {
                allTags.filter { selectedTagIds.contains(it.id) }
            }
            if (selectedTags.isNotEmpty()) {
                val getTagPath: (Tag) -> String = { tag ->
                    val tagMap = allTags.associateBy { it.id }
                    val path = mutableListOf<String>()
                    var current: Tag? = tag
                    var safety = 50
                    while (current != null && safety-- > 0) {
                        path.add(current.name)
                        current = tagMap[current.parentId]
                    }
                    path.reversed().joinToString("/")
                }
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = com.clipvault.app.ui.theme.Dimensions.pageHorizontal, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(com.clipvault.app.ui.theme.Dimensions.itemSpacing),
                    verticalArrangement = Arrangement.spacedBy(com.clipvault.app.ui.theme.Dimensions.itemSpacing)
                ) {
                    selectedTags.forEach { tag ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.toggleTagSelection(tag.id) },
                            label = { Text(getTagPath(tag)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.toggleTagSelection(tag.id) }
                                )
                            }
                        )
                    }
                }
            }

            // Clipboard Suggestion
            AnimatedVisibility(
                visible = clipboardSuggestion != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                clipboardSuggestion?.let { text ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = BentoAsymmetricCardShape,
                        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("发现剪贴板新内容", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text.take(50).let { if (it.length == 50) "$it..." else it },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(onClick = { viewModel.dismissSuggestion() }) { Text("忽略") }
                            Button(onClick = { viewModel.saveSuggestion() }, shape = PillShape) { Text("保存") }
                        }
                    }
                }
            }

            // Staggered grid
            when (val refreshState = pagingItems.loadState.refresh) {
                is LoadState.Loading -> {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalItemSpacing = 16.dp
                    ) {
                        items(6) {
                            com.clipvault.app.ui.components.BentoSkeletonItem()
                        }
                    }
                }
                is LoadState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Error: ${refreshState.error.localizedMessage}")
                    }
                }
                is LoadState.NotLoading -> {
                    if (pagingItems.itemCount == 0) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No clips yet. Tap + to add one!")
                        }
                    } else {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(com.clipvault.app.ui.theme.Dimensions.sectionGap),
                            horizontalArrangement = Arrangement.spacedBy(com.clipvault.app.ui.theme.Dimensions.sectionGap),
                            verticalItemSpacing = com.clipvault.app.ui.theme.Dimensions.sectionGap
                        ) {
                            pagingItems(
                                items = pagingItems,
                                key = { it.id },
                                span = { item ->
                                    // 动态布局逻辑：带图条目或超长文本占据全宽 (Bento Span)
                                    if (item.thumbnailPath.isNotBlank() || item.content.length > 150) {
                                        StaggeredGridItemSpan.FullLine
                                    } else {
                                        StaggeredGridItemSpan.SingleLane
                                    }
                                }
                            ) { clipItem ->
                                clipItem?.let { item ->
                                    ClipCard(
                                        item = item,
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        onClick = {
                                            if (selectedItemIds.isNotEmpty()) {
                                                viewModel.toggleItemSelection(item.id)
                                            } else {
                                                onItemClick(item.id)
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.toggleItemSelection(item.id)
                                        },
                                        isSelected = selectedItemIds.contains(item.id)
                                    )
                                }
                            }

                            // Loading indicator at bottom
                            if (pagingItems.loadState.append is LoadState.Loading) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
 
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun LazyStaggeredGridItemScope.ClipCard(
    item: ClipItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    with(sharedTransitionScope) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateItem() // 添加列表项重排动效
                .sharedElement(
                    rememberSharedContentState(key = "item_${item.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { onLongClick() }
                    )
                },
            colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = BentoAsymmetricCardShape
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Content preview: display cover thumbnail if it exists
            if (item.thumbnailPath.isNotBlank()) {
                AsyncImage(
                    model = item.thumbnailPath,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (item.id % 2L == 0L) 1.2f else 0.8f) // non-symmetric bento grid aspect ratio
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(com.clipvault.app.ui.theme.Dimensions.itemSpacing))
            }

            // Display attachment type indicators
            val hasLink = item.attachments.any { it.type == "link" }
            val hasMedia = item.attachments.any { it.type == "media" }
            val hasFile = item.attachments.any { it.type == "file" }
            
            if (hasLink || hasMedia || hasFile) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasLink) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (hasMedia) {
                        Icon(
                            imageVector = Icons.Default.VideoFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Text content
            if (item.content.isNotBlank()) {
                Text(
                    text = item.content.take(100).let { if (it.length == 100) "$it..." else it },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (item.id % 2L == 0L) 6 else 4, // Bento-like staggered heights
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(com.clipvault.app.ui.theme.Dimensions.itemSpacing))

            // Timestamp
            Text(
                text = dateFormat.format(Date(item.createdAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
}
