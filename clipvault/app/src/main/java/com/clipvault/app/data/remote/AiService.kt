package com.clipvault.app.data.remote

import android.util.Base64
import com.clipvault.app.data.local.entity.AiProvider
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AiResult {
    data class Success(val summary: String, val suggestedTags: List<String>) : AiResult
    data class Error(val message: String) : AiResult
}

@Singleton
class AiService @Inject constructor() {

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Build the chat completions URL from the provider's base URL.
     * Handles all common input variants:
     *   - https://api.openai.com/v1
     *   - https://api.openai.com/v1/
     *   - https://api.openai.com
     *   - https://api.openai.com/
     *   - https://api.openai.com/v1/chat/completions
     */
    private fun buildChatUrl(baseUrl: String): String {
        var url = baseUrl.trimEnd('/')

        // If URL already ends with /chat/completions, use as-is
        if (url.endsWith("/chat/completions")) return url

        // Remove trailing /v1 if present (we'll add it back)
        if (url.endsWith("/v1")) {
            url = url.removeSuffix("/v1")
        }

        // Build final URL
        return "$url/v1/chat/completions"
    }

    suspend fun analyze(
        provider: AiProvider,
        apiKey: String,
        content: String,
        contentType: String,
        imagePath: String? = null
    ): AiResult = withContext(Dispatchers.IO) {
        try {
            val url = buildChatUrl(provider.baseUrl)

            val messages = buildMessages(provider.systemPrompt, content, contentType, imagePath, provider.supportsVision)
            val requestBody = buildRequestBody(provider, messages)

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            when (response.code) {
                401, 403 -> AiResult.Error("API Key 无效或已过期")
                429 -> AiResult.Error("请求频率过高")
                in 500..599 -> AiResult.Error("AI 服务暂时不可用")
                200 -> parseResponse(responseBody)
                else -> AiResult.Error("请求失败: HTTP ${response.code}")
            }
        } catch (e: java.net.SocketTimeoutException) {
            AiResult.Error("请求超时")
        } catch (e: java.io.IOException) {
            AiResult.Error("网络错误: ${e.message}")
        } catch (e: Exception) {
            AiResult.Error("未知错误: ${e.message}")
        }
    }

    suspend fun testConnection(
        provider: AiProvider,
        apiKey: String
    ): AiResult = withContext(Dispatchers.IO) {
        try {
            val url = buildChatUrl(provider.baseUrl)

            val messages = listOf(
                mapOf("role" to "user", "content" to "Say 'Hello'")
            )
            val requestBody = buildRequestBody(provider, messages)

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            when (response.code) {
                401, 403 -> AiResult.Error("API Key 无效或已过期")
                429 -> AiResult.Error("请求频率过高")
                in 500..599 -> AiResult.Error("AI 服务暂时不可用")
                200 -> AiResult.Success("Connection successful", emptyList())
                else -> AiResult.Error("测试失败: HTTP ${response.code}")
            }
        } catch (e: java.net.SocketTimeoutException) {
            AiResult.Error("连接超时")
        } catch (e: java.io.IOException) {
            AiResult.Error("网络错误: ${e.message}")
        } catch (e: Exception) {
            AiResult.Error("未知错误: ${e.message}")
        }
    }

    private fun buildMessages(
        systemPrompt: String,
        content: String,
        contentType: String,
        imagePath: String?,
        supportsVision: Boolean
    ): List<Map<String, Any>> {
        val messages = mutableListOf<Map<String, Any>>()

        // System message
        messages.add(mapOf("role" to "system", "content" to systemPrompt))

        // User message based on content type
        when (contentType) {
            "image" -> {
                if (supportsVision && imagePath != null) {
                    val base64 = encodeImageBase64(imagePath)
                    if (base64 != null) {
                        messages.add(mapOf(
                            "role" to "user",
                            "content" to listOf(
                                mapOf("type" to "text", "text" to "分析这张图片"),
                                mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:image/jpeg;base64,$base64"))
                            )
                        ))
                    } else {
                        messages.add(mapOf("role" to "user", "content" to "[图片收藏] 无法读取图片"))
                    }
                } else {
                    messages.add(mapOf("role" to "user", "content" to "[图片收藏] $content"))
                }
            }
            "link" -> {
                messages.add(mapOf("role" to "user", "content" to "URL: $content"))
            }
            else -> {
                val truncated = if (content.length > 32000) {
                    content.take(32000) + "\n[内容已截断]"
                } else {
                    content
                }
                messages.add(mapOf("role" to "user", "content" to truncated))
            }
        }

        return messages
    }

    private fun buildRequestBody(provider: AiProvider, messages: List<Map<String, Any>>): String {
        val body = mapOf(
            "model" to provider.modelName,
            "messages" to messages,
            "temperature" to provider.temperature,
            "max_tokens" to provider.maxTokens,
            "stream" to false
        )
        return gson.toJson(body)
    }

    private fun parseResponse(responseBody: String): AiResult {
        return try {
            // Check if SSE format
            val content = if (responseBody.startsWith("data:")) {
                val lastData = responseBody.lines()
                    .filter { it.startsWith("data:") }
                    .lastOrNull { !it.contains("[DONE]") }
                    ?.removePrefix("data:")
                    ?.trim()
                lastData ?: responseBody
            } else {
                responseBody
            }

            val json = JsonParser.parseString(content).asJsonObject
            val choices = json.getAsJsonArray("choices")
            if (choices == null || choices.size() == 0) {
                return AiResult.Error("AI 未返回有效内容")
            }

            val message = choices[0].asJsonObject.getAsJsonObject("message")
            val messageContent = message?.get("content")?.asString
            if (messageContent.isNullOrBlank()) {
                return AiResult.Error("AI 未返回有效内容")
            }

            // Try to parse JSON response
            try {
                val aiResponse = JsonParser.parseString(messageContent).asJsonObject
                val summary = aiResponse.get("summary")?.asString ?: messageContent
                val tags = aiResponse.getAsJsonArray("suggested_tags")
                    ?.map { it.asString }
                    ?: emptyList()
                AiResult.Success(summary, tags)
            } catch (e: Exception) {
                // Not JSON, use full response as summary
                AiResult.Success(messageContent, emptyList())
            }
        } catch (e: Exception) {
            AiResult.Error("解析响应失败: ${e.message}")
        }
    }

    private fun encodeImageBase64(imagePath: String): String? {
        return try {
            val file = java.io.File(imagePath)
            if (!file.exists()) return null

            // Check file size, compress if needed
            val bytes = if (file.length() > 1024 * 1024) {
                compressImage(imagePath)
            } else {
                file.readBytes()
            }

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun compressImage(imagePath: String): ByteArray {
        val file = java.io.File(imagePath)
        val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
            ?: return file.readBytes()

        val maxDim = 1024
        val scale = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1f)
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()

        val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        val output = java.io.ByteArrayOutputStream()
        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, output)

        bitmap.recycle()
        scaled.recycle()

        return output.toByteArray()
    }
}
