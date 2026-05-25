package com.clipvault.app.ui.newitem
 
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import kotlinx.coroutines.launch
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
 
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewItemScreen(
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
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Set initial text from intent
    LaunchedEffect(initialText) {
        initialText?.let { viewModel.setContent(it) }
    }

    // Navigate back on save
    LaunchedEffect(saved) {
        if (saved) onBack()
    }

    // Show error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        try {
            uri?.let {
                android.util.Log.d("NewItemScreen", "image picked: $it")
                viewModel.copyUriAndSetType(it, "image/jpeg")
            }
        } catch (e: Exception) {
            android.util.Log.e("NewItemScreen", "image picker callback failed", e)
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Failed to import image: ${e.message}")
            }
        }
    }

    // File picker for media
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        try {
            uri?.let {
                android.util.Log.d("NewItemScreen", "file picked: $it")
                val mimeType = context.contentResolver.getType(it)
                viewModel.copyUriAndSetType(it, mimeType)
            }
        } catch (e: Exception) {
            android.util.Log.e("NewItemScreen", "file picker callback failed", e)
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Failed to import file: ${e.message}")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("New Clip") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Content input (16dp rounded corners)
            OutlinedTextField(
                value = content,
                onValueChange = { viewModel.setContent(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                label = { Text("Content") },
                placeholder = { Text("Enter text, URL, or paste content...") },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )

            if (attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Attachments",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    attachments.forEachIndexed { index, attachment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            if (attachment.type == "image") {
                                AsyncImage(
                                    model = attachment.filePath,
                                    contentDescription = "Selected image preview",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Row(
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (attachment.type == "media") Icons.Default.CameraAlt else Icons.Default.Save,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = attachment.filePath.substringAfterLast('/'),
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.removeAttachment(index) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Import buttons (Pill shape)
            Text(
                text = "Import from",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    shape = com.clipvault.app.ui.theme.PillShape
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Text("Gallery")
                }
                OutlinedButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    shape = com.clipvault.app.ui.theme.PillShape
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Text("File")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tags section
            Text(
                text = "Tags",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            var newTagName by remember { mutableStateOf("") }

            com.clipvault.app.ui.components.TagTreeSelector(
                allTags = allTags,
                selectedTagIds = selectedTags.toSet(),
                tagPaths = remember(allTags) {
                    val tagMap = allTags.associateBy { it.id }
                    allTags.associate { tag ->
                        val path = mutableListOf<String>()
                        var current: com.clipvault.app.data.local.entity.Tag? = tag
                        var safety = 50
                        while (current != null && safety-- > 0) {
                            path.add(current.name)
                            current = tagMap[current.parentId]
                        }
                        tag.id to path.reversed().joinToString("/")
                    }
                },
                onTagToggle = { tagId, _ -> viewModel.toggleTag(tagId) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // New tag input (16dp rounded corners)
            OutlinedTextField(
                value = newTagName,
                onValueChange = { newTagName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("New tag name") },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                trailingIcon = {
                    if (newTagName.isNotBlank()) {
                        IconButton(onClick = {
                            viewModel.createTag(newTagName, null)
                            newTagName = ""
                        }) {
                            Text("Add", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save button (Pill shape)
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && (content.isNotBlank() || attachments.isNotEmpty()),
                shape = com.clipvault.app.ui.theme.PillShape
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Icon(Icons.Default.Save, contentDescription = null)
                Text("Save")
            }
        }
    }
}
