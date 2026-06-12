package com.clipvault.app.data.behavior

import com.clipvault.app.data.local.dao.BehaviorDao
import com.clipvault.app.data.local.dao.InsightDao
import com.clipvault.app.data.local.entity.UserInsight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// [自适应] 层级2: 环境因素 — 洞察引擎
// 定期聚合 BehaviorLog → UserInsight，用于自适应调整
// 当 sampleCount >= 50 时才启用自适应调整（避免小样本偏差）
@Singleton
class InsightEngine @Inject constructor(
    private val behaviorDao: BehaviorDao,
    private val insightDao: InsightDao
) {
    // [自适应] 聚合所有洞察类型
    suspend fun aggregateAll() = withContext(Dispatchers.IO) {
        try {
            aggregateTagFrequency()
            aggregateTimePatterns()
            aggregateContentDistribution()
            aggregateSearchHotWords()
            aggregateAiAdoption()
            cleanupOldLogs()
        } catch (e: Exception) {
            android.util.Log.w("InsightEngine", "Aggregation failed", e)
        }
    }

    // [自适应] Tag 使用频率聚合
    private suspend fun aggregateTagFrequency() {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val tagOps = behaviorDao.getTagOperationFrequency(thirtyDaysAgo, limit = 50)
        tagOps.forEach { (metadata, count) ->
            val tagName = extractJsonString(metadata, "operation") ?: return@forEach
            insightDao.upsert(
                UserInsight(
                    insightType = INSIGHT_TAG_FREQUENCY,
                    key = tagName,
                    value = count.toFloat(),
                    sampleCount = count,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    // [自适应] 时间分布聚合（几点最活跃）
    private suspend fun aggregateTimePatterns() {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val timeDistribution = behaviorDao.getCollectionTimeDistribution(thirtyDaysAgo)
        val total = timeDistribution.sumOf { it.cnt }
        timeDistribution.forEach { (hour, count) ->
            insightDao.upsert(
                UserInsight(
                    insightType = INSIGHT_TIME_PATTERN,
                    key = hour.toString(),
                    value = count.toFloat() / total.coerceAtLeast(1),
                    sampleCount = total,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    // [自适应] 内容类型分布聚合
    private suspend fun aggregateContentDistribution() {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val collections = behaviorDao.getByTypeSince(BehaviorTracker.EVENT_COLLECT, thirtyDaysAgo)
        val typeCounts = mutableMapOf<String, Int>()
        collections.forEach { log ->
            val contentType = extractJsonString(log.metadata, "contentType") ?: "text"
            typeCounts[contentType] = (typeCounts[contentType] ?: 0) + 1
        }
        val total = typeCounts.values.sum()
        typeCounts.forEach { (type, count) ->
            insightDao.upsert(
                UserInsight(
                    insightType = INSIGHT_CONTENT_DISTRIBUTION,
                    key = type,
                    value = count.toFloat() / total.coerceAtLeast(1),
                    sampleCount = total,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    // [自适应] 搜索热词聚合
    private suspend fun aggregateSearchHotWords() {
        val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val ninetyDaysAgo = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000

        // [自适应] 搜索热词衰减: 7天内 ×3, 30天内 ×1, 90天后 ×0.3
        val recentSearches = behaviorDao.getSearchKeywordFrequency(sevenDaysAgo, limit = 20)
        val monthSearches = behaviorDao.getSearchKeywordFrequency(thirtyDaysAgo, limit = 20)
        val oldSearches = behaviorDao.getSearchKeywordFrequency(ninetyDaysAgo, limit = 20)

        val weightedScores = mutableMapOf<String, Float>()
        recentSearches.forEach { (meta, cnt) ->
            val query = extractJsonString(meta, "query") ?: return@forEach
            weightedScores[query] = (weightedScores[query] ?: 0f) + cnt * 3f
        }
        monthSearches.forEach { (meta, cnt) ->
            val query = extractJsonString(meta, "query") ?: return@forEach
            weightedScores[query] = (weightedScores[query] ?: 0f) + cnt * 1f
        }
        oldSearches.forEach { (meta, cnt) ->
            val query = extractJsonString(meta, "query") ?: return@forEach
            weightedScores[query] = (weightedScores[query] ?: 0f) + cnt * 0.3f
        }

        val totalSearches = behaviorDao.getCountByType(BehaviorTracker.EVENT_SEARCH)
        weightedScores.entries.sortedByDescending { it.value }.take(20).forEach { (query, score) ->
            insightDao.upsert(
                UserInsight(
                    insightType = INSIGHT_SEARCH_HOT,
                    key = query,
                    value = score,
                    sampleCount = totalSearches,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    // [自适应] AI 总结采纳率聚合
    private suspend fun aggregateAiAdoption() {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val aiEvents = behaviorDao.getByTypeSince(BehaviorTracker.EVENT_AI_SUMMARY, thirtyDaysAgo)
        val total = aiEvents.size
        val adopted = aiEvents.count { log ->
            extractJsonBool(log.metadata, "adopted")
        }
        if (total > 0) {
            insightDao.upsert(
                UserInsight(
                    insightType = INSIGHT_AI_ADOPTION,
                    key = "adoption_rate",
                    value = adopted.toFloat() / total,
                    sampleCount = total,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    // [自适应] 清理超过180天的行为日志（保留聚合后的 UserInsights）
    private suspend fun cleanupOldLogs() {
        val cutoff = System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000
        behaviorDao.deleteOlderThan(cutoff)
    }

    // [自适应] 「忘记我的习惯」功能
    suspend fun forgetAll() = withContext(Dispatchers.IO) {
        behaviorDao.deleteAll()
        insightDao.deleteAll()
    }

    // [自适应] 获取所有洞察数据（用于设置页展示）
    suspend fun getAllInsights(): List<UserInsight> = withContext(Dispatchers.IO) {
        insightDao.getAllInsightsOnce()
    }

    // [自适应] 判断是否达到自适应阈值
    suspend fun isAdaptiveReady(): Boolean = withContext(Dispatchers.IO) {
        behaviorDao.getCount() >= 50
    }

    // JSON 辅助方法
    private fun extractJsonString(json: String, key: String): String? {
        val pattern = """"$key"\s*:\s*"([^"]+)""""
        return Regex(pattern).find(json)?.groupValues?.get(1)
    }

    private fun extractJsonBool(json: String, key: String): Boolean {
        val pattern = """"$key"\s*:\s*(true|false)"""
        return Regex(pattern).find(json)?.groupValues?.get(1) == "true"
    }

    companion object {
        const val INSIGHT_TAG_FREQUENCY = "tag_frequency"
        const val INSIGHT_TIME_PATTERN = "time_pattern"
        const val INSIGHT_CONTENT_DISTRIBUTION = "content_distribution"
        const val INSIGHT_SEARCH_HOT = "search_hot"
        const val INSIGHT_AI_ADOPTION = "ai_adoption"
    }
}
