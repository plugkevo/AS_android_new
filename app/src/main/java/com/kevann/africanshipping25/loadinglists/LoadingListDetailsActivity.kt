package com.kevann.africanshipping25.loadinglists

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.kevann.africanshipping25.ViewPagerAdapter
import com.kevann.africanshipping25.R
import com.kevann.africanshipping25.translation.GoogleTranslationManager
import com.kevann.africanshipping25.translation.GoogleTranslationHelper


class LoadingListDetailsActivity : AppCompatActivity() {

    // Declare your UI elements
    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout
    private lateinit var headerTitle: TextView

    private lateinit var translationManager: GoogleTranslationManager
    private lateinit var translationHelper: GoogleTranslationHelper
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading_list_details)

        // Initialize translation
        translationManager = GoogleTranslationManager(this)
        translationHelper = GoogleTranslationHelper(translationManager)
        sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        // Initialize UI elements using findViewById
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tbLayout)
        headerTitle = findViewById(R.id.tv_header_title)

        // Retrieve the ID from the Intent FIRST
        val loadingListId = intent.getStringExtra("loadingListId")

        if (loadingListId != null) {
            Log.d("LoadingListDetail", "Received Loading List ID: $loadingListId")

            val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"

            // Translate tab titles FIRST, then add fragments, then set adapter
            val adapter = ViewPagerAdapter(supportFragmentManager)
            translateUIElements(currentLanguage)

            translationHelper.translateText("Enter Goods", currentLanguage) { enterGoodsTitle ->
                translationHelper.translateText("View Goods", currentLanguage) { viewGoodsTitle ->
                    // Add fragments to adapter
                    adapter.addFragment(EnterWarehouseGoods.Companion.newInstance(loadingListId), enterGoodsTitle)
                    adapter.addFragment(ViewWarehouseGoods.Companion.newInstance(loadingListId), viewGoodsTitle)

                    // NOW set the adapter to the ViewPager (after fragments are added)
                    viewPager.adapter = adapter
                    // Link the TabLayout with the ViewPager
                    tabLayout.setupWithViewPager(viewPager)
                }
            }

        } else {
            Log.e("LoadingListDetail", "No Loading List ID received!")
            val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
            var errorMsg = "Error: Loading List ID missing."
            translationHelper.translateText(errorMsg, currentLanguage) { translated ->
                Toast.makeText(this, translated, Toast.LENGTH_SHORT).show()
            }
            finish() // Close this activity
        }
    }

    private fun translateUIElements(targetLanguage: String) {
        translationHelper.translateAndSetText(headerTitle, "Loading List", targetLanguage)
    }
}
