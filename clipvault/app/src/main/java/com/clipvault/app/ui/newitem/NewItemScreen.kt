package com.clipvault.app.ui.newitem

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.InputChip
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.clipvault.app.data.local.entity.Tag

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewItemScreen(
    onBack: () -> Unit,
    initialText: String? = null,
    viewModel: NewItemViewModel = hiltViewModel()
) {
    val content by viewModel.content.collectAsState()
    val type by viewModel.type.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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
        uri?.let {
            viewModel.copyUriAndSetType(it, "image/jpeg")
        }
    }

    // File picker for media
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it)
            viewModel.copyUriAndSetType(it, mimeType)
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
            // Content input
            OutlinedTextField(
                value = content,
                onValueChange = { viewModel.setContent(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                label = { Text("Content") },
                placeholder = { Text("Enter text, URL, or paste content...") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Import buttons
            Text(
                text = "Import from",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Text("Gallery")
                }
                OutlinedButton(onClick = { filePickerLauncher.launch("*/*") }) {
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

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allTags.forEach { tag ->
                    InputChip(
                        selected = selectedTags.contains(tag.id),
                        onClick = { viewModel.toggleTag(tag.id) },
                        label = { Text(tag.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // New tag input
            OutlinedTextField(
                value = newTagName,
                onValueChange = { newTagName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("New tag name") },
                singleLine = true,
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

            // Save button
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && content.isNotBlank()
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
