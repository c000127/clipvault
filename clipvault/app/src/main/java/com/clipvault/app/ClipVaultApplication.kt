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
        // Build the database instance instantly (non-blocking)
        val db = AppDatabase.getInstance(this)

        // Pre-initialize database asynchronously on a background thread to ensure smooth startup and non-blocking navigation
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("ClipVault", "Starting DB async pre-warmup...")
                // Querying the clip item count forces the database to open and execute migrations
                val count = db.clipItemDao().getCount()
                Log.d("ClipVault", "DB async pre-warmup OK. Initial clip count: $count")
            } catch (e: Exception) {
                Log.e("ClipVault", "DB async pre-warmup FAILED, but data preserved", e)
                AppDatabase.migrationFailed = true
            }
        }
    }
}
