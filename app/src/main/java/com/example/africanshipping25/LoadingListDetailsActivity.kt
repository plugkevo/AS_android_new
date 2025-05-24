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
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_loading_list_details)

        // Initialize UI elements using findViewById
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tbLayout)

        // Retrieve the ID from the Intent FIRST
        val loadingListId = intent.getStringExtra("loadingListId")

        if (loadingListId != null) {
            Log.d("LoadingListDetail", "Received Loading List ID: $loadingListId")

            val adapter = ViewPagerAdapter(supportFragmentManager)

            // Pass the loadingListId to the EnterWarehouseGoods fragment using its newInstance method
            adapter.addFragment(EnterWarehouseGoods.newInstance(loadingListId), "Enter Goods")
            adapter.addFragment(ViewWarehouseGoods(), "View Goods") // Pass ID to ViewWarehouseGoods if needed too
            //adapter.addFragment(ThirdFragment(), "My Tenders")

            // Set the adapter to the ViewPager
            viewPager.adapter = adapter
            // Link the TabLayout with the ViewPager
            tabLayout.setupWithViewPager(viewPager)

        } else {
            Log.e("LoadingListDetail", "No Loading List ID received!")
            // Handle the case where no ID is passed, e.g., show an error message or finish the activity
            Toast.makeText(this, "Error: Loading List ID missing.", Toast.LENGTH_SHORT).show()
            finish() // Close this activity
        }
    }
}