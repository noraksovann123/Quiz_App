package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity : AppCompatActivity() {
    
    private lateinit var userProfileImage: CircleImageView
    private lateinit var userName: TextView
    private lateinit var searchIcon: ImageView
    private lateinit var bottomNavigation: BottomNavigationView
    
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    
    // Fragment instances
    private val homeFragment = HomeFragment()
    private val libraryFragment = LibraryFragment()
    private val profileFragment = ProfileFragment()
    
    // Keep track of current fragment
    private var activeFragment: Fragment? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        
        // Check if user is logged in
        val isGuest = intent.getBooleanExtra("isGuest", false)
        if (!isGuest && auth.currentUser == null) {
            // Navigate to welcome/login screen
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        // Initialize views
        initializeViews()
        
        // Load user data
        if (!isGuest) {
            loadUserData()
        } else {
            // Show guest UI
            userName.text = "Guest User"
            userProfileImage.setImageResource(R.drawable.default_profile_image)
        }
        
        // Load HomeFragment by default
        if (savedInstanceState == null) {
            loadFragment(homeFragment)
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }
    
    private fun initializeViews() {
        userProfileImage = findViewById(R.id.userProfileImage)
        userName = findViewById(R.id.userName)
        searchIcon = findViewById(R.id.searchIcon)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        
        // Set click listeners
        userProfileImage.setOnClickListener {
            // Navigate to profile
            bottomNavigation.selectedItemId = R.id.nav_profile
        }
        
        searchIcon.setOnClickListener {
            navigateToSearch()
        }
        
        // Setup bottom navigation
        setupBottomNavigation()
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(homeFragment)
                    true
                }
                R.id.nav_library -> {
                    loadFragment(libraryFragment)
                    true
                }
                R.id.nav_create -> {
                    navigateToCreateQuiz()
                    false // Don't select this tab
                }
                R.id.nav_profile -> {
                    loadFragment(profileFragment)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return
        
        // Load user from Firebase
        database.reference.child("users").child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("fullName").getValue(String::class.java) 
                        ?: snapshot.child("name").getValue(String::class.java)
                        ?: currentUser.displayName 
                        ?: "User"
                    
                    val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)
                        ?: currentUser.photoUrl?.toString()
                        ?: ""
                    
                    // Update UI with user data
                    userName.text = name
                    
                    // Load profile image
                    if (photoUrl.isNotEmpty()) {
                        Glide.with(this@MainActivity)
                            .load(photoUrl)
                            .circleCrop()
                            .into(userProfileImage)
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Use data from Firebase Auth as fallback
                    userName.text = currentUser.displayName ?: "User"
                    
                    if (currentUser.photoUrl != null) {
                        Glide.with(this@MainActivity)
                            .load(currentUser.photoUrl)
                            .circleCrop()
                            .into(userProfileImage)
                    }
                }
            })
    }
    
    private fun loadFragment(fragment: Fragment) {
        // Update active fragment
        activeFragment = fragment
        
        // Start transaction
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    
    private fun navigateToSearch() {
        val intent = Intent(this, SearchActivity::class.java)
        startActivity(intent)
    }
    
    private fun navigateToCreateQuiz() {
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            // Prompt to login
            Toast.makeText(this, "Please login to create quizzes", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            return
        }
        
        // Check if user is an author
        database.reference.child("users").child(currentUser.uid).child("role")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val role = snapshot.getValue(String::class.java) ?: "user"
                    
                    if (role == "author" || role == "admin") {
                        // Allow quiz creation
                        val intent = Intent(this@MainActivity, CreateQuizActivity::class.java)
                        startActivity(intent)
                    } else {
                        // Show message that only authors can create quizzes
                        Toast.makeText(this@MainActivity, "Only authors can create quizzes", Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Default to allowing quiz creation on error
                    val intent = Intent(this@MainActivity, CreateQuizActivity::class.java)
                    startActivity(intent)
                }
            })
    }
    
    // This allows fragments to update the selected navigation item
    fun setSelectedNavigationItem(itemId: Int) {
        bottomNavigation.selectedItemId = itemId
    }
}