package com.clipvault.app.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import com.clipvault.app.data.local.entity.Tag
import com.clipvault.app.ui.theme.BentoAsymmetricCardShape
import com.clipvault.app.ui.theme.PillShape
import com.clipvault.app.ui.theme.ExpressiveBottomSheetShape
import com.clipvault.app.ui.detail.AiState
import com.clipvault.app.ui.theme.ClipVaultMotion
import com.clipvault.app.ui.theme.ClipSharedElementKey
import com.clipvault.app.ui.theme.ClipSharedElementType
// [自适应] 导入自适应布局工具
import com.clipvault.app.ui.adaptive.rememberAdaptiveTokens

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
    
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val isInteractionAllowed = lifecycleState == androidx.lifecycle.Lifecycle.State.RESUMED

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
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = ClipSharedElementKey(viewModel.itemId, ClipSharedElementType.Bounds)),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> ClipVaultMotion.SpatialExpressiveSpring },
                        clipInOverlayDuringTransition = OverlayClip(BentoAsymmetricCardShape),
                        enter = fadeIn(ClipVaultMotion.NonSpatialExpressiveSpring),
                        exit = fadeOut(ClipVaultMotion.NonSpatialExpressiveSpring)
                    )
                    .sharedElement(
                        rememberSharedContentState(key = ClipSharedElementKey(viewModel.itemId, ClipSharedElementType.Content)),
                        animatedVisibilityScope = animatedVisibilityScope
                    ),
                containerColor = MaterialTheme.colorScheme.surface,
                topBar = {
                    TopAppBar(
                        title = {
                            val titleText = item?.content?.lineSequence()?.firstOrNull()?.take(20) ?: "Clip Detail"
                            Text(
                                text = titleText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 3.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AnimatedContent(
                            targetState = isEditing,
                            transitionSpec = {
                                // [动效] 编辑模式切换：纯 spring scale 变换
                                (scaleIn(initialScale = 0.95f, animationSpec = ClipVaultMotion.ScaleIn))
                                    .togetherWith(
                                        scaleOut(targetScale = 0.95f, animationSpec = ClipVaultMotion.ScaleIn)
                                    )
                            },
                            label = "ActionHub"
                        ) { editing ->
                            if (editing) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 24.dp, vertical = 16.dp)
                                        .navigationBarsPadding(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.cancelEdit() },
                                        modifier = Modifier.weight(1f),
                                        shape = PillShape,
                                        enabled = isInteractionAllowed // 动画拦截
                                    ) {
                                        Text("Cancel")
                                    }
                                    Button(
                                        onClick = { viewModel.saveEdit() },
                                        modifier = Modifier.weight(1f),
                                        shape = PillShape,
                                        enabled = isInteractionAllowed, // 动画拦截
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Save")
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 24.dp, vertical = 12.dp)
                                        .navigationBarsPadding(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Emotional/Dynamic AI Button
                                    Button(
                                        onClick = {
                                            if (item?.aiSummary?.isNotBlank() == true) viewModel.regenerateAiSummary()
                                            else viewModel.analyzeContent()
                                        },
                                        modifier = Modifier.weight(2f),
                                        shape = PillShape,
                                        enabled = isInteractionAllowed && aiState !is AiState.Loading, // 双重拦截
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        if (aiState is AiState.Loading) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (item?.aiSummary?.isNotBlank() == true) "Refine AI" else "AI Insight")
                                    }

                                    // Secondary Actions
                                    IconButton(
                                        onClick = { viewModel.startEdit() },
                                        enabled = isInteractionAllowed,
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, PillShape)
                                    ) {
                                        Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    IconButton(
                                        onClick = { showDeleteDialog = true },
                                        enabled = isInteractionAllowed,
                                        modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), PillShape)
                                    ) {
                                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                // [自适应] 大屏模式下限制内容最大宽度并居中
                val tokens = rememberAdaptiveTokens()
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                item?.let { clipItem ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (tokens.contentMaxWidth != androidx.compose.ui.unit.Dp.Unspecified)
                                    Modifier.widthIn(max = tokens.contentMaxWidth)
                                else Modifier
                            )
                            .verticalScroll(rememberScrollState())
                    ) {
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            visible = true
                        }

                        // HERO SECTION: Images & Media
                        // [动效] Hero 区域入场：纯 spring 滑动，不加 fade
                        AnimatedVisibility(
                            visible = visible,
                            enter = slideInVertically(initialOffsetY = { 40 },
                                animationSpec = ClipVaultMotion.PageSlide),
                            label = "HeroAnim"
                        ) {
                            Column {
                                val imageAttachments = clipItem.attachments.filter { it.type == "image" }
                                if (imageAttachments.isNotEmpty()) {
                                    imageAttachments.forEach { attachment ->
                                        var aspectRatio by remember { mutableStateOf<Float?>(null) }
                                        AsyncImage(
                                            model = attachment.filePath,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(if (aspectRatio != null) Modifier.aspectRatio(aspectRatio!!) else Modifier.heightIn(min = 200.dp))
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                                .clip(MaterialTheme.shapes.extraLarge),
                                            onSuccess = { state ->
                                                val size = state.painter.intrinsicSize
                                                if (size.width > 0 && size.height > 0) aspectRatio = size.width / size.height
                                            },
                                            contentScale = ContentScale.FillWidth
                                        )
                                    }
                                }

                                if (playingUri != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .clip(BentoAsymmetricCardShape)
                                            .background(Color.Black)
                                    ) {
                                        MediaContent(exoPlayer = viewModel.exoPlayer)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // CONTENT SECTION
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = BentoAsymmetricCardShape,
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Notes, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Content", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                if (isEditing) {
                                    OutlinedTextField(
                                        value = editContent,
                                        onValueChange = { viewModel.updateEditContent(it) },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            unfocusedBorderColor = Color.Transparent,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                    )
                                } else {
                                    Text(
                                        text = clipItem.content,
                                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // AI INSIGHT SECTION
                        // [动效] AI 结果：纯 expand/shrink 容器动画，不加 fade
                        AnimatedVisibility(
                            visible = clipItem.aiSummary.isNotBlank(),
                            enter = expandVertically(
                                animationSpec = ClipVaultMotion.ExpandSpring),
                            exit = shrinkVertically(
                                animationSpec = ClipVaultMotion.ExpandSpring)
                        ) {
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = BentoAsymmetricCardShape,
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically) { 
                                            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                                            Text("AI Synthesis", modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary) 
                                        }
                                        if (!isEditing) {
                                            IconButton(onClick = { viewModel.deleteAiSummary() }, modifier = Modifier.size(24.dp)) { 
                                                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(14.dp)) 
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
                            }
                        }

                        // ATTACHMENTS SECTION
                        val other = clipItem.attachments.filter { it.type != "image" }
                        if (other.isNotEmpty() || isEditing) {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = BentoAsymmetricCardShape,
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(text = "Assets", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                                    Spacer(Modifier.height(12.dp))
                                    
                                    if (isEditing) {
                                        editAttachments.forEach { attachment ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp)).padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(if (attachment.type == "image") Icons.Default.Image else Icons.Default.AttachFile, null, modifier = Modifier.size(24.dp))
                                                Text(text = attachment.filePath.substringAfterLast('/'), modifier = Modifier.weight(1f).padding(horizontal = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                IconButton(onClick = { viewModel.removeAttachmentFromEdit(attachment.id) }, modifier = Modifier.size(24.dp)) { 
                                                    Icon(Icons.Default.RemoveCircleOutline, null, tint = MaterialTheme.colorScheme.error) 
                                                }
                                            }
                                        }
                                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            AssistChip(
                                                onClick = { imagePickerLauncher.launch("image/*") },
                                                label = { Text("Add Photo") },
                                                leadingIcon = { Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(18.dp)) }
                                            )
                                            AssistChip(
                                                onClick = { attachmentPickerLauncher.launch("*/*") },
                                                label = { Text("Add File") },
                                                leadingIcon = { Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(18.dp)) }
                                            )
                                        }
                                    } else {
                                        other.forEach { attachment ->
                                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                                when (attachment.type) {
                                                    "media" -> Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.PlayCircle, null, tint = MaterialTheme.colorScheme.primary)
                                                        Text(text = attachment.filePath.substringAfterLast('/'), modifier = Modifier.weight(1f).padding(horizontal = 8.dp), style = MaterialTheme.typography.bodyMedium)
                                                        Button(onClick = { viewModel.playMedia(attachment.filePath) }, shape = PillShape, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), modifier = Modifier.height(28.dp)) { Text("Play", style = MaterialTheme.typography.labelSmall) }
                                                    }
                                                    "link" -> LinkContent(url = attachment.filePath, fetchedContent = clipItem.fetchedContent, fetchState = fetchState, onFetch = { viewModel.fetchLinkContent(attachment.filePath) }, onOpenBrowser = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(attachment.filePath))) })
                                                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.secondary)
                                                        Text(text = attachment.filePath.substringAfterLast('/'), modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.bodyMedium)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // METADATA (TAGS) SECTION
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = BentoAsymmetricCardShape,
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "Identity", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    IconButton(onClick = { showTagEditSheet = true }, modifier = Modifier.size(24.dp)) { 
                                        Icon(Icons.Default.Label, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp)) 
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                if (tags.isEmpty()) {
                                    Text("No tags assigned.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        tags.forEach { tag ->
                                            SuggestionChip(
                                                onClick = { },
                                                label = { Text(tag.name) },
                                                shape = PillShape,
                                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                                            )
                                        }
                                    }
                                }
                                if (clipItem.sourceApp.isNotBlank()) {
                                    Spacer(Modifier.height(12.dp))
                                    Text("Captured from ${clipItem.sourceApp}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(100.dp)) // Padding for bottom bar
                    }
                }
                } // [自适应] Box wrapper for centered content on large screens
            }

            if (!isInteractionAllowed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial)
                                        .changes.forEach { it.consume() }
                                }
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun LinkContent(url: String, fetchedContent: String, fetchState: FetchState, onFetch: () -> Unit, onOpenBrowser: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = url, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        }
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenBrowser, shape = PillShape, contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.height(32.dp)) { Text("Open", style = MaterialTheme.typography.labelMedium) }
            TextButton(onClick = onFetch, modifier = Modifier.height(32.dp)) { Text("Fetch Content", style = MaterialTheme.typography.labelMedium) }
        }
        if (fetchedContent.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(text = fetchedContent.take(150) + "...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun MediaContent(exoPlayer: ExoPlayer) {
    AndroidView(
        factory = { ctx -> androidx.media3.ui.PlayerView(ctx).apply { player = exoPlayer; useController = true } }, 
        modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(20.dp))
    )
}
