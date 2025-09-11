package com.example.africanshipping25

import android.widget.TextView

class GoogleTranslationHelper(private val translationManager: GoogleTranslationManager) {

    fun translateAndSetText(textView: TextView, originalText: String, targetLanguage: String) {
        translationManager.translateText(originalText, targetLanguage) { translatedText ->
            textView.text = translatedText
        }
    }

    fun translateText(originalText: String, targetLanguage: String, callback: (String) -> Unit) {
        translationManager.translateText(originalText, targetLanguage, callback)
    }

    fun clearCache() {
        translationManager.clearCache()
    }
}