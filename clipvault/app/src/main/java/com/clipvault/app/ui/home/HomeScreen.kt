package com.clipvault.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import com.clipvault.app.ui.theme.BentoAsymmetricCardShape
import com.clipvault.app.ui.theme.PillShape
// [自适应] 导入自适应布局工具
import com.clipvault.app.ui.adaptive.rememberAdaptiveTokens
import com.clipvault.app.ui.adaptive.rememberDeviceFormFactor
import com.clipvault.app.ui.adaptive.DeviceFormFactor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
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
import androidx.compose.ui.graphics.Color
// [动效] 引用全局动效 Token
import com.clipvault.app.ui.theme.ClipVaultMotion

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
    // [自适应] 获取当前设备形态和 Token
    val tokens = rememberAdaptiveTokens()
    val formFactor = rememberDeviceFormFactor()
 
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
                        Text("ClipVault", style = MaterialTheme.typography.headlineMedium)
                    }
                },
                navigationIcon = {
                    if (selectedItemIds.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearItemSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                actions = {
                    if (selectedItemIds.isEmpty()) {
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
            Column {
                // Batch Actions Bar
                AnimatedVisibility(
                    visible = selectedItemIds.isNotEmpty(),
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = androidx.compose.animation.core.tween(ClipVaultMotion.Standard, easing = ClipVaultMotion.DefaultEasing)
                    ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(ClipVaultMotion.Standard)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = androidx.compose.animation.core.tween(ClipVaultMotion.Quick, easing = ClipVaultMotion.DefaultEasing)
                    ) + fadeOut(animationSpec = androidx.compose.animation.core.tween(ClipVaultMotion.Quick, easing = ClipVaultMotion.DefaultEasing))
                ) {
                    BottomAppBar(
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
                
                // Thumb-zone Search & Filter Bar
                // [自适应] 大屏模式下限制搜索栏最大宽度并居中
                if (selectedItemIds.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .widthIn(max = if (formFactor.isLargeScreen) 600.dp else Dp.Unspecified)
                                .fillMaxWidth()
                                .padding(horizontal = tokens.pageHorizontal, vertical = 12.dp)
                                .navigationBarsPadding(),
                            shape = PillShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shadowElevation = 4.dp
                        ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            IconButton(onClick = { showTagFilter = true }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = if (selectedTagIds.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Search your vault...", style = MaterialTheme.typography.bodyLarge) },
                                leadingIcon = null,
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                            Icon(Icons.Default.Close, null)
                                        }
                                    }
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                )
                            )
                            
                            FloatingActionButton(
                                onClick = onNewItem,
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.primary,
                                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Add, "New")
                            }
                        }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        // [自适应] 大屏模式下限制内容最大宽度并居中
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (tokens.contentMaxWidth != androidx.compose.ui.unit.Dp.Unspecified)
                        Modifier.widthIn(max = tokens.contentMaxWidth)
                    else Modifier
                )
        ) {
            val dbInitFailed = remember { com.clipvault.app.ClipVaultApplication.dbInitFailed }
            
            if (dbInitFailed) {
                ErrorBanner()
            }

            // Selected tags chips (Filter info)
            SelectedTagsRow(allTags, selectedTagIds, viewModel)

            // Clipboard Suggestion
            ClipboardSuggestionCard(clipboardSuggestion, viewModel)

            // Content Grid
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                ContentGrid(
                    pagingItems = pagingItems,
                    selectedItemIds = selectedItemIds,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onItemClick = { id ->
                        if (selectedItemIds.isNotEmpty()) viewModel.toggleItemSelection(id)
                        else onItemClick(id)
                    },
                    onLongClick = { id -> viewModel.toggleItemSelection(id) }
                )
            }
        }
        } // [自适应] Box wrapper for centered content on large screens
    }
}

@Composable
private fun ErrorBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = BentoAsymmetricCardShape
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(16.dp))
            Text("Database synchronization issues detected.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SelectedTagsRow(allTags: List<Tag>, selectedTagIds: Set<Long>, viewModel: HomeViewModel) {
    val selectedTags = remember(allTags, selectedTagIds) {
        allTags.filter { selectedTagIds.contains(it.id) }
    }
    if (selectedTags.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedTags.forEach { tag ->
                InputChip(
                    selected = true,
                    onClick = { viewModel.toggleTagSelection(tag.id) },
                    label = { Text(tag.name) },
                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp).clickable { viewModel.toggleTagSelection(tag.id) }) },
                    shape = PillShape
                )
            }
        }
    }
}

