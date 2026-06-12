package com.clipvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// [自适应] 层级2: 环境因素 — 用户洞察实体
// 由 BehaviorLog 聚合而成，当 sampleCount >= 50 时启用自适应调整
@Entity(
    tableName = "user_insights",
    indices = [Index("insightType", "key", unique = true)]
)
data class UserInsight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val insightType: String,    // "tag_frequency" | "time_pattern" | "content_distribution" | "search_hot" | "ai_adoption"
    val key: String,            // 维度键: Tag名、小时、内容类型、搜索词等
    val value: Float,           // 权重/频率/百分比
    val sampleCount: Int,       // 样本量（>= 50 才启用自适应）
    val lastUpdated: Long = System.currentTimeMillis()
)
