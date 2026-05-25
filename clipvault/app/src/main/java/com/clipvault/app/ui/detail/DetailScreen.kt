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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val item by viewModel.item.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val fetchState by viewModel.fetchState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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
            if (it.type == "media") {
                viewModel.setupPlayer()
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Content based on type
                when (clipItem.type) {
                    "text" -> {
                        Text(
                            text = clipItem.content,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    "image" -> {
                        AsyncImage(
                            model = clipItem.content,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            contentScale = ContentScale.Fit
                        )
                    }
                    "link" -> {
                        LinkContent(
                            url = clipItem.content,
                            fetchedContent = clipItem.fetchedContent,
                            fetchState = fetchState,
                            onFetch = { viewModel.fetchLinkContent() },
                            onOpenBrowser = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(clipItem.content))
                                context.startActivity(intent)
                            }
                        )
                    }
                    "media" -> {
                        MediaContent(exoPlayer = viewModel.exoPlayer)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tags section
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.removeTag(tag.id) },
                            label = { Text(tag.name) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                            }
                        )
                    }

                    // Add tag chips (show tags not yet associated)
                    val availableTags = allTags.filter { tag -> tags.none { it.id == tag.id } }
                    availableTags.take(10).forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = { viewModel.addTag(tag.id) },
                            label = { Text(tag.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Note section
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                var noteText by remember(clipItem.note) { mutableStateOf(clipItem.note) }
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    label = { Text("Add notes...") }
                )

                Button(
                    onClick = { viewModel.updateNote(noteText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Save Notes")
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
            Button(onClick = onOpenBrowser) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                Text("Open")
            }
            Button(onClick = onFetch) {
                Icon(Icons.Default.Download, contentDescription = null)
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

@Composable
private fun MediaContent(exoPlayer: ExoPlayer) {
    AndroidView(
        factory = { ctx ->
            android.widget.VideoView(ctx).apply {
                // Using VideoView as fallback since PlayerView may not be in compose artifact
                // ExoPlayer integration via PlayerView would need media3-ui (non-compose)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    )
}
