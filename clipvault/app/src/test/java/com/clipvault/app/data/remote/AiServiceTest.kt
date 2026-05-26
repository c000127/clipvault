package com.clipvault.app.data.remote

import com.clipvault.app.data.local.entity.AiProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AiServiceTest {

    @Test
    fun testMiMoConnection() = runTest {
        val service = AiService()
        val provider = AiProvider(
            name = "小米 MiMo",
            baseUrl = "https://token-plan-cn.xiaomimimo.com/v1",
            apiKey = "",
            modelName = "mimo-v2.5",
            supportsVision = false
        )
        val apiKey = System.getenv("MIMO_API_KEY") ?: "YOUR_API_KEY_HERE"

        println("Testing connection with MiMo API...")
        val testResult = service.testConnection(provider, apiKey)
        println("Connection result: $testResult")
        assertTrue(testResult is AiResult.Success)

        println("Testing analysis with MiMo API...")
        val analyzeResult = service.analyze(
            provider = provider,
            apiKey = apiKey,
            content = "Say hello and suggest three tags in JSON format matching DEFAULT_SYSTEM_PROMPT requirements",
            contentType = "text"
        )
        println("Analysis result: $analyzeResult")
        assertTrue(analyzeResult is AiResult.Success)
    }
}
