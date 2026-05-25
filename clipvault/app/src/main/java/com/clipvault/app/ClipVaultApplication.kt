package com.clipvault.app

import android.app.Application
import android.util.Log
import com.clipvault.app.data.local.AppDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ClipVaultApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Pre-initialize database, capturing migration or initialization errors
            AppDatabase.getInstance(this)
            Log.d("ClipVault", "DB init OK")
        } catch (e: Exception) {
            Log.e("ClipVault", "DB init FAILED, but data preserved", e)
            AppDatabase.migrationFailed = true
        }
    }
}
