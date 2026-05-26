package com.clipvault.app.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import com.clipvault.app.data.local.entity.Tag
import com.clipvault.app.ui.theme.BentoAsymmetricCardShape
import com.clipvault.app.ui.theme.PillShape
import com.clipvault.app.ui.theme.ExpressiveBottomSheetShape
import com.clipvault.app.ui.detail.AiState
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.clipvault.app.ui.theme.ClipVaultMotion

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
    val isEditing by viewModel.isEditing.collectAsState()
    val editContent by viewModel.editContent.collectAsState()
    val editAttachments by viewModel.editAttachments.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val attachmentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addAttachmentToEdit(it, context.contentResolver.getType(it)) }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addAttachmentToEdit(it, "image/jpeg") }
    }
 
    var showAiResultSheet by remember { mutableStateOf(false) }
    var showTagEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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
 
    LaunchedEffect(item) {
        item?.let {
            if (playingUri == null) {
                val firstMedia = it.attachments.firstOrNull { att -> att.type == "media" }
                if (firstMedia != null) viewModel.playMedia(firstMedia.filePath)
            }
        }
    }
 
    LaunchedEffect(aiState) {
        if (aiState is AiState.Success) showAiResultSheet = true
    }

    if (aiState is AiState.Error) {
        val errorMsg = (aiState as AiState.Error).message
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.clearAiState() },
            title = { Text("AI 分析失败") },
            text = { Text(errorMsg) },
            confirmButton = { TextButton(onClick = { viewModel.clearAiState() }) { Text("确定") } }
        )
    }

    if (showAiResultSheet && aiState is AiState.Success) {
        val successState = aiState as AiState.Success
        var summaryText by remember(successState.summary) { mutableStateOf(successState.summary) }
        val suggestedTags = successState.suggestedTags
        val selectedSuggestedTags = remember { mutableStateListOf<String>().apply { addAll(suggestedTags) } }

        ModalBottomSheet(
            onDismissRequest = { showAiResultSheet = false; viewModel.clearAiState() },
            shape = ExpressiveBottomSheetShape
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(text = "AI 分析结果", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "总结摘要 (可编辑)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = summaryText,
                    onValueChange = { summaryText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (suggestedTags.isNotEmpty()) {
                    Text(text = "推荐标签", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestedTags.forEach { tagPath ->
                            val isSelected = selectedSuggestedTags.contains(tagPath)
                            InputChip(
                                selected = isSelected,
                                onClick = { if (isSelected) selectedSuggestedTags.remove(tagPath) else selectedSuggestedTags.add(tagPath) },
                                label = { Text(tagPath) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = { showAiResultSheet = false; viewModel.clearAiState() }, modifier = Modifier.weight(1f), shape = PillShape) { Text("取消") }
                    Button(onClick = { viewModel.applyAiResult(summaryText, selectedSuggestedTags.toList()); showAiResultSheet = false; viewModel.clearAiState() }, modifier = Modifier.weight(1f), shape = PillShape) { Text("保存并应用") }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showTagEditSheet) {
        ModalBottomSheet(onDismissRequest = { showTagEditSheet = false }, shape = ExpressiveBottomSheetShape) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
                Text(text = "Edit Tags", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                if (allTags.isEmpty()) {
                    Text(text = "No tags created yet.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    com.clipvault.app.ui.components.TagTreeSelector(
                        allTags = allTags,
                        selectedTagIds = tags.map { it.id }.toSet(),
                        tagPaths = tagPaths,
                        onTagToggle = { tagId, selected -> if (selected) viewModel.addTag(tagId) else viewModel.removeTag(tagId) }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showTagEditSheet = false }, modifier = Modifier.fillMaxWidth(), shape = PillShape) { Text("Done") }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条收藏吗？") },
            confirmButton = { TextButton(onClick = { showDeleteDialog = false; viewModel.deleteItem(); onBack() }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }

    with(sharedTransitionScope) {
        Scaffold(
            modifier = Modifier.sharedElement(
                rememberSharedContentState(key = "item_${viewModel.itemId}"),
                animatedVisibilityScope = animatedVisibilityScope
            ),
            topBar = {
                TopAppBar(
                    title = { Text("Detail") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
                )
            }
        ) { innerPadding ->
            item?.let { clipItem ->
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = BentoAsymmetricCardShape,
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Content", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                if (!isEditing) IconButton(onClick = { viewModel.startEdit() }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                            }
                            if (isEditing) {
                                OutlinedTextField(value = editContent, onValueChange = { viewModel.updateEditContent(it) }, modifier = Modifier.fillMaxWidth().height(200.dp), shape = RoundedCornerShape(16.dp))
                            } else {
                                Text(text = clipItem.content, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }

                    if (playingUri != null) {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            shape = BentoAsymmetricCardShape,
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                MediaContent(exoPlayer = viewModel.exoPlayer)
                            }
                        }
                    }

                    if (isEditing) {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            shape = BentoAsymmetricCardShape,
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Edit Attachments", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                if (editAttachments.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        editAttachments.forEachIndexed { index, attachment ->
                                            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                if (attachment.type == "image") {
                                                    AsyncImage(model = attachment.filePath, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentDescription = null, contentScale = ContentScale.Crop)
                                                } else {
                                                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(48.dp))
                                                }
                                                Text(text = attachment.filePath.substringAfterLast('/'), modifier = Modifier.weight(1f).padding(horizontal = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                IconButton(onClick = { viewModel.removeAttachmentFromEdit(attachment.id) }) { Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error) }
                                            }
                                        }
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { imagePickerLauncher.launch("image/*") }, modifier = Modifier.weight(1f), shape = PillShape) { Text("Add Image") }
                                    OutlinedButton(onClick = { attachmentPickerLauncher.launch("*/*") }, modifier = Modifier.weight(1f), shape = PillShape) { Text("Add File") }
                                }
                            }
                        }
                    } else {
                        clipItem.attachments.filter { it.type == "image" }.forEach { attachment ->
                            var aspectRatio by remember { mutableStateOf<Float?>(null) }
                            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp)).padding(12.dp)) {
                                AsyncImage(
                                    model = attachment.filePath,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().then(if (aspectRatio != null) Modifier.aspectRatio(aspectRatio!!) else Modifier.heightIn(min = 180.dp)).clip(MaterialTheme.shapes.large),
                                    onSuccess = { state ->
                                        val size = state.painter.intrinsicSize
                                        if (size.width > 0 && size.height > 0) aspectRatio = size.width / size.height
                                    },
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }

                        val other = clipItem.attachments.filter { it.type != "image" }
                        if (other.isNotEmpty()) {
                            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp)).padding(12.dp)) {
                                Text(text = "Attachments & Links", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                other.forEach { attachment ->
                                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        when (attachment.type) {
                                            "media" -> Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Text(text = attachment.filePath.substringAfterLast('/'), modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                                                Button(onClick = { viewModel.playMedia(attachment.filePath) }, shape = PillShape) { Text("Play") }
                                            }
                                            "link" -> LinkContent(url = attachment.filePath, fetchedContent = clipItem.fetchedContent, fetchState = fetchState, onFetch = { viewModel.fetchLinkContent(attachment.filePath) }, onOpenBrowser = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(attachment.filePath))) })
                                            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Text(text = attachment.filePath.substringAfterLast('/'), modifier = Modifier.padding(horizontal = 8.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isEditing) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.cancelEdit() }, modifier = Modifier.weight(1f), shape = PillShape) { Text("Cancel") }
                            Button(onClick = { viewModel.saveEdit() }, modifier = Modifier.weight(1f), shape = PillShape) { Text("Save") }
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Card(modifier = Modifier.weight(1f), shape = PillShape, colors = CardDefaults.cardColors(containerColor = if (aiState is AiState.Loading) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primaryContainer)) {
                                Row(modifier = Modifier.clickable(enabled = aiState !is AiState.Loading) { if (clipItem.aiSummary.isNotBlank()) viewModel.regenerateAiSummary() else viewModel.analyzeContent() }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    if (aiState is AiState.Loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    else Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                    Text(text = if (aiState is AiState.Loading) "分析中..." else if (clipItem.aiSummary.isNotBlank()) "重新生成" else "AI 智能分析", modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                            OutlinedButton(onClick = { showDeleteDialog = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), shape = PillShape) { Text("Delete") }
                        }
                    }

                    if (clipItem.aiSummary.isNotBlank()) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            shape = BentoAsymmetricCardShape,
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(verticalAlignment = Alignment.CenterVertically) { 
                                        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                                        Text("AI Insight", modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.tertiary) 
                                    }
                                    Row {
                                        IconButton(onClick = { viewModel.regenerateAiSummary() }, enabled = aiState !is AiState.Loading, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp)) }
                                        IconButton(onClick = { viewModel.deleteAiSummary() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                                    }
                                }
                                Text(text = clipItem.aiSummary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp)).padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Tags", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { showTagEditSheet = true }) { Icon(Icons.Default.Edit, null) }
                        }
                        tags.forEach { tag ->
                            Text(text = "# ${tag.name}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
}

@Composable
private fun LinkContent(url: String, fetchedContent: String, fetchState: FetchState, onFetch: () -> Unit, onOpenBrowser: () -> Unit) {
    Column {
        Text(text = url, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenBrowser, shape = PillShape) { Text("Open") }
            Button(onClick = onFetch, shape = PillShape) { Text("Fetch") }
        }
        if (fetchedContent.isNotBlank()) Text(text = fetchedContent.take(200), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun MediaContent(exoPlayer: ExoPlayer) {
    AndroidView(factory = { ctx -> androidx.media3.ui.PlayerView(ctx).apply { player = exoPlayer } }, modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(16.dp)))
}
