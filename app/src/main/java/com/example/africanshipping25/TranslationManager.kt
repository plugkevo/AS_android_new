package com.example.africanshipping25

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslationManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    private var currentTranslator: Translator? = null
    private var isModelDownloaded = false

    companion object {
        private const val TAG = "TranslationManager"

        // Supported languages for your app
        val SUPPORTED_LANGUAGES = mapOf(
            "English" to TranslateLanguage.ENGLISH,
            "French" to TranslateLanguage.FRENCH,
            "Spanish" to TranslateLanguage.SPANISH,
            "Portuguese" to TranslateLanguage.PORTUGUESE,
            "Arabic" to TranslateLanguage.ARABIC,
            "Swahili" to TranslateLanguage.SWAHILI
        )
    }

    fun initializeTranslator(targetLanguage: String, onReady: (() -> Unit)? = null) {
        val targetLangCode = SUPPORTED_LANGUAGES[targetLanguage] ?: TranslateLanguage.ENGLISH

        // Don't reinitialize if same language
        if (getCurrentLanguage() == targetLanguage && currentTranslator != null) {
            onReady?.invoke()
            return
        }

        // Close existing translator
        currentTranslator?.close()
        isModelDownloaded = false

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetLangCode)
            .build()

        currentTranslator = Translation.getClient(options)

        // Download model if needed
        downloadModelIfNeeded(onReady)
    }

    private fun downloadModelIfNeeded(onReady: (() -> Unit)? = null) {
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        currentTranslator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener {
                Log.d(TAG, "Translation model downloaded successfully")
                isModelDownloaded = true
                onReady?.invoke()
            }
            ?.addOnFailureListener { exception ->
                Log.e(TAG, "Error downloading translation model", exception)
                isModelDownloaded = false
                onReady?.invoke() // Still call onReady even if download fails
            }
    }

    fun translateText(text: String, callback: (String) -> Unit) {
        val currentLang = getCurrentLanguage()

        // If English or translation not available, return original text
        if (currentLang == "English" || currentTranslator == null) {
            callback(text)
            return
        }

        currentTranslator?.translate(text)
            ?.addOnSuccessListener { translatedText ->
                callback(translatedText)
            }
            ?.addOnFailureListener { exception ->
                Log.e(TAG, "Translation failed for text: $text", exception)
                callback(text) // Return original text on failure
            }
    }

    fun translateTextList(textList: List<String>, callback: (List<String>) -> Unit) {
        val currentLang = getCurrentLanguage()

        if (currentLang == "English" || currentTranslator == null) {
            callback(textList)
            return
        }

        val translatedList = mutableListOf<String>()
        var completedCount = 0

        textList.forEach { text ->
            translateText(text) { translatedText ->
                translatedList.add(translatedText)
                completedCount++

                if (completedCount == textList.size) {
                    callback(translatedList)
                }
            }
        }
    }

    fun getCurrentLanguage(): String {
        return sharedPreferences.getString("language", "English") ?: "English"
    }

    fun isTranslationAvailable(language: String): Boolean {
        return SUPPORTED_LANGUAGES.containsKey(language)
    }

    fun isModelReady(): Boolean {
        return isModelDownloaded && currentTranslator != null
    }

    fun cleanup() {
        currentTranslator?.close()
        currentTranslator = null
        isModelDownloaded = false
    }
}