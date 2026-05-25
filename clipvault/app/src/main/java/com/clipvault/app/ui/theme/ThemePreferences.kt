package com.clipvault.app.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

enum class ThemeMode {
    LIGHT,
    DARK,
    FOLLOW_SYSTEM
}

@Singleton
class ThemePreferences @Inject constructor(
    private val context: Context
) {
    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        when (prefs[THEME_MODE_KEY]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.FOLLOW_SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }
}
