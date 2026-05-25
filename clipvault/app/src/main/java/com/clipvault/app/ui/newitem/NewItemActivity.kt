package com.clipvault.app.ui.newitem

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.clipvault.app.ui.theme.ClipVaultTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewItemActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Extract text from intent immediately
        val initialText = extractTextFromIntent(intent)

        // Copy URI if present (ACTION_SEND with media)
        var initialUri: Uri? = null
        var initialMimeType: String? = null

        if (intent?.action == Intent.ACTION_SEND) {
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                // Try to take persistable permission
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // Not all URIs support persistable permission
                }
                initialUri = uri
                initialMimeType = intent.type
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
