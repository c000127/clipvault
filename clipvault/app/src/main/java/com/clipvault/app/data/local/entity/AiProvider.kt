package com.clipvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

const val DEFAULT_SYSTEM_PROMPT = """You are an AI assistant for a personal knowledge collection app called ClipVault.
Given the user's saved content, please:
1. Write a concise summary (2-3 sentences in the user's language).
2. Suggest 3-5 relevant tags for categorization. Tags should be hierarchical if appropriate (e.g., "Work/ProjectA").
Return in JSON format:
{"summary": "...", "suggested_tags": ["tag1", "Work/ProjectB", "tag3"]}"""

@Entity(tableName = "ai_providers")
data class AiProvider(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                       // 用户自定义名称，如 "My GPT"
    val baseUrl: String,                    // 如 https://api.openai.com/v1（末尾不带 /chat/completions）
    val apiKey: String = "",             // Room 中不存实际密钥，存储空字符串或占位符。实际 API Key 存储在 DataStore 中，key 格式 "api_key_{providerId}"
    val modelName: String,                  // 如 gpt-4o-mini（传递 exact ID）
    val supportsVision: Boolean = false,    // 模型是否支持图片理解（对应 OpenClaw 的 input:["text","image"]）
    val maxTokens: Int = 4096,              // 最大输出 token 数
    val temperature: Float = 0.7f,           // 生成温度
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val isActive: Boolean = false            // 当前激活的配置
)
