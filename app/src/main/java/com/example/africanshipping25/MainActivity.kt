package com.example.africanshipping25

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
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

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val TAG = "MainActivity"

    // Toolbar and Notification Badge
    private lateinit var notificationBadge: TextView
    private lateinit var notificationIconContainer: View
    private lateinit var btnNotificaations: ImageView

    // Bottom Navigation
    private lateinit var bottomNavigation: BottomNavigationView

    // FAB
    private lateinit var fabNewShipment: FloatingActionButton

    // More Options Button
    private lateinit var btnMoreOptions: ImageButton

    // Firebase Auth
    private lateinit var auth: FirebaseAuth

    // Update Checker
    private lateinit var updateChecker: UpdateChecker

    // Fragment references for language communication
    private var homeFragment: HomeFragment? = null
    private var shipmentsFragment: ShipmentsFragment? = null
    private var loadingFragment: LoadingFragment? = null
    private var paymentFragment: PaymentFragment? = null
    private var profileFragment: ProfileFragment? = null

    // SharedPreferences for language changes
    private lateinit var sharedPreferences: SharedPreferences

    // Receiver to update badge when count changes
    private val unseenCountReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ViewNotificationsFragment.ACTION_UNSEEN_COUNT_UPDATED) {
                updateNotificationBadge()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize SharedPreferences first
        sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        // Apply saved theme before calling super.onCreate()
        val savedTheme = sharedPreferences.getString("theme", "System Default")
        when (savedTheme) {
            "Light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "System Default" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Register SharedPreferences listener for language changes
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        updateChecker = UpdateChecker(this)

        // Check for updates when app starts
        checkForUpdatesOnStart()

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize UI components
        initializeUIComponents()

        // Set up click listeners
        setupClickListeners()

        // Set up bottom navigation
        setupBottomNavigation()

        // Load the default fragment (Home) if starting fresh
        if (savedInstanceState == null) {
            if (intent?.extras?.containsKey("google.message_id") != true && intent?.extras?.containsKey("from") != true) {
                bottomNavigation.selectedItemId = R.id.nav_home
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

    private fun initializeUIComponents() {
        // Initialize toolbar elements
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Dashboard"

        // Initialize notification icon and badge
        notificationBadge = findViewById(R.id.tv_notification_badge)
        notificationIconContainer = findViewById(R.id.notification_icon_container)
        btnNotificaations = findViewById(R.id.iv_notifications)

        // Initialize other UI components
        bottomNavigation = findViewById(R.id.bottom_navigation)
        fabNewShipment = findViewById(R.id.fab_new_shipment)
        btnMoreOptions = findViewById(R.id.btn_more_options)
    }

    private fun setupClickListeners() {
        btnMoreOptions.setOnClickListener { view ->
            showMoreOptionsMenu(view)
        }

        btnNotificaations.setOnClickListener {
            navigateToNotificationsFragment()
        }

        fabNewShipment.setOnClickListener {
            openNewShipmentForm()
        }
    }

    private fun checkForUpdatesOnStart() {
        updateChecker.checkForUpdatesOnAppStart(this)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(unseenCountReceiver, IntentFilter(ViewNotificationsFragment.ACTION_UNSEEN_COUNT_UPDATED))
        updateNotificationBadge()
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(unseenCountReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister SharedPreferences listener to prevent memory leaks
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    // SharedPreferences change listener for language updates
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "selected_language" -> {
                Log.d(TAG, "Language preference changed, notifying fragments")
                notifyFragmentsOfLanguageChange()
            }
            "theme" -> {
                // Handle theme changes if needed
                Log.d(TAG, "Theme preference changed")
            }
        }
    }

    private fun notifyFragmentsOfLanguageChange() {
        // Notify all active fragments about language change
        homeFragment?.refreshTranslations()
        //shipmentsFragment?.refreshTranslations()
        //loadingFragment?.refreshTranslations()
        //paymentFragment?.refreshTranslations()
        //profileFragment?.refreshTranslations()

        Log.d(TAG, "All fragments notified of language change")
    }

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
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment !is ProfileFragment) {
            if (profileFragment == null) {
                profileFragment = ProfileFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, profileFragment!!)
                .addToBackStack("profile")
                .commit()
            supportActionBar?.title = "Profile"
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
        auth.signOut()
        val intent = Intent(this, login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (homeFragment == null) {
                        homeFragment = HomeFragment()
                    }
                    loadFragment(homeFragment!!)
                    supportActionBar?.title = "Dashboard"
                    true
                }
                R.id.nav_shipments -> {
                    if (shipmentsFragment == null) {
                        shipmentsFragment = ShipmentsFragment()
                    }
                    loadFragment(shipmentsFragment!!)
                    supportActionBar?.title = "Shipments"
                    true
                }
                R.id.nav_loading_list -> {
                    if (loadingFragment == null) {
                        loadingFragment = LoadingFragment()
                    }
                    loadFragment(loadingFragment!!)
                    supportActionBar?.title = "Loading List"
                    true
                }
                R.id.nav_payment -> {
                    if (paymentFragment == null) {
                        paymentFragment = PaymentFragment()
                    }
                    loadFragment(paymentFragment!!)
                    supportActionBar?.title = "Payment"
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment?.javaClass != fragment.javaClass) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }

    private fun openNewShipmentForm() {
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
                navigateToNotificationsFragment()
            }
        }
        intent?.replaceExtras(Bundle())
    }

    fun navigateToNotificationsFragment() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment !is ViewNotificationsFragment) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ViewNotificationsFragment())
                .addToBackStack("notifications_list")
                .commit()
            supportActionBar?.title = "Notifications"
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
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            when (currentFragment) {
                is HomeFragment -> supportActionBar?.title = "Dashboard"
                is ShipmentsFragment -> supportActionBar?.title = "Shipments"
                is LoadingFragment -> supportActionBar?.title = "Loading List"
                is PaymentFragment -> supportActionBar?.title = "Payment"
                is ViewNotificationsFragment -> supportActionBar?.title = "Notifications"
                is ProfileFragment -> supportActionBar?.title = "Profile"
                else -> supportActionBar?.title = "African Shipping"
            }
        } else {
            super.onBackPressed()
        }
    }

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

    // Public method to get fragment references (useful for testing or external access)
    fun getHomeFragment(): HomeFragment? = homeFragment
    fun getProfileFragment(): ProfileFragment? = profileFragment
}