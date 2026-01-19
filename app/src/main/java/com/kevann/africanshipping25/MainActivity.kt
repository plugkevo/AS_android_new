package com.kevann.africanshipping25

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
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.kevann.africanshipping25.fragments.HomeFragment
import com.kevann.africanshipping25.fragments.PaymentFragment
import com.kevann.africanshipping25.fragments.ProfileFragment
import com.kevann.africanshipping25.fragments.ShipmentsFragment
import com.kevann.africanshipping25.loadinglists.LoadingFragment
import com.kevann.africanshipping25.notifications.ViewNotificationsFragment
import com.kevann.africanshipping25.shipments.NewShipmentDialogFragment
import com.kevann.africanshipping25.translation.GoogleTranslationHelper
import com.kevann.africanshipping25.translation.GoogleTranslationManager

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

    // Fragment references for language communication
    private var homeFragment: HomeFragment? = null
    private var shipmentsFragment: ShipmentsFragment? = null
    private var loadingFragment: LoadingFragment? = null
    private var paymentFragment: PaymentFragment? = null
    private var profileFragment: ProfileFragment? = null

    // SharedPreferences for language changes
    private lateinit var sharedPreferences: SharedPreferences

    // Translation components - exactly like ProfileFragment
    private lateinit var translationManager: GoogleTranslationManager
    private lateinit var translationHelper: GoogleTranslationHelper

    // UI elements that need translation
    private lateinit var toolbarTitle: TextView

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

        // Initialize translation components - exactly like ProfileFragment
        translationManager = GoogleTranslationManager(this)
        translationHelper = GoogleTranslationHelper(translationManager)

        // Register SharedPreferences listener for language changes
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

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

        // Translate UI elements based on current language - exactly like ProfileFragment
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        translateUIElements(currentLanguage)
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

    // Translation method - exactly like ProfileFragment
    private fun translateUIElements(targetLanguage: String) {
        // Translate the current toolbar title
        val currentTitle = supportActionBar?.title?.toString() ?: "Dashboard"
        translateToolbarTitle(currentTitle, targetLanguage)

        // Translate bottom navigation items (this requires updating the menu items)
        translateBottomNavigationItems(targetLanguage)
    }

    private fun translateToolbarTitle(title: String, targetLanguage: String) {
        translationHelper.translateText(title, targetLanguage) { translatedTitle ->
            supportActionBar?.title = translatedTitle
        }
    }

    private fun translateBottomNavigationItems(targetLanguage: String) {
        // Get the menu from bottom navigation
        val menu = bottomNavigation.menu

        // Translate each menu item
        menu.findItem(R.id.nav_home)?.let { item ->
            translationHelper.translateText("Home", targetLanguage) { translatedText ->
                item.title = translatedText
            }
        }

        menu.findItem(R.id.nav_shipments)?.let { item ->
            translationHelper.translateText("Shipments", targetLanguage) { translatedText ->
                item.title = translatedText
            }
        }

        menu.findItem(R.id.nav_loading_list)?.let { item ->
            translationHelper.translateText("Loading List", targetLanguage) { translatedText ->
                item.title = translatedText
            }
        }

        menu.findItem(R.id.nav_payment)?.let { item ->
            translationHelper.translateText("Payment", targetLanguage) { translatedText ->
                item.title = translatedText
            }
        }
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

        // Cleanup translation manager
        if (::translationManager.isInitialized) {
            translationManager.cleanup()
        }
    }

    // SharedPreferences change listener for language updates
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "selected_language" -> {
                Log.d(TAG, "Language preference changed, updating MainActivity and notifying fragments")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    refreshTranslations()
                    notifyFragmentsOfLanguageChange()
                }, 100)
            }
            "theme" -> {
                Log.d(TAG, "Theme preference changed")
            }
        }
    }

    // Public method to refresh translations - exactly like ProfileFragment
    private fun refreshTranslations() {
        Log.d(TAG, "refreshTranslations() called")

        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"

        // Clear translation cache like ProfileFragment
        translationHelper.clearCache()

        // Retranslate UI elements
        translateUIElements(currentLanguage)

        Log.d(TAG, "MainActivity translation refresh completed")
    }

    private fun notifyFragmentsOfLanguageChange() {
        Log.d(TAG, "Notifying fragments of language change...")

        // Get current fragment and notify it specifically
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        when (currentFragment) {
            is HomeFragment -> {
                Log.d(TAG, "Refreshing HomeFragment translations")
                currentFragment.refreshTranslations()
            }
            is ProfileFragment -> {
                Log.d(TAG, "Refreshing ProfileFragment translations")
                currentFragment.refreshProfile()
            }
        }

        // Also notify stored fragment references if they exist
        homeFragment?.let {
            if (it.isAdded && it.view != null) {
                Log.d(TAG, "Refreshing stored HomeFragment reference")
                it.refreshTranslations()
            }
        }

        profileFragment?.let {
            if (it.isAdded && it.view != null) {
                Log.d(TAG, "Refreshing stored ProfileFragment reference")
                it.refreshProfile()
            }
        }

        Log.d(TAG, "All fragments notified of language change")
    }

    private fun showMoreOptionsMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.inflate(R.menu.menu_toolbar_options)

        // Translate menu items before showing
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        translatePopupMenu(popupMenu, currentLanguage)

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

    private fun translatePopupMenu(popupMenu: PopupMenu, targetLanguage: String) {
        val menu = popupMenu.menu

        menu.findItem(R.id.action_profile)?.let { item ->
            translationHelper.translateText("Profile", targetLanguage) { translatedText ->
                item.title = translatedText
            }
        }

        menu.findItem(R.id.action_logout)?.let { item ->
            translationHelper.translateText("Logout", targetLanguage) { translatedText ->
                item.title = translatedText
            }
        }
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

            // Translate and set toolbar title
            val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
            translateToolbarTitle("Profile", currentLanguage)
        }
    }

    private fun showLogoutConfirmation() {
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        val builder = AlertDialog.Builder(this)

        // Translate dialog elements using ProfileFragment pattern
        translationHelper.translateText("Logout", currentLanguage) { translatedTitle ->
            builder.setTitle(translatedTitle)
        }

        translationHelper.translateText("Are you sure you want to logout?", currentLanguage) { translatedMessage ->
            builder.setMessage(translatedMessage)
        }

        translationHelper.translateText("Logout", currentLanguage) { logoutText ->
            translationHelper.translateText("Cancel", currentLanguage) { cancelText ->
                builder.setPositiveButton(logoutText) { _, _ ->
                    performLogout()
                }
                builder.setNegativeButton(cancelText, null)
                builder.show()
            }
        }
    }

    private fun performLogout() {
        auth.signOut()
        val intent = Intent(this, login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        showTranslatedToast("Logged out successfully")
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (homeFragment == null) {
                        homeFragment = HomeFragment()
                    }
                    loadFragment(homeFragment!!)
                    val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
                    translateToolbarTitle("Dashboard", currentLanguage)
                    true
                }
                R.id.nav_shipments -> {
                    if (shipmentsFragment == null) {
                        shipmentsFragment = ShipmentsFragment()
                    }
                    loadFragment(shipmentsFragment!!)
                    val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
                    translateToolbarTitle("Shipments", currentLanguage)
                    true
                }
                R.id.nav_loading_list -> {
                    if (loadingFragment == null) {
                        loadingFragment = LoadingFragment()
                    }
                    loadFragment(loadingFragment!!)
                    val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
                    translateToolbarTitle("Loading List", currentLanguage)
                    true
                }
                R.id.nav_payment -> {
                    if (paymentFragment == null) {
                        paymentFragment = PaymentFragment()
                    }
                    loadFragment(paymentFragment!!)
                    val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
                    translateToolbarTitle("Payment", currentLanguage)
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

            // Translate and set toolbar title
            val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
            translateToolbarTitle("Notifications", currentLanguage)
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
            val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"

            when (currentFragment) {
                is HomeFragment -> translateToolbarTitle("Dashboard", currentLanguage)
                is ShipmentsFragment -> translateToolbarTitle("Shipments", currentLanguage)
                is LoadingFragment -> translateToolbarTitle("Loading List", currentLanguage)
                is PaymentFragment -> translateToolbarTitle("Payment", currentLanguage)
                is ViewNotificationsFragment -> translateToolbarTitle("Notifications", currentLanguage)
                is ProfileFragment -> translateToolbarTitle("Profile", currentLanguage)
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d(TAG, "FCM Token: $token")
        }
    }

    private fun getFirebaseInstallationId() {
        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Firebase Installation ID: ${task.result}")
            } else {
                Log.e(TAG, "Unable to get Firebase Installation ID", task.exception)
            }
        }
    }

    private fun showTranslatedToast(message: String) {
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        translationHelper.translateText(message, currentLanguage) { translatedMessage ->
            Toast.makeText(this, translatedMessage, Toast.LENGTH_SHORT).show()
        }
    }
}