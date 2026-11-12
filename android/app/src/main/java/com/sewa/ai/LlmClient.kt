package com.sewa.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object LlmClient {
    private const val TAG = "SewaLlmClient"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 30_000

    suspend fun reply(userText: String, recentMessages: List<String>): String = withContext(Dispatchers.IO) {
        val endpoint = BuildConfig.LLM_ENDPOINT
        if (endpoint.contains("YOUR-SERVICE")) {
            return@withContext "Ye message local command nahi hai. LLM ke liye Render API URL ko LlmClient.kt mein configure karna hoga."
        }

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        try {
            val messages = JSONArray()
            recentMessages.takeLast(8).forEach { line ->
                val role = if (line.startsWith("You:")) "user" else "assistant"
                messages.put(JSONObject().put("role", role).put("content", line.substringAfter(": ").trim()))
            }
            messages.put(JSONObject().put("role", "user").put("content", userText))

            connection.outputStream.bufferedWriter().use {
                it.write(JSONObject().put("messages", messages).toString())
            }

            val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299) {
                Log.w(TAG, "LLM request failed with HTTP ${connection.responseCode}")
                return@withContext "LLM service abhi available nahi hai. Thodi der baad try karein."
            }
            JSONObject(body).optString("reply").takeIf { it.isNotBlank() }
                ?: "LLM ne valid response nahi diya."
        } catch (e: IOException) {
            Log.w(TAG, "LLM network request failed", e)
            "Internet ya LLM service available nahi hai. Local commands abhi bhi kaam karenge."
        } catch (e: Exception) {
            Log.e(TAG, "LLM response parse failed", e)
            "LLM response samajh nahi aaya. Dobara try karein."
        } finally {
            connection.disconnect()
        }
    }
}
