package com.clipvault.app.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.ElevatedCard
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
import com.clipvault.app.data.local.entity.Tag
import androidx.compose.material.icons.filled.Image
import com.clipvault.app.ui.theme.BentoAsymmetricCardShape
import com.clipvault.app.ui.theme.PillShape
import com.clipvault.app.ui.theme.ExpressiveBottomSheetShape
import com.clipvault.app.ui.detail.AiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
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

    // 文件选择器（编辑模式添加附件用）
    val attachmentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addAttachmentToEdit(it, context.contentResolver.getType(it))
        }
    }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addAttachmentToEdit(it, "image/jpeg") }
    }
 
    var showAiResultSheet by remember { mutableStateOf(false) }
    var showTagEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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
                    com.clipvault.app.ui.components.TagTreeSelector(
                        allTags = allTags,
                        selectedTagIds = tags.map { it.id }.toSet(),
                        tagPaths = tagPaths,
                        onTagToggle = { tagId, selected ->
                            if (selected) {
                                viewModel.addTag(tagId)
                            } else {
                                viewModel.removeTag(tagId)
                            }
                        }
                    )
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

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条收藏吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteItem()
                        onBack()
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail") },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        item?.let { clipItem ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                    // Card 1: Content text if not empty (padded, elevated card with Bento shape)
                    if (isEditing || clipItem.content.isNotBlank() || clipItem.attachments.isNotEmpty()) {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = BentoAsymmetricCardShape,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Content",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (!isEditing) {
                                        IconButton(onClick = { viewModel.startEdit() }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Content")
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                if (isEditing) {
                                    OutlinedTextField(
                                        value = editContent,
                                        onValueChange = { viewModel.updateEditContent(it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        placeholder = { Text("Write content here...") },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = clipItem.content,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Card 2: Display media player if a media attachment is playing (wrapped in elevated card)
                    if (playingUri != null) {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = BentoAsymmetricCardShape,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                MediaContent(exoPlayer = viewModel.exoPlayer)
                            }
                        }
                    }

                    if (isEditing) {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = BentoAsymmetricCardShape,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Edit Attachments",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                if (editAttachments.isEmpty()) {
                                    Text(
                                        text = "No attachments",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        editAttachments.forEachIndexed { index, attachment ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (attachment.type == "image") {
                                                    AsyncImage(
                                                        model = attachment.filePath,
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .clip(RoundedCornerShape(8.dp)),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.AttachFile,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(48.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = attachment.filePath.substringAfterLast('/'),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                // Reorder buttons
                                                if (index > 0) {
                                                    IconButton(
                                                        onClick = { viewModel.reorderAttachment(index, index - 1) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                if (index < editAttachments.size - 1) {
                                                    IconButton(
                                                        onClick = { viewModel.reorderAttachment(index, index + 1) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                // Delete button
                                                IconButton(
                                                    onClick = { viewModel.removeAttachmentFromEdit(attachment.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { imagePickerLauncher.launch("image/*") },
                                        modifier = Modifier.weight(1f),
                                        shape = PillShape
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Image")
                                    }
                                    OutlinedButton(
                                        onClick = { attachmentPickerLauncher.launch("*/*") },
                                        modifier = Modifier.weight(1f),
                                        shape = PillShape
                                    ) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add File")
                                    }
                                }
                            }
                        }
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    } else {
                        // Card 3a: Image Attachments (each gets its own massive, full-bleed elevated card to maximize screen width and scale adaptively)
                        val (imageAttachments, otherAttachments) = remember(clipItem.attachments) {
                            clipItem.attachments.partition { it.type == "image" }
                        }

                        imageAttachments.forEach { attachment ->
                            var aspectRatio by remember(attachment.filePath) { mutableStateOf<Float?>(null) }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                val imageModifier = if (aspectRatio != null) {
                                    Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(aspectRatio!!)
                                        .clip(MaterialTheme.shapes.large)
                                } else {
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 180.dp)
                                        .clip(MaterialTheme.shapes.large)
                                }

                                AsyncImage(
                                    model = attachment.filePath,
                                    contentDescription = "Image attachment",
                                    modifier = imageModifier,
                                    onSuccess = { state ->
                                        val size = state.painter.intrinsicSize
                                        if (size.width > 0 && size.height > 0) {
                                            aspectRatio = size.width / size.height
                                        }
                                    },
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        // Card 3b: Other Attachments (media, link, file)
                        if (otherAttachments.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(4.dp)) {
                                    Text(
                                        text = "Attachments & Links",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        otherAttachments.forEach { attachment ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        color = androidx.compose.ui.graphics.Color.Transparent,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                when (attachment.type) {
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
                            }
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }

                    if (isEditing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.cancelEdit() },
                                modifier = Modifier.weight(1f),
                                shape = PillShape
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Cancel")
                            }
                            Button(
                                onClick = { viewModel.saveEdit() },
                                modifier = Modifier.weight(1f),
                                shape = PillShape
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Save")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = PillShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (aiState is AiState.Loading)
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    else
                                        MaterialTheme.colorScheme.primaryContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = aiState !is AiState.Loading) {
                                            if (clipItem.aiSummary.isNotBlank()) {
                                                viewModel.regenerateAiSummary()
                                            } else {
                                                viewModel.analyzeContent()
                                            }
                                        }
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (aiState is AiState.Loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when {
                                            aiState is AiState.Loading -> "分析中..."
                                            clipItem.aiSummary.isNotBlank() -> "重新生成"
                                            else -> "AI 智能分析"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (aiState is AiState.Loading)
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        else
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                shape = PillShape
                             ) {
                                 Icon(Icons.Default.Delete, contentDescription = null)
                                 Spacer(modifier = Modifier.width(4.dp))
                                 Text("Delete")
                             }
                        }
                    }

                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // AI Summary Section — shown only when aiSummary is non-blank
                    if (clipItem.aiSummary.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "AI 总结",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Row {
                                    // 重新生成按钮
                                    IconButton(
                                        onClick = { viewModel.regenerateAiSummary() },
                                        enabled = aiState !is AiState.Loading,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "重新生成",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    // 删除总结按钮
                                    IconButton(
                                        onClick = { viewModel.deleteAiSummary() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "删除总结",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = clipItem.aiSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Card 5: Tags & Metadata Section (grouped, flat container Column)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tags",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { showTagEditSheet = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Tags")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // 构建选中 tag 的树形结构（只显示已选 tags 的层级关系）
                            val selectedTagTree = remember(tags, allTags) {
                                val tagMap = allTags.associateBy { it.id }
                                // 找出所有已选 tag 及其所有祖先
                                val ancestors = mutableSetOf<Tag>()
                                tags.forEach { tag ->
                                    var current: Tag? = tag
                                    var safety = 50
                                    while (current != null && safety-- > 0) {
                                        ancestors.add(current)
                                        current = tagMap[current.parentId]
                                    }
                                }
                                // 按层级排序：parent → children
                                ancestors.sortedBy { tag ->
                                    var count = 0
                                    var c: Tag? = tag
                                    while (c != null) { count++; c = tagMap[c.parentId] }
                                    count
                                }
                            }

                            // 用 Column 树形展示
                            Column(modifier = Modifier.padding(4.dp)) {
                                selectedTagTree.forEach { tag ->
                                    val isSelectedTag = tags.any { it.id == tag.id }  // 是否直接选中（vs 祖先）
                                    val depth = tagPaths[tag.id]?.count { it == '/' } ?: 0
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = (depth * 24).dp, top = 2.dp, bottom = 2.dp)
                                            .background(
                                                color = if (isSelectedTag)
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                                else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 层级指示线/点
                                        if (depth > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(20.dp)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        // 图标
                                        Icon(
                                            imageVector = if (isSelectedTag) Icons.Default.Label else Icons.Default.Folder,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isSelectedTag)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // 名称
                                        Text(
                                            text = tag.name,
                                            style = if (isSelectedTag)
                                                MaterialTheme.typography.bodyLarge
                                            else
                                                MaterialTheme.typography.bodyMedium,
                                            color = if (isSelectedTag)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        // 如果子 tag 被选中，已选标记
                                        if (isSelectedTag) {
                                            Spacer(modifier = Modifier.weight(1f))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            if (clipItem.sourceApp.isNotBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Source: ${clipItem.sourceApp}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
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
