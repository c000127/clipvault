package com.clipvault.app.data.behavior

import com.clipvault.app.data.local.dao.BehaviorDao
import com.clipvault.app.data.local.entity.BehaviorLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// [自适应] 层级2: 环境因素 — 行为追踪器
// 在关键操作点记录事件，用于推断用户习惯
// 所有数据仅本地存储，永不上传
@Singleton
class BehaviorTracker @Inject constructor(
    private val behaviorDao: BehaviorDao
) {
    // 每个应用启动分配一个 sessionId
    val sessionId: String = UUID.randomUUID().toString()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // [自适应] 记录行为事件（异步，不阻塞调用方）
    fun track(eventType: String, metadata: String = "") {
        scope.launch {
            try {
                behaviorDao.insert(
                    BehaviorLog(
                        eventType = eventType,
                        metadata = metadata,
                        sessionId = sessionId
                    )
                )
            } catch (e: Exception) {
                android.util.Log.w("BehaviorTracker", "Failed to track event: $eventType", e)
            }
        }
    }

    // [自适应] 便捷方法
    fun trackCollection(contentType: String = "text") {
        track(EVENT_COLLECT, """{"contentType":"$contentType"}""")
    }

    fun trackSearch(query: String) {
        track(EVENT_SEARCH, """{"query":"${query.take(100)}"}""")
    }

    fun trackAiSummary(itemId: Long, adopted: Boolean) {
        track(EVENT_AI_SUMMARY, """{"itemId":$itemId,"adopted":$adopted}""")
    }

    fun trackTagOperation(operation: String, tagId: Long) {
        track(EVENT_TAG_OPERATION, """{"operation":"$operation","tagId":$tagId}""")
    }

    fun trackPageVisit(page: String) {
        track(EVENT_PAGE_VISIT, """{"page":"$page"}""")
    }

    fun trackReading(itemId: Long, durationMs: Long) {
        track(EVENT_READING, """{"itemId":$itemId,"durationMs":$durationMs}""")
    }

    companion object {
        const val EVENT_COLLECT = "收藏"
        const val EVENT_SEARCH = "搜索"
        const val EVENT_AI_SUMMARY = "AI总结"
        const val EVENT_TAG_OPERATION = "Tag操作"
        const val EVENT_PAGE_VISIT = "页面访问"
        const val EVENT_READING = "阅读"
    }
}
