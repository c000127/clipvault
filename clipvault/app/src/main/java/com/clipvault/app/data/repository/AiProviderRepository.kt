package com.clipvault.app.data.repository

import com.clipvault.app.data.local.dao.AiProviderDao
import com.clipvault.app.data.local.entity.AiProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI Provider 配置管理仓库
 *
 * 注意：API Key 加密存储将在 Task 10 实现。
 * 当前实现中，apiKey 字段在 Room 中存储空字符串，
 * 实际 API Key 将存储在 Preferences DataStore 中，
 * key 格式为 "api_key_{providerId}"。
 *
 * 加密方案：
 * - 使用 Android Keystore AES-256-GCM 加密
 * - 密钥别名："clipvault_aes_key"
 * - 格式：Base64(IV[12] + CipherText + AuthTag[16])
 */
@Singleton
class AiProviderRepository @Inject constructor(
    private val aiProviderDao: AiProviderDao
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

    // API Key 存储（占位：Task 10 实现加密逻辑）
    suspend fun saveApiKey(providerId: Long, apiKey: String) {
        // TODO: Task 10 - 使用 DataStore + Android Keystore AES-GCM 加密存储
        // 当前占位：不存储实际密钥
    }

    suspend fun getApiKey(providerId: Long): String {
        // TODO: Task 10 - 从 DataStore 解密读取
        // 当前占位：返回空字符串
        return ""
    }

    suspend fun deleteApiKey(providerId: Long) {
        // TODO: Task 10 - 从 DataStore 删除
    }
}
