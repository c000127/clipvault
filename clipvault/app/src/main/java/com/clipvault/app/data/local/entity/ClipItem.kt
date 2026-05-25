package com.clipvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ClipItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,           // "text" | "image" | "link" | "media"
    val content: String,        // 文本内容 / 本地文件路径 / URL / 图片路径
    val note: String = "",      // 用户备注（AI 总结也追加至此）
    val thumbnailPath: String = "",  // 缩略图本地路径
    val fetchedContent: String = "", // Jsoup 抓取的页面纯文本，link 类型专用
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sourceApp: String = ""  // 来源应用包名（可选记录）
)
