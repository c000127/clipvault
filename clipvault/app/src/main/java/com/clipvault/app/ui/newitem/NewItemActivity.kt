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
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.clipvault.app.ui.theme.ClipVaultTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewItemActivity : ComponentActivity() {

    private val viewModel: NewItemViewModel by viewModels()

    // Permission launcher for in-app media access (if needed)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "需要媒体访问权限才能完整附加文件", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val initialText = extractTextFromIntent(intent)

        // Process shared media URIs IMMEDIATELY on Activity start
        // This avoids URI permission expiration when activity lifecycle state changes.
        if (intent?.action == Intent.ACTION_SEND) {
            val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }

            streamUri?.let { uri ->
                // Try to persist the permission
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // Not all URIs support persistable permissions
                }

                val mimeType = intent.type ?: contentResolver.getType(uri)
                
                // For Android 13+ granular media, proactively request permission for future accesses if needed,
                // but do not block the immediate copy since we already have temporary read grant for this URI.
                requestMediaPermissionsIfNeeded(mimeType)
                
                // Copy the URI contents to app private storage stream-wise immediately
                viewModel.copyUriAndSetType(uri, mimeType)
            }
        }

        setContent {
            ClipVaultTheme {
                // [动效] NewItemActivity 独立于 NavHost，需要自己的 SharedTransitionLayout
                androidx.compose.animation.SharedTransitionLayout {
                    androidx.compose.animation.AnimatedVisibility(visible = true) {
                        NewItemScreen(
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            onBack = { finish() },
                            initialText = initialText,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    private fun requestMediaPermissionsIfNeeded(mimeType: String?) {
        if (mimeType == null) return
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                mimeType.startsWith("image/") && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED -> {
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
                }
                mimeType.startsWith("video/") && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED -> {
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
                }
                mimeType.startsWith("audio/") && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED -> {
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
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
