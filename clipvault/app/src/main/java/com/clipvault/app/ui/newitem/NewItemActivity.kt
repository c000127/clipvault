package com.clipvault.app.ui.newitem

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.clipvault.app.ui.theme.ClipVaultTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewItemActivity : ComponentActivity() {

    private var pendingMediaUri: Uri? = null
    private var pendingMimeType: String? = null

    // Permission launcher for media access
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted && pendingMediaUri != null) {
            // Permission granted, proceed with copy
            proceedWithCopy(pendingMediaUri!!, pendingMimeType)
        } else {
            Toast.makeText(this, "需要媒体访问权限才能附加文件", Toast.LENGTH_LONG).show()
        }
        pendingMediaUri = null
        pendingMimeType = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Extract text from intent immediately
        val initialText = extractTextFromIntent(intent)

        // Copy URI if present (ACTION_SEND with media)
        var initialUri: Uri? = null
        var initialMimeType: String? = null

        if (intent?.action == Intent.ACTION_SEND) {
            // Use modern API to extract URI
            val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }

            streamUri?.let { uri ->
                // Try to take persistable permission
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // Not all URIs support persistable permission
                }

                // For media URIs, check/request permission before copying
                val mimeType = intent.type ?: contentResolver.getType(uri)
                if (mimeType != null && (mimeType.startsWith("image/") || mimeType.startsWith("video/") || mimeType.startsWith("audio/"))) {
                    if (hasMediaPermission(mimeType)) {
                        initialUri = uri
                        initialMimeType = mimeType
                    } else {
                        // Request permission first, then copy
                        pendingMediaUri = uri
                        pendingMimeType = mimeType
                        requestMediaPermission(mimeType)
                    }
                } else {
                    // Non-media (text), no permission needed
                    initialUri = uri
                    initialMimeType = mimeType
                }
            }
        }

        setContent {
            ClipVaultTheme {
                val viewModel = androidx.hilt.navigation.compose.hiltViewModel<NewItemViewModel>()

                // Copy URI content if present
                remember(initialUri) {
                    if (initialUri != null) {
                        viewModel.copyUriAndSetType(initialUri, initialMimeType)
                    }
                    true
                }

                NewItemScreen(
                    onBack = { finish() },
                    initialText = initialText
                )
            }
        }
    }

    private fun hasMediaPermission(mimeType: String): Boolean {
        val permission = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                when {
                    mimeType.startsWith("image/") -> Manifest.permission.READ_MEDIA_IMAGES
                    mimeType.startsWith("video/") -> Manifest.permission.READ_MEDIA_VIDEO
                    mimeType.startsWith("audio/") -> Manifest.permission.READ_MEDIA_AUDIO
                    else -> return true
                }
            }
            else -> Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestMediaPermission(mimeType: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = mutableListOf<String>()
            when {
                mimeType.startsWith("image/") -> permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                mimeType.startsWith("video/") -> permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                mimeType.startsWith("audio/") -> permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (permissions.isNotEmpty()) {
                requestPermissionLauncher.launch(permissions.toTypedArray())
            }
        } else {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    private fun proceedWithCopy(uri: Uri, mimeType: String?) {
        // Trigger copy in the ViewModel via content
        // We'll use a static reference approach or re-set content
        setContent {
            ClipVaultTheme {
                val viewModel = androidx.hilt.navigation.compose.hiltViewModel<NewItemViewModel>()
                remember(uri) {
                    viewModel.copyUriAndSetType(uri, mimeType)
                    true
                }
                NewItemScreen(
                    onBack = { finish() },
                    initialText = extractTextFromIntent(intent)
                )
            }
        }
    }

    private fun extractTextFromIntent(intent: Intent?): String? {
        return when (intent?.action) {
            Intent.ACTION_PROCESS_TEXT -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            }
            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }
            else -> null
        }
    }
}
