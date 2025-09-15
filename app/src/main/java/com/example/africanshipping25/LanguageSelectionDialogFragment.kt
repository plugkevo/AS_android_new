package com.example.africanshipping25

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class LanguageSelectionDialogFragment : DialogFragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private var onLanguageSelectedListener: OnLanguageSelectedListener? = null

    companion object {
        private const val TAG = "LanguageSelectionDialog"
    }

    interface OnLanguageSelectedListener {
        fun onLanguageSelected(languageCode: String, languageName: String)
    }

    fun setOnLanguageSelectedListener(listener: OnLanguageSelectedListener) {
        this.onLanguageSelectedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        val languages = arrayOf(
            "English",
            "Français",
            "Español",
            "Português",
            "العربية",
            "Kiswahili",
            "አማርኛ",
            "Hausa",
            "Yoruba",
            "Igbo"
        )

        val languageCodes = arrayOf(
            "en",
            "fr",
            "es",
            "pt",
            "ar",
            "sw",
            "am",
            "ha",
            "yo",
            "ig"
        )

        val currentLanguageCode = sharedPreferences.getString("selected_language", "en") ?: "en"
        val currentIndex = languageCodes.indexOf(currentLanguageCode)

        Log.d(TAG, "Current language: $currentLanguageCode, Index: $currentIndex")

        return AlertDialog.Builder(requireContext())
            .setTitle("Select Language")
            .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                val selectedLanguageCode = languageCodes[which]
                val selectedLanguageName = languages[which]

                Log.d(TAG, "Language selected: $selectedLanguageCode ($selectedLanguageName)")

                // Save to SharedPreferences
                sharedPreferences.edit()
                    .putString("selected_language", selectedLanguageCode)
                    .apply()

                // Notify listener
                onLanguageSelectedListener?.onLanguageSelected(selectedLanguageCode, selectedLanguageName)

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }
}