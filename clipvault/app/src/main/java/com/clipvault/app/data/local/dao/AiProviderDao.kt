package com.clipvault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.clipvault.app.data.local.entity.AiProvider
import kotlinx.coroutines.flow.Flow

@Dao
interface AiProviderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(provider: AiProvider): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(providers: List<AiProvider>): List<Long>

    @Update
    suspend fun update(provider: AiProvider)

    @Delete
    suspend fun delete(provider: AiProvider)

    @Query("DELETE FROM ai_providers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM ai_providers WHERE id = :id")
    fun getById(id: Long): Flow<AiProvider?>

    @Query("SELECT * FROM ai_providers WHERE id = :id")
    suspend fun getByIdOnce(id: Long): AiProvider?

    @Query("SELECT * FROM ai_providers ORDER BY name ASC")
    fun getAllProviders(): Flow<List<AiProvider>>

    @Query("SELECT * FROM ai_providers ORDER BY name ASC")
    suspend fun getAllProvidersOnce(): List<AiProvider>

    // 获取当前激活的配置
    @Query("SELECT * FROM ai_providers WHERE isActive = 1 LIMIT 1")
    fun getActiveProvider(): Flow<AiProvider?>

    @Query("SELECT * FROM ai_providers WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProviderOnce(): AiProvider?

    // 激活切换：先取消所有激活，再激活指定配置
    @Transaction
    suspend fun setActiveProvider(providerId: Long) {
        deactivateAll()
        activate(providerId)
    }

    @Query("UPDATE ai_providers SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE ai_providers SET isActive = 1 WHERE id = :providerId")
    suspend fun activate(providerId: Long)

    @Query("SELECT COUNT(*) FROM ai_providers")
    suspend fun getCount(): Int
}
