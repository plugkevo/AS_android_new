package com.kevann.africanshipping25.translation

import android.content.Context
import android.widget.TextView
import android.widget.Toast
import com.kevann.africanshipping25.translation.GoogleTranslationManager

class GoogleTranslationHelper(private val translationManager: GoogleTranslationManager) {

    fun translateAndSetText(textView: TextView, originalText: String, targetLanguage: String) {
        translationManager.translateText(originalText, targetLanguage) { translatedText ->
            textView.text = translatedText
        }
    }

    fun translateText(originalText: String, targetLanguage: String, callback: (String) -> Unit) {
        translationManager.translateText(originalText, targetLanguage, callback)
    }

    fun showTranslatedToast(context: Context, message: String, targetLanguage: String) {
        translationManager.translateText(message, targetLanguage) { translatedMessage ->
            Toast.makeText(context, translatedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    fun clearCache() {
        translationManager.clearCache()
    }
}
