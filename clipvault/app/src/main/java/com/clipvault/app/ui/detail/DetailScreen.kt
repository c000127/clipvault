package com.clipvault.app.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import com.clipvault.app.ui.theme.BentoAsymmetricCardShape
import com.clipvault.app.ui.theme.PillShape
import com.clipvault.app.ui.theme.ExpressiveBottomSheetShape
import com.clipvault.app.ui.detail.AiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val item by viewModel.item.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val tagPaths by viewModel.tagPaths.collectAsState()
    val fetchState by viewModel.fetchState.collectAsState()
    val aiState by viewModel.aiState.collectAsState()
    val playingUri by viewModel.playingUri.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
 
    var showAiResultSheet by remember { mutableStateOf(false) }
    var showTagEditSheet by remember { mutableStateOf(false) }
 
    // Lifecycle management for media player
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.pause()
                Lifecycle.Event.ON_RESUME -> viewModel.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.pause()
        }
    }
 
    // Setup player when item loads
    LaunchedEffect(item) {
        item?.let {
            if (playingUri == null) {
                val firstMedia = it.attachments.firstOrNull { att -> att.type == "media" }
                if (firstMedia != null) {
                    viewModel.playMedia(firstMedia.filePath)
                }
            }
        }
    }
 
    // Auto open AI bottom sheet when analysis finishes successfully
    LaunchedEffect(aiState) {
        if (aiState is AiState.Success) {
            showAiResultSheet = true
        }
    }

    // AI Analysis status dialog overlays
    if (aiState is AiState.Loading) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI 分析中...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (aiState is AiState.Error) {
        val errorMsg = (aiState as AiState.Error).message
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.clearAiState() },
            title = { Text("AI 分析失败") },
            text = { Text(errorMsg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAiState() }) {
                    Text("确定")
                }
            }
        )
    }

    // AI Analysis Result Bottom Sheet
    if (showAiResultSheet && aiState is AiState.Success) {
        val successState = aiState as AiState.Success
        var summaryText by remember(successState.summary) { mutableStateOf(successState.summary) }
        val suggestedTags = successState.suggestedTags
        val selectedSuggestedTags = remember { mutableStateListOf<String>().apply { addAll(suggestedTags) } }

        ModalBottomSheet(
            onDismissRequest = {
                showAiResultSheet = false
                viewModel.clearAiState()
            },
            shape = ExpressiveBottomSheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "AI 分析结果",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "总结摘要 (可编辑)",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = summaryText,
                    onValueChange = { summaryText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (suggestedTags.isNotEmpty()) {
                    Text(
                        text = "推荐标签 (点击选择/取消)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestedTags.forEach { tagPath ->
                            val isSelected = selectedSuggestedTags.contains(tagPath)
                            InputChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedSuggestedTags.remove(tagPath)
                                    else selectedSuggestedTags.add(tagPath)
                                },
                                label = { Text(tagPath) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showAiResultSheet = false
                            viewModel.clearAiState()
                        },
                        modifier = Modifier.weight(1f),
                        shape = PillShape
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            viewModel.applyAiResult(summaryText, selectedSuggestedTags.toList())
                            showAiResultSheet = false
                            viewModel.clearAiState()
                        },
                        modifier = Modifier.weight(1f),
                        shape = PillShape
                    ) {
                        Text("保存并应用")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Tag association edit bottom sheet
    if (showTagEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTagEditSheet = false },
            shape = ExpressiveBottomSheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Edit Tags",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (allTags.isEmpty()) {
                    Text(
                        text = "No tags created yet. Add tags in home or settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    allTags.forEach { tag ->
                        val isAssociated = tags.any { it.id == tag.id }
                        val path = tagPaths[tag.id] ?: tag.name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isAssociated) {
                                        viewModel.removeTag(tag.id)
                                    } else {
                                        viewModel.addTag(tag.id)
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isAssociated,
                                onCheckedChange = { checked ->
                                    if (checked != null) {
                                        if (checked) {
                                            viewModel.addTag(tag.id)
                                        } else {
                                            viewModel.removeTag(tag.id)
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = path,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showTagEditSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape
                ) {
                    Text("Done")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.deleteItem()
                        onBack()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { innerPadding ->
        item?.let { clipItem ->
            with(sharedTransitionScope) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "clip_${clipItem.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            }
                        )
                ) {
                    // Content text if not empty
                    if (clipItem.content.isNotBlank()) {
                        Text(
                            text = clipItem.content,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Display media player if a media attachment is playing
                    if (playingUri != null) {
                        MediaContent(exoPlayer = viewModel.exoPlayer)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Display all attachments
                    if (clipItem.attachments.isNotEmpty()) {
                        Text(
                            text = "Attachments",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            clipItem.attachments.forEach { attachment ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        when (attachment.type) {
                                            "image" -> {
                                                AsyncImage(
                                                    model = attachment.filePath,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(200.dp)
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                            "media" -> {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = attachment.filePath.substringAfterLast('/'),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Button(
                                                        onClick = { viewModel.playMedia(attachment.filePath) },
                                                        shape = PillShape
                                                    ) {
                                                        Text("Play")
                                                    }
                                                }
                                            }
                                            "link" -> {
                                                LinkContent(
                                                    url = attachment.filePath,
                                                    fetchedContent = clipItem.fetchedContent,
                                                    fetchState = fetchState,
                                                    onFetch = { viewModel.fetchLinkContent(attachment.filePath) },
                                                    onOpenBrowser = {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(attachment.filePath))
                                                        context.startActivity(intent)
                                                    }
                                                )
                                            }
                                            else -> {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = attachment.filePath.substringAfterLast('/'),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
 
                    // AI Analysis Action Button
                    Button(
                        onClick = { viewModel.analyzeContent() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = PillShape
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI 智能分析")
                    }
 
                    Spacer(modifier = Modifier.height(16.dp))
 
                    // Tags section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tags",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { showTagEditSheet = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Tags")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
 
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tags.forEach { tag ->
                            val path = tagPaths[tag.id] ?: tag.name
                            androidx.compose.material3.AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = path,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.widthIn(max = 200.dp)
                            )
                        }
                    }
 
                    Spacer(modifier = Modifier.height(16.dp))
 
                    // Metadata
                    Text(
                        text = "Type: ${clipItem.type}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (clipItem.sourceApp.isNotBlank()) {
                        Text(
                            text = "Source: ${clipItem.sourceApp}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun LinkContent(
    url: String,
    fetchedContent: String,
    fetchState: FetchState,
    onFetch: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    Column {
        Icon(
            imageVector = Icons.Default.Link,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = url,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onOpenBrowser,
                shape = PillShape
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open")
            }
            Button(
                onClick = onFetch,
                shape = PillShape
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Fetch Content")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (fetchState) {
            is FetchState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
            is FetchState.Success -> {
                Text(
                    text = fetchState.content.take(500),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is FetchState.Error -> {
                Text(
                    text = fetchState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            is FetchState.Idle -> {
                if (fetchedContent.isNotBlank()) {
                    Text(
                        text = fetchedContent.take(500),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun MediaContent(exoPlayer: ExoPlayer) {
    AndroidView(
        factory = { ctx ->
            androidx.media3.ui.PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(BentoAsymmetricCardShape)
    )
}
