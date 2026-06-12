package com.clipvault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.clipvault.app.data.local.entity.UserInsight
import kotlinx.coroutines.flow.Flow

// [自适应] 用户洞察 DAO — 读写聚合后的行为模式数据
@Dao
interface InsightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(insight: UserInsight): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(insights: List<UserInsight>): List<Long>

    @Update
    suspend fun update(insight: UserInsight)

    @Query("SELECT * FROM user_insights WHERE insightType = :type AND key = :key LIMIT 1")
    suspend fun get(type: String, key: String): UserInsight?

    @Query("SELECT * FROM user_insights WHERE insightType = :type ORDER BY value DESC")
    suspend fun getByType(type: String): List<UserInsight>

    @Query("SELECT * FROM user_insights WHERE insightType = :type AND sampleCount >= :minSamples ORDER BY value DESC")
    suspend fun getByTypeWithMinSamples(type: String, minSamples: Int = 50): List<UserInsight>

    @Query("SELECT * FROM user_insights WHERE insightType = :type ORDER BY value DESC")
    fun getByTypeFlow(type: String): Flow<List<UserInsight>>

    @Query("SELECT * FROM user_insights ORDER BY insightType, value DESC")
    fun getAllInsights(): Flow<List<UserInsight>>

    @Query("SELECT * FROM user_insights ORDER BY insightType, value DESC")
    suspend fun getAllInsightsOnce(): List<UserInsight>

    // [自适应] Upsert: 先查询存在则更新，否则插入
    @Transaction
    suspend fun upsert(insight: UserInsight) {
        val existing = get(insight.insightType, insight.key)
        if (existing != null) {
            update(insight.copy(id = existing.id))
        } else {
            insert(insight)
        }
    }

    @Query("SELECT COUNT(*) FROM user_insights")
    suspend fun getCount(): Int

    // 清除所有洞察数据
    @Query("DELETE FROM user_insights")
    suspend fun deleteAll()

    // [自适应] 删除指定类型的洞察
    @Query("DELETE FROM user_insights WHERE insightType = :type")
    suspend fun deleteByType(type: String)
}
