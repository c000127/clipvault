package com.clipvault.app.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.clipvault.app.data.local.CryptoManager
import com.clipvault.app.data.local.dao.AiProviderDao
import com.clipvault.app.data.local.entity.AiProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.apiKeyDataStore: DataStore<Preferences> by preferencesDataStore(name = "api_keys")

@Singleton
class AiProviderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiProviderDao: AiProviderDao,
    private val cryptoManager: CryptoManager
) {
    // CRUD 操作
    suspend fun insert(provider: AiProvider): Long = aiProviderDao.insert(provider)

    suspend fun update(provider: AiProvider) = aiProviderDao.update(provider)

    suspend fun delete(provider: AiProvider) = aiProviderDao.delete(provider)

    suspend fun deleteById(id: Long) = aiProviderDao.deleteById(id)

    // 查询操作
    fun getById(id: Long): Flow<AiProvider?> = aiProviderDao.getById(id)

    suspend fun getByIdOnce(id: Long): AiProvider? = aiProviderDao.getByIdOnce(id)

    fun getAllProviders(): Flow<List<AiProvider>> = aiProviderDao.getAllProviders()

    suspend fun getAllProvidersOnce(): List<AiProvider> = aiProviderDao.getAllProvidersOnce()

    fun getActiveProvider(): Flow<AiProvider?> = aiProviderDao.getActiveProvider()

    suspend fun getActiveProviderOnce(): AiProvider? = aiProviderDao.getActiveProviderOnce()

    suspend fun getCount(): Int = aiProviderDao.getCount()

    // 激活切换
    suspend fun setActiveProvider(providerId: Long) {
        aiProviderDao.setActiveProvider(providerId)
    }

    /**
     * Save API Key encrypted in DataStore.
     * Key format: "api_key_{providerId}"
     * Value: AES-GCM encrypted, Base64 encoded
     */
    suspend fun saveApiKey(providerId: Long, apiKey: String) {
        try {
            val key = stringPreferencesKey("api_key_$providerId")
            val encrypted = cryptoManager.encrypt(apiKey)
            context.apiKeyDataStore.edit { prefs ->
                prefs[key] = encrypted
            }
        } catch (e: Exception) {
            android.util.Log.e("AiProviderRepository", "Failed to save API key", e)
        }
    }

    /**
     * Get API Key from DataStore, decrypted.
     */
    suspend fun getApiKey(providerId: Long): String {
        Log.d("AiProviderRepo", "getApiKey: start")
        return try {
            val key = stringPreferencesKey("api_key_$providerId")
            val prefs = withTimeoutOrNull(3000) {
                context.apiKeyDataStore.data
                    .catch { exception ->
                        if (exception is IOException) {
                            emit(emptyPreferences())
                        } else {
                            throw exception
                        }
                    }
                    .first()
            } ?: return ""
            val encrypted = prefs[key] ?: return ""
            val result = cryptoManager.decrypt(encrypted)
            Log.d("AiProviderRepo", "getApiKey: success key=${result.take(4)}***")
            result
        } catch (e: Exception) {
            Log.e("AiProviderRepo", "getApiKey: error", e)
            ""
        }
    }

    /**
     * Delete API Key from DataStore.
     */
    suspend fun deleteApiKey(providerId: Long) {
        try {
            val key = stringPreferencesKey("api_key_$providerId")
            context.apiKeyDataStore.edit { prefs ->
                prefs.remove(key)
            }
        } catch (e: Exception) {
            android.util.Log.e("AiProviderRepository", "Failed to delete API key", e)
        }
    }

    /**
     * Get full AiProvider with decrypted API key.
     */
    suspend fun getProviderWithApiKey(providerId: Long): AiProvider? {
        val provider = aiProviderDao.getByIdOnce(providerId) ?: return null
        val apiKey = getApiKey(providerId)
        return provider.copy(apiKey = apiKey)
    }

    /**
     * Get active provider with decrypted API key.
     */
    suspend fun getActiveProviderWithApiKey(): AiProvider? {
        val provider = aiProviderDao.getActiveProviderOnce() ?: return null
        val apiKey = getApiKey(provider.id)
        return provider.copy(apiKey = apiKey)
    }
}
