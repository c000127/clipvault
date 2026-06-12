package com.clipvault.app.data.behavior

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

// [自适应] 层级3: 人生阅历 — 生命周期阶段管理
// 根据首次安装时间和使用时长，决定系统行为的演化程度

// [自适应] 生命周期阶段枚举
enum class LifecycleStage {
    NEWBORN,    // 0-7天: 保守默认布局，积极引导Tag体系，AI功能默认关闭
    GROWTH,     // 1-4周: 采集行为数据但不主动调整UI，推荐Tag结构优化
    MATURE,     // 1-3月: 启用自适应布局，根据行为数据调整排序和优先级
    EVOLUTION;  // 3月+: 完全个性化，时间线重排，AI Prompt根据历史采纳率自进化

    val isAdaptiveEnabled: Boolean
        get() = this == MATURE || this == EVOLUTION

    val showGuidance: Boolean
        get() = this == NEWBORN || this == GROWTH

    val tag: String
        get() = when (this) {
            NEWBORN -> "新生期"
            GROWTH -> "成长期"
            MATURE -> "成熟期"
            EVOLUTION -> "演化期"
        }
}

private val Context.lifecycleDataStore: DataStore<Preferences> by preferencesDataStore(name = "lifecycle_prefs")

// [自适应] 生命周期阶段管理器
@Singleton
class LifecycleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val insightEngine: InsightEngine
) {
    companion object {
        private val FIRST_INSTALL_TIME = longPreferencesKey("first_install_time")
        private val LAST_SESSION_TIME = longPreferencesKey("last_session_time")
        private val SESSION_ID = stringPreferencesKey("current_session_id")
    }

    // [自适应] 获取首次安装时间（首次运行时自动记录）
    suspend fun getFirstInstallTime(): Long {
        val prefs = context.lifecycleDataStore.data.first()
        val stored = prefs[FIRST_INSTALL_TIME]
        if (stored == null) {
            val now = System.currentTimeMillis()
            context.lifecycleDataStore.edit { it[FIRST_INSTALL_TIME] = now }
            return now
        }
        return stored
    }

    // [自适应] 判断当前生命周期阶段
    suspend fun getCurrentStage(): LifecycleStage {
        val installTime = getFirstInstallTime()
        val daysSinceInstall = (System.currentTimeMillis() - installTime) / (24.0 * 60 * 60 * 1000)

        return when {
            daysSinceInstall < 7 -> LifecycleStage.NEWBORN
            daysSinceInstall < 28 -> LifecycleStage.GROWTH
            daysSinceInstall < 90 -> LifecycleStage.MATURE
            else -> LifecycleStage.EVOLUTION
        }
    }

    // [自适应] Flow 版本，用于 Compose 观察
    fun currentStageFlow(): Flow<LifecycleStage> {
        return context.lifecycleDataStore.data.map { prefs ->
            val installTime = prefs[FIRST_INSTALL_TIME] ?: System.currentTimeMillis()
            val daysSinceInstall = (System.currentTimeMillis() - installTime) / (24.0 * 24 * 60 * 60 * 1000)

            when {
                daysSinceInstall < 7 -> LifecycleStage.NEWBORN
                daysSinceInstall < 28 -> LifecycleStage.GROWTH
                daysSinceInstall < 90 -> LifecycleStage.MATURE
                else -> LifecycleStage.EVOLUTION
            }
        }
    }

    // [自适应] 会话记忆：记录并恢复上次会话状态
    suspend fun onSessionStart() {
        val now = System.currentTimeMillis()
        context.lifecycleDataStore.edit { prefs ->
            prefs[LAST_SESSION_TIME] = now
        }
    }

    suspend fun getLastSessionTime(): Long? {
        val prefs = context.lifecycleDataStore.data.first()
        return prefs[LAST_SESSION_TIME]
    }

    // [自适应] 获取使用天数
    suspend fun getDaysSinceInstall(): Int {
        val installTime = getFirstInstallTime()
        return ((System.currentTimeMillis() - installTime) / (24.0 * 60 * 60 * 1000)).toInt()
    }

    // [自适应] 判断是否应该启用自适应（综合考虑阶段和样本量）
    suspend fun shouldEnableAdaptive(): Boolean {
        val stage = getCurrentStage()
        if (!stage.isAdaptiveEnabled) return false
        return insightEngine.isAdaptiveReady()
    }

    // [自适应] 获取阶段描述信息
    suspend fun getStageInfo(): StageInfo {
        val stage = getCurrentStage()
        val days = getDaysSinceInstall()
        val adaptiveEnabled = shouldEnableAdaptive()
        return StageInfo(stage, days, adaptiveEnabled)
    }
}

data class StageInfo(
    val stage: LifecycleStage,
    val daysSinceInstall: Int,
    val adaptiveEnabled: Boolean
)
