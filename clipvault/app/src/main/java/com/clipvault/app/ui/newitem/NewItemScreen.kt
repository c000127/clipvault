package com.clipvault.app.ui.newitem
 
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import kotlinx.coroutines.launch
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.clipvault.app.ui.theme.BentoAsymmetricCardShape
import com.clipvault.app.ui.theme.PillShape
// [自适应] 导入自适应布局工具
import com.clipvault.app.ui.adaptive.rememberAdaptiveTokens
// [动效] SharedTransition 支持
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
 
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun NewItemScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
    initialText: String? = null,
    viewModel: NewItemViewModel = hiltViewModel()
) {
    val content by viewModel.content.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(initialText) {
        initialText?.let { viewModel.setContent(it) }
    }

    LaunchedEffect(saved) {
        if (saved) onBack()
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        try {
            uri?.let { viewModel.copyUriAndSetType(it, "image/jpeg") }
        } catch (e: Exception) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Failed to import image: ${e.message}") }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        try {
            uri?.let {
                val mimeType = context.contentResolver.getType(it)
                viewModel.copyUriAndSetType(it, mimeType)
            }
        } catch (e: Exception) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Failed to import file: ${e.message}") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        // [自适应] 大屏模式下限制内容最大宽度并居中
        val tokens = rememberAdaptiveTokens()
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = tokens.pageHorizontal)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "New Memory",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Capture a piece of your digital world",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { viewModel.setContent(it) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                label = { Text("Content") },
                placeholder = { Text("Enter text, URL, or paste content...") },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )

            if (attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Attachments", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    attachments.forEachIndexed { index, attachment ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = BentoAsymmetricCardShape,
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                if (attachment.type == "image") {
                                    AsyncImage(model = attachment.filePath, contentDescription = null, modifier = Modifier.size(80.dp).clip(BentoAsymmetricCardShape), contentScale = ContentScale.Crop)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                        Icon(imageVector = if (attachment.type == "media") Icons.Default.PlayCircle else Icons.Default.Description, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = attachment.filePath.substringAfterLast('/'), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                IconButton(onClick = { viewModel.removeAttachment(index) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Import from", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { imagePickerLauncher.launch("image/*") }, shape = PillShape, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) {
                    Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Gallery")
                }
                Button(onClick = { filePickerLauncher.launch("*/*") }, shape = PillShape, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) {
                    Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("File")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Identity", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            var newTagName by remember { mutableStateOf("") }
            com.clipvault.app.ui.components.TagTreeSelector(
                allTags = allTags,
                selectedTagIds = selectedTags.toSet(),
                tagPaths = emptyMap(),
                onTagToggle = { tagId, _ -> viewModel.toggleTag(tagId) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = newTagName,
                onValueChange = { newTagName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Quick Tag") },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                trailingIcon = {
                    if (newTagName.isNotBlank()) {
                        IconButton(onClick = { viewModel.createTag(newTagName, null); newTagName = "" }) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isSaving && (content.isNotBlank() || attachments.isNotEmpty()),
                shape = PillShape
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(Modifier.width(12.dp))
                }
                Text("Save to Vault", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
        } // [自适应] Box wrapper for centered content on large screens
    }
}
