package com.kevann.africanshipping25.translation

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap

class GoogleTranslationManager(private val context: Context) {

    private val translationService: TranslationService
    private val translationCache = ConcurrentHashMap<String, String>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Replace with your actual Google Cloud Translation API key
    private val apiKey = "AIzaSyAjucN5j7cxxL6RXP3gTUSVdNGQYvt5Row"

    companion object {
        private const val TAG = "GoogleTranslationManager"
        private const val BASE_URL = "https://translation.googleapis.com/"
    }

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        translationService = retrofit.create(TranslationService::class.java)
    }

    fun translateText(
        text: String,
        targetLanguage: String,
        callback: (String) -> Unit
    ) {
        // Check cache first
        val cacheKey = "${text}_${targetLanguage}"
        translationCache[cacheKey]?.let { cachedTranslation ->
            callback(cachedTranslation)
            return
        }

        // If target language is English, return original text
        if (targetLanguage.lowercase() == "english" || targetLanguage == "en") {
            callback(text)
            return
        }

        scope.launch {
            try {
                val targetCode = getLanguageCode(targetLanguage)
                val request = TranslationRequest(
                    q = listOf(text),
                    target = targetCode,
                    source = "en"
                )

                val response = translationService.translateText(apiKey, request)
                val translatedText = response.data.translations.firstOrNull()?.translatedText ?: text

                // Cache the translation
                translationCache[cacheKey] = translatedText

                withContext(Dispatchers.Main) {
                    callback(translatedText)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Translation failed for text: $text", e)
                withContext(Dispatchers.Main) {
                    callback(text) // Return original text on error
                }
            }
        }
    }

    fun translateMultipleTexts(
        texts: List<String>,
        targetLanguage: String,
        callback: (List<String>) -> Unit
    ) {
        if (targetLanguage.lowercase() == "english" || targetLanguage == "en") {
            callback(texts)
            return
        }

        scope.launch {
            try {
                val targetCode = getLanguageCode(targetLanguage)
                val request = TranslationRequest(
                    q = texts,
                    target = targetCode,
                    source = "en"
                )

                val response = translationService.translateText(apiKey, request)
                val translatedTexts = response.data.translations.map { it.translatedText }

                // Cache translations
                texts.forEachIndexed { index, originalText ->
                    val cacheKey = "${originalText}_${targetLanguage}"
                    if (index < translatedTexts.size) {
                        translationCache[cacheKey] = translatedTexts[index]
                    }
                }

                withContext(Dispatchers.Main) {
                    callback(translatedTexts)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Batch translation failed", e)
                withContext(Dispatchers.Main) {
                    callback(texts) // Return original texts on error
                }
            }
        }
    }

    private fun getLanguageCode(language: String): String {
        return when (language.lowercase()) {
            "english" -> "en"
            "french" -> "fr"
            "spanish" -> "es"
            "portuguese" -> "pt"
            "arabic" -> "ar"
            "swahili" -> "sw"
            "german" -> "de"
            "italian" -> "it"
            "chinese" -> "zh"
            "japanese" -> "ja"
            "korean" -> "ko"
            "russian" -> "ru"
            "hindi" -> "hi"
            else -> "en"
        }
    }

    fun clearCache() {
        translationCache.clear()
    }

    fun cleanup() {
        scope.cancel()
        translationCache.clear()
    }
}