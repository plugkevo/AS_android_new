package com.example.africanshipping25

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    // Toolbar and Notification Badge
    private lateinit var notificationBadge: TextView
    private lateinit var notificationIconContainer: View // The FrameLayout for the icon and badge
    private lateinit var btnNotificaations: ImageView // This is your bell icon

    // Bottom Navigation
    private lateinit var bottomNavigation: BottomNavigationView

    // FAB
    private lateinit var fabNewShipment: FloatingActionButton // Declared here

    // More Options Button (replaces logout)
    private lateinit var btnMoreOptions: ImageButton

    // Firebase Auth
    private lateinit var auth: FirebaseAuth

    // Receiver to update badge when count changes
    private val unseenCountReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ViewNotificationsFragment.ACTION_UNSEEN_COUNT_UPDATED) {
                updateNotificationBadge()
            }
        }
    }

    private lateinit var updateChecker: UpdateChecker

    override fun onCreate(savedInstanceState: Bundle?) {

        // Apply saved theme before calling super.onCreate()
        val sharedPreferences = getSharedPreferences("app_preferences", 0)
        val savedTheme = sharedPreferences.getString("theme", "System Default")
        when (savedTheme) {
            "Light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "System Default" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        updateChecker = UpdateChecker(this)

        // Check for updates when app starts
        checkForUpdatesOnStart()


        // Set status bar color
        //window.statusBarColor = ContextCompat.getColor(this, R.color.dark_blue)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize toolbar elements
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Dashboard" // Default title

        // Initialize notification icon and badge
        notificationBadge = findViewById(R.id.tv_notification_badge)
        notificationIconContainer = findViewById(R.id.notification_icon_container)
        btnNotificaations = findViewById(R.id.iv_notifications) // Initialize the bell icon ImageView

        // Initialize other UI components
        bottomNavigation = findViewById(R.id.bottom_navigation)
        fabNewShipment = findViewById(R.id.fab_new_shipment) // Initialize FAB
        btnMoreOptions = findViewById(R.id.btn_more_options) // Initialize more options button

        // Set OnClickListeners
        btnMoreOptions.setOnClickListener { view ->
            showMoreOptionsMenu(view)
        }

        // Changed to navigate to ViewNotificationsFragment directly
        btnNotificaations.setOnClickListener {
            navigateToNotificationsFragment()
        }

        fabNewShipment.setOnClickListener {
            openNewShipmentForm()
        }

        // Set up bottom navigation
        setupBottomNavigation()

        // Load the default fragment (Home) if starting fresh
        if (savedInstanceState == null) {
            // Only set if not launched from a notification intent, otherwise handleNotificationIntent
            // will take precedence and navigate.
            if (intent?.extras?.containsKey("google.message_id") != true && intent?.extras?.containsKey("from") != true) {
                bottomNavigation.selectedItemId = R.id.nav_home // This will trigger the listener and load HomeFragment
            }
        }

        // Handle initial intent (e.g., from tapping a notification)
        handleNotificationIntent(intent)

        // Initial badge update
        updateNotificationBadge()

        // Get FCM token and Installation ID
        getFCMToken()
        getFirebaseInstallationId()

    }
    private fun checkForUpdatesOnStart() {
        // Always check for mandatory updates
        updateChecker.checkForUpdatesOnAppStart(this)
    }

    override fun onResume() {
        super.onResume()
        // Register the BroadcastReceiver when the activity is resumed
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(unseenCountReceiver, IntentFilter(ViewNotificationsFragment.ACTION_UNSEEN_COUNT_UPDATED))

        // Update badge in case unseen count changed while app was in background
        updateNotificationBadge()
    }

    override fun onPause() {
        super.onPause()
        // Unregister the BroadcastReceiver when the activity is paused
        LocalBroadcastManager.getInstance(this).unregisterReceiver(unseenCountReceiver)
    }

    // --- Private Helper Methods ---

    private fun showMoreOptionsMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.inflate(R.menu.menu_toolbar_options)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_profile -> {
                    navigateToProfile()
                    true
                }
                R.id.action_logout -> {
                    showLogoutConfirmation()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun navigateToProfile() {
        // Create and navigate to ProfileFragment
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment !is ProfileFragment) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment())
                .addToBackStack("profile") // Add to back stack to allow going back
                .commit()
            supportActionBar?.title = "Profile" // Update toolbar title
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        auth.signOut() // Sign out the current user

        // After logging out, navigate to your login/splash/onboarding activity
        val intent = Intent(this, login::class.java) // Replace LoginActivity with your actual login activity
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clears activity stack
        startActivity(intent)
        finish() // Finish MainActivity so the user can't navigate back to it
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    // Update toolbar title when selecting from bottom nav
                    supportActionBar?.title = "Dashboard"
                    true
                }
                R.id.nav_shipments -> {
                    loadFragment(ShipmentsFragment())
                    supportActionBar?.title = "Shipments"
                    true
                }
                R.id.nav_loading_list -> {
                    loadFragment(LoadingFragment())
                    supportActionBar?.title = "Loading List"
                    true
                }
                R.id.nav_payment -> {
                    loadFragment(PaymentFragment())
                    supportActionBar?.title = "Payment"
                    true
                }
                else -> false // Should not happen if menu items are correctly mapped
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        // Check if the fragment is already the current fragment to avoid unnecessary replacements
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment?.javaClass != fragment.javaClass) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                // Do not add to back stack for main bottom nav items to avoid deep nesting
                .commit()
        }
    }

    private fun openNewShipmentForm() {
        // Instantiate and show your NewShipmentDialogFragment
        val dialog = NewShipmentDialogFragment()
        dialog.show(supportFragmentManager, "NewShipmentDialog")
    }

    private fun handleNotificationIntent(intent: Intent?) {
        intent?.extras?.let { extras ->
            val fromFCM = extras.containsKey("google.message_id") || extras.containsKey("from")
            if (fromFCM) {
                Log.d(TAG, "Activity launched/resumed from FCM notification.")
                Log.d(TAG, "FCM Custom Data Received:")
                for (key in extras.keySet()) {
                    val value = extras.getString(key)
                    Log.d(TAG, "  Key: $key, Value: $value")
                }

                // If launched from a notification, directly go to the notifications list
                navigateToNotificationsFragment()
                // Ensure the bottom navigation doesn't interfere if it has a notifications item.
                // For now, assuming notification list is accessed only via the toolbar icon.
            }
        }
        // Clear the intent's data after processing to prevent it from being re-processed
        intent?.replaceExtras(Bundle())
    }

    // Public method to allow fragments to request navigation
    fun navigateToNotificationsFragment() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment !is ViewNotificationsFragment) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ViewNotificationsFragment())
                .addToBackStack("notifications_list") // Add to back stack to allow going back
                .commit()
            supportActionBar?.title = "Notifications" // Update toolbar title
        }
    }

    private fun updateNotificationBadge() {
        val unseenCount = ViewNotificationsFragment.getUnseenCount(this)
        if (unseenCount > 0) {
            notificationBadge.text = if (unseenCount > 99) "99+" else unseenCount.toString()
            notificationBadge.visibility = View.VISIBLE
        } else {
            notificationBadge.visibility = View.GONE
        }
        Log.d(TAG, "Updated notification badge to: $unseenCount")
    }

    override fun onBackPressed() {
        // If there are fragments in the back stack (e.g., from navigating to notifications), pop them
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            // After popping, update toolbar title based on the fragment now visible
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            when (currentFragment) {
                is HomeFragment -> supportActionBar?.title = "Dashboard"
                is ShipmentsFragment -> supportActionBar?.title = "Shipments"
                is LoadingFragment -> supportActionBar?.title = "Loading List"
                is PaymentFragment -> supportActionBar?.title = "Payment"
                is ViewNotificationsFragment -> supportActionBar?.title = "Notifications"
                is ProfileFragment -> supportActionBar?.title = "Profile"
                else -> supportActionBar?.title = "African Shipping" // Default or app name
            }
        } else {
            // If no fragments in back stack, let system handle back press (exit app)
            super.onBackPressed()
        }
    }

    // New method to get the FCM token
    private fun getFCMToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result
                Log.d(TAG, "Current FCM Token: $token")
            }
    }

    // New method to get the Firebase Installation ID
    private fun getFirebaseInstallationId() {
        FirebaseInstallations.getInstance().getId()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching Firebase Installation ID failed", task.exception)
                    return@addOnCompleteListener
                }
                val installationId = task.result
                Log.d(TAG, "Firebase Installation ID: $installationId")
            }
    }


}