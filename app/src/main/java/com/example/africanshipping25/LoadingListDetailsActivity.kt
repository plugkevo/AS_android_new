package com.example.africanshipping25

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager // Import ViewPager
import com.google.android.material.tabs.TabLayout // Import TabLayout

class LoadingListDetailsActivity : AppCompatActivity() {

    // Declare your UI elements
    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout // Assuming your TabLayout ID is 'tbLayout' or similar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_loading_list_details)

        // Initialize UI elements using findViewById
        viewPager = findViewById(R.id.viewPager) // Make sure 'viewPager' is the ID in your XML
        tabLayout = findViewById(R.id.tbLayout)   // Make sure 'tbLayout' is the ID in your XML

        val adapter = ViewPagerAdapter(supportFragmentManager)

        adapter.addFragment(EnterWarehouseGoods(), "Enter Goods")
        adapter.addFragment(ViewWarehouseGoods(), "View Goods")
        //adapter.addFragment(ThirdFragment(), "My Tenders")

        // Set the adapter to the ViewPager
        viewPager.adapter = adapter
        // Link the TabLayout with the ViewPager
        tabLayout.setupWithViewPager(viewPager)

        // Retrieve the ID from the Intent
        val loadingListId = intent.getStringExtra("loadingListId")

        if (loadingListId != null) {
            Log.d("LoadingListDetail", "Received Loading List ID: $loadingListId")
            // Now you can use this ID to fetch the full details of the loading list from Firestore
            // For example:
            // FirebaseFirestore.getInstance().collection("loading_lists").document(loadingListId).get()...
        } else {
            Log.e("LoadingListDetail", "No Loading List ID received!")
            // Handle the case where no ID is passed, e.g., show an error message or finish the activity
            Toast.makeText(this, "Error: Loading List ID missing.", Toast.LENGTH_SHORT).show()
            finish() // Close this activity
        }
    }
}