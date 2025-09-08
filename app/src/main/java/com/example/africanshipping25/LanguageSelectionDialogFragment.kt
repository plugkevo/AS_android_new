package com.example.africanshipping25

import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class LanguageSelectionDialogFragment : DialogFragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private var onLanguageSelectedListener: OnLanguageSelectedListener? = null

    interface OnLanguageSelectedListener {
        fun onLanguageSelected(language: String)
    }

    fun setOnLanguageSelectedListener(listener: OnLanguageSelectedListener) {
        this.onLanguageSelectedListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_language_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("app_preferences", 0)

        // Initialize UI elements
        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group_languages)
        val btnOk = view.findViewById<Button>(R.id.btn_ok)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)

        // Set current language selection
        setCurrentLanguageSelection(radioGroup)

        // Set up click listeners
        btnOk.setOnClickListener {
            val selectedLanguage = getSelectedLanguage(radioGroup)
            if (selectedLanguage != null) {
                // Save to SharedPreferences
                sharedPreferences.edit()
                    .putString("language", selectedLanguage)
                    .apply()

                // Notify listener
                onLanguageSelectedListener?.onLanguageSelected(selectedLanguage)

                // Show confirmation
                Toast.makeText(context, "Language changed to $selectedLanguage", Toast.LENGTH_SHORT).show()

                dismiss()
            } else {
                Toast.makeText(context, "Please select a language", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun setCurrentLanguageSelection(radioGroup: RadioGroup) {
        val currentLanguage = sharedPreferences.getString("language", "English")

        val radioButtonId = when (currentLanguage) {
            "English" -> R.id.radio_english
            "French" -> R.id.radio_french
            "Spanish" -> R.id.radio_spanish
            "Portuguese" -> R.id.radio_portuguese
            "Arabic" -> R.id.radio_arabic
            "Swahili" -> R.id.radio_swahili
            else -> R.id.radio_english
        }

        radioGroup.check(radioButtonId)
    }

    private fun getSelectedLanguage(radioGroup: RadioGroup): String? {
        val selectedId = radioGroup.checkedRadioButtonId

        return when (selectedId) {
            R.id.radio_english -> "English"
            R.id.radio_french -> "French"
            R.id.radio_spanish -> "Spanish"
            R.id.radio_portuguese -> "Portuguese"
            R.id.radio_arabic -> "Arabic"
            R.id.radio_swahili -> "Swahili"
            else -> null
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}