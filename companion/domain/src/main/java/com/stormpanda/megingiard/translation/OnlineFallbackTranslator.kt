package com.stormpanda.megingiard.translation

import com.stormpanda.megingiard.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val TAG = "OnlineFallbackTranslator"
private const val CONNECT_TIMEOUT_MS = 3000
private const val READ_TIMEOUT_MS = 3000

/**
 * Lightweight online translation fallback engine via REST API.
 */
class OnlineFallbackTranslator : TranslationEngine {
    override val name: String = "OnlineFallback"

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String,
    ): TranslationResult? = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return@withContext null

        val srcCode = when (sourceLang.lowercase()) {
            "ja" -> "ja"
            "en" -> "en"
            else -> "auto"
        }
        val targetCode = "zh-TW"

        // 1. Try Google Translate public endpoint
        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val urlString = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$srcCode&tl=$targetCode&dt=t&q=$encodedQuery"
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                // Google format: [[["translated text","orig text",...]],...]
                val parsed = json.parseToJsonElement(responseBody)
                val sentences = parsed as? kotlinx.serialization.json.JsonArray
                val firstArr = sentences?.getOrNull(0) as? kotlinx.serialization.json.JsonArray
                if (firstArr != null) {
                    val sb = java.lang.StringBuilder()
                    for (item in firstArr) {
                        val piece = (item as? kotlinx.serialization.json.JsonArray)?.getOrNull(0)
                        val textPiece = (piece as? kotlinx.serialization.json.JsonPrimitive)?.content
                        if (textPiece != null) sb.append(textPiece)
                    }
                    val trans = sb.toString().trim()
                    if (trans.isNotEmpty()) {
                        val finalTraditional = ZhTwConverter.convertToTaiwanTraditional(trans)
                        AppLog.i(TAG, "Google Translate success: '$trimmed' -> '$finalTraditional'")
                        return@withContext TranslationResult(
                            originalText = text,
                            translatedText = finalTraditional,
                            sourceLang = sourceLang,
                            targetLang = targetLang,
                            engineName = "GoogleTranslate",
                        )
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "Google Translate endpoint failed: ${e.message}")
        }

        // 2. Try MyMemory API fallback
        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val myMemSrc = if (srcCode == "auto") "autodetect" else srcCode
            val urlString = "https://api.mymemory.translated.net/get?q=$encodedQuery&langpair=$myMemSrc|$targetCode"
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("User-Agent", "Megingiard-Handheld/0.9.0")
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                val parsedJson = json.parseToJsonElement(responseBody).jsonObject
                val responseData = parsedJson["responseData"]?.jsonObject
                val translatedTextRaw = responseData?.get("translatedText")?.jsonPrimitive?.content

                if (!translatedTextRaw.isNullOrBlank()) {
                    val finalTraditional = ZhTwConverter.convertToTaiwanTraditional(translatedTextRaw)
                    AppLog.i(TAG, "MyMemory translation success: '$trimmed' -> '$finalTraditional'")
                    return@withContext TranslationResult(
                        originalText = text,
                        translatedText = finalTraditional,
                        sourceLang = sourceLang,
                        targetLang = targetLang,
                        engineName = name,
                    )
                }
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "MyMemory fallback failed: ${e.message}")
        }
        null
    }
}
