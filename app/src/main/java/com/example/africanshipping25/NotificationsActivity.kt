package com.example.africanshipping25

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout

class NotificationsActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)

        // Initialize UI elements using findViewById
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tbLayout)

        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_blue)

        val adapter = ViewPagerAdapter(supportFragmentManager)

        // Pass the loadingListId to the EnterWarehouseGoods fragment using its newInstance method
        adapter.addFragment(ViewNotificationsFragment(), "View Notifications")
        adapter.addFragment(SendNotificationsFragment(), "Send Notifications") // Pass ID to ViewWarehouseGoods if needed too
        //adapter.addFragment(ThirdFragment(), "My Tenders")

        // Set the adapter to the ViewPager
        viewPager.adapter = adapter
        // Link the TabLayout with the ViewPager
        tabLayout.setupWithViewPager(viewPager)

    }
}