package com.clipvault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clipvault.app.data.local.entity.BehaviorLog
import kotlinx.coroutines.flow.Flow

// [自适应] 行为事件 DAO — 记录和查询用户行为
@Dao
interface BehaviorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: BehaviorLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<BehaviorLog>): List<Long>

    @Query("SELECT * FROM behavior_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<BehaviorLog>

    @Query("SELECT * FROM behavior_logs WHERE eventType = :eventType ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getByType(eventType: String, limit: Int = 100): List<BehaviorLog>

    @Query("SELECT * FROM behavior_logs WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getSince(since: Long): List<BehaviorLog>

    @Query("SELECT * FROM behavior_logs WHERE eventType = :eventType AND timestamp >= :since")
    suspend fun getByTypeSince(eventType: String, since: Long): List<BehaviorLog>

    @Query("SELECT COUNT(*) FROM behavior_logs")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM behavior_logs WHERE eventType = :eventType")
    suspend fun getCountByType(eventType: String): Int

    // [自适应] 按 sessionId 查询会话行为序列
    @Query("SELECT * FROM behavior_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySession(sessionId: String): List<BehaviorLog>

    // [自适应] 按 eventType 分组统计（用于聚合引擎）
    @Query("SELECT eventType, COUNT(*) as cnt FROM behavior_logs GROUP BY eventType")
    suspend fun getCountByTypeGrouped(): List<EventTypeCount>

    // [自适应] 搜索关键词频率统计（7天内）
    @Query("""
        SELECT metadata, COUNT(*) as cnt FROM behavior_logs 
        WHERE eventType = '搜索' AND timestamp >= :since 
        GROUP BY metadata ORDER BY cnt DESC LIMIT :limit
    """)
    suspend fun getSearchKeywordFrequency(since: Long, limit: Int = 20): List<MetadataCount>

    // [自适应] Tag 操作频率统计
    @Query("""
        SELECT metadata, COUNT(*) as cnt FROM behavior_logs 
        WHERE eventType = 'Tag操作' AND timestamp >= :since 
        GROUP BY metadata ORDER BY cnt DESC LIMIT :limit
    """)
    suspend fun getTagOperationFrequency(since: Long, limit: Int = 50): List<MetadataCount>

    // [自适应] 时间分布统计（按小时）
    @Query("""
        SELECT CAST(strftime('%H', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) as hour, 
               COUNT(*) as cnt 
        FROM behavior_logs 
        WHERE eventType = '收藏' AND timestamp >= :since 
        GROUP BY hour ORDER BY cnt DESC
    """)
    suspend fun getCollectionTimeDistribution(since: Long): List<HourCount>

    // 清除所有行为数据（「忘记我的习惯」功能）
    @Query("DELETE FROM behavior_logs")
    suspend fun deleteAll()

    // [自适应] 归档：删除超过指定天数的记录
    @Query("DELETE FROM behavior_logs WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT MIN(timestamp) FROM behavior_logs")
    suspend fun getOldestTimestamp(): Long?
}

// [自适应] 聚合查询结果 DTO
data class EventTypeCount(
    val eventType: String,
    val cnt: Int
)

data class MetadataCount(
    val metadata: String,
    val cnt: Int
)

data class HourCount(
    val hour: Int,
    val cnt: Int
)
