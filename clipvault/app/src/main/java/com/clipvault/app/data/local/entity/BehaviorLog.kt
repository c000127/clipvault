package com.clipvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// [自适应] 层级2: 环境因素 — 行为采集实体
// 记录用户关键操作事件，用于推断使用习惯和偏好
@Entity(
    tableName = "behavior_logs",
    indices = [Index("eventType"), Index("timestamp"), Index("sessionId")]
)
data class BehaviorLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,      // "收藏" | "搜索" | "AI总结" | "Tag操作" | "阅读" | "页面访问"
    val metadata: String,       // JSON: 搜索词、Tag ID、内容类型、页面名等
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String       // 会话标识，用于关联行为序列
)
