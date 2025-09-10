package com.example.africanshipping25

import android.widget.TextView
import android.widget.Button
import android.view.View

class TranslationHelper(private val translationManager: TranslationManager) {

    // Common UI translations cache
    private val translationCache = mutableMapOf<String, String>()

    fun translateAndSetText(textView: TextView, originalText: String) {
        // Check cache first
        if (translationCache.containsKey(originalText)) {
            textView.text = translationCache[originalText]
            return
        }

        translationManager.translateText(originalText) { translatedText ->
            translationCache[originalText] = translatedText
            textView.text = translatedText
        }
    }

    fun translateAndSetText(button: Button, originalText: String) {
        // Check cache first
        if (translationCache.containsKey(originalText)) {
            button.text = translationCache[originalText]
            return
        }

        translationManager.translateText(originalText) { translatedText ->
            translationCache[originalText] = translatedText
            button.text = translatedText
        }
    }

    // Batch translate multiple views at once
    fun translateViews(viewsWithText: Map<View, String>) {
        val textsToTranslate = viewsWithText.values.toList()

        translationManager.translateTextList(textsToTranslate) { translatedTexts ->
            viewsWithText.entries.forEachIndexed { index, (view, _) ->
                val translatedText = translatedTexts[index]
                when (view) {
                    is TextView -> view.text = translatedText
                    is Button -> view.text = translatedText
                }
            }
        }
    }

    // Helper method for common profile screen translations
    fun translateProfileScreen(
        editProfileLabel: TextView?,
        changePasswordLabel: TextView?,
        addressBookLabel: TextView?,
        notificationsLabel: TextView?,
        languageLabel: TextView?,
        themeLabel: TextView?,
        helpSupportLabel: TextView?,
        privacyPolicyLabel: TextView?,
        termsServiceLabel: TextView?,
        aboutLabel: TextView?
    ) {
        val viewsToTranslate = mutableMapOf<View, String>()

        editProfileLabel?.let { viewsToTranslate[it] = "Edit Profile" }
        changePasswordLabel?.let { viewsToTranslate[it] = "Change Password" }
        addressBookLabel?.let { viewsToTranslate[it] = "Address Book" }
        notificationsLabel?.let { viewsToTranslate[it] = "Notifications" }
        languageLabel?.let { viewsToTranslate[it] = "Language" }
        themeLabel?.let { viewsToTranslate[it] = "Theme" }
        helpSupportLabel?.let { viewsToTranslate[it] = "Help & Support" }
        privacyPolicyLabel?.let { viewsToTranslate[it] = "Privacy Policy" }
        termsServiceLabel?.let { viewsToTranslate[it] = "Terms of Service" }
        aboutLabel?.let { viewsToTranslate[it] = "About" }

        translateViews(viewsToTranslate)
    }

    // Helper for shipment status translations
    fun translateShipmentStatus(status: String, callback: (String) -> Unit) {
        val statusTranslations = mapOf(
            "Delivered" to "Delivered",
            "In Transit" to "In Transit",
            "Processing" to "Processing",
            "Pending" to "Pending",
            "Out for Delivery" to "Out for Delivery",
            "Picked Up" to "Picked Up"
        )

        val textToTranslate = statusTranslations[status] ?: status
        translationManager.translateText(textToTranslate, callback)
    }

    // Clear cache when language changes
    fun clearCache() {
        translationCache.clear()
    }
}