@Composable
private fun ClipboardSuggestionCard(text: String?, viewModel: HomeViewModel) {
    AnimatedVisibility(
        visible = text != null,
        enter = expandVertically(
            animationSpec = androidx.compose.animation.core.tween(ClipVaultMotion.Standard, easing = ClipVaultMotion.DefaultEasing)
        ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(ClipVaultMotion.Standard)),
        exit = shrinkVertically(
            animationSpec = androidx.compose.animation.core.tween(ClipVaultMotion.Quick, easing = ClipVaultMotion.DefaultEasing)
        ) + fadeOut(animationSpec = androidx.compose.animation.core.tween(ClipVaultMotion.Quick, easing = ClipVaultMotion.DefaultEasing)),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        text?.let { content ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = BentoAsymmetricCardShape,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentPaste, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("New item in clipboard", style = MaterialTheme.typography.labelMedium)
                        Text(content, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { viewModel.dismissSuggestion() }) { Text("Dismiss") }
                    Button(onClick = { viewModel.saveSuggestion() }, shape = PillShape) { Text("Save") }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContentGrid(
    pagingItems: androidx.paging.compose.LazyPagingItems<ClipItem>,
    selectedItemIds: Set<Long>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onItemClick: (Long) -> Unit,
    onLongClick: (Long) -> Unit
) {
    // [自适应] 根据设备形态决定列数和间距
    val tokens = rememberAdaptiveTokens()
    val formFactor = rememberDeviceFormFactor()
    val gridColumns = tokens.gridColumns
    val spacing = tokens.itemSpacing

    when (val refreshState = pagingItems.loadState.refresh) {
        is LoadState.Loading -> {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(gridColumns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(tokens.pageHorizontal),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalItemSpacing = spacing
            ) {
                items(6) { com.clipvault.app.ui.components.BentoSkeletonItem() }
            }
        }
        is LoadState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Failed to load clips. Swipe down to retry.")
            }
        }
        is LoadState.NotLoading -> {
            if (pagingItems.itemCount == 0) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Your vault is empty. Add your first memory!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(gridColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(tokens.pageHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalItemSpacing = spacing
                ) {
                    pagingItems(
                        items = pagingItems,
                        key = { it.id },
                        span = { item ->
                            // [自适应] 大屏模式下减少 FullLine span，让卡片更紧凑
                            val threshold = if (formFactor.isLargeScreen) 200 else 120
                            if (item.thumbnailPath.isNotBlank() || item.content.length > threshold) StaggeredGridItemSpan.FullLine
                            else StaggeredGridItemSpan.SingleLane
                        }
                    ) { clipItem ->
                        clipItem?.let { item ->
                            ClipCard(
                                item = item,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onClick = { onItemClick(item.id) },
                                onLongClick = { onLongClick(item.id) },
                                isSelected = selectedItemIds.contains(item.id)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)
@Composable
private fun LazyStaggeredGridItemScope.ClipCard(
    item: ClipItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    with(sharedTransitionScope) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .animateItem()
                .sharedElement(
                    rememberSharedContentState(key = "item_${item.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
            shape = BentoAsymmetricCardShape
        ) {
            Column {
                if (item.thumbnailPath.isNotBlank()) {
                    AsyncImage(
                        model = item.thumbnailPath,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(if (item.id % 3L == 0L) 1.5f else 1f)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(modifier = Modifier.padding(12.dp)) {
                    if (item.content.isNotBlank()) {
                        Text(
                            text = item.content,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Tag Hierarchy Path (Optimized Visualization)
                    if (item.tags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item.tags.take(3).forEach { tag ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = PillShape
                                ) {
                                    Text(
                                        text = tag.name,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateFormat.format(Date(item.createdAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (item.attachments.any { it.type == "link" }) {
                            Icon(Icons.Default.Link, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}
