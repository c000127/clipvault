package com.clipvault.app

import android.app.Application
import android.util.Log
import com.clipvault.app.data.local.AppDatabase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class ClipVaultApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Pre-initialize database asynchronously on a background thread to ensure smooth startup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.getInstance(this@ClipVaultApplication)
                Log.d("ClipVault", "DB async init OK")
            } catch (e: Exception) {
                Log.e("ClipVault", "DB async init FAILED, but data preserved", e)
                AppDatabase.migrationFailed = true
            }
        }
    }
}
