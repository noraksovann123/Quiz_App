package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity : AppCompatActivity() {
    
    private lateinit var userProfileImage: CircleImageView
    private lateinit var userName: TextView
    private lateinit var searchIcon: ImageView
    private lateinit var discoverRecyclerView: RecyclerView
    private lateinit var trendingRecyclerView: RecyclerView
    private lateinit var topPicksRecyclerView: RecyclerView
    private lateinit var bottomNavigation: BottomNavigationView
    
    private lateinit var database: DatabaseReference
    private lateinit var loginFirebase: LoginFirebase
    
    private lateinit var discoverAdapter: QuizAdapter
    private lateinit var trendingAdapter: QuizAdapter
    private lateinit var topPicksAdapter: QuizAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference
        loginFirebase = LoginFirebase()
        
        // Check if user is logged in
        val isGuest = intent.getBooleanExtra("isGuest", false)
        if (!isGuest && FirebaseAuth.getInstance().currentUser == null) {
            // Navigate to welcome/login screen
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        // Initialize views
        initializeViews()
        
        // Setup RecyclerViews
        setupRecyclerViews()
        
        // Load user data
        if (!isGuest) {
            loadUserData()
        } else {
            // Show guest UI
            userName.text = "Guest User"
            userProfileImage.setImageResource(R.drawable.default_profile_image)
        }
        
        // Load featured quizzes
        loadDiscoverQuizzes()
        loadTrendingQuizzes()
        loadTopPicksQuizzes()
    }
    
    private fun initializeViews() {
        userProfileImage = findViewById(R.id.userProfileImage)
        userName = findViewById(R.id.userName)
        searchIcon = findViewById(R.id.searchIcon)
        discoverRecyclerView = findViewById(R.id.discoverRecyclerView)
        trendingRecyclerView = findViewById(R.id.trendingRecyclerView)
        topPicksRecyclerView = findViewById(R.id.topPicksRecyclerView)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        
        // Set click listeners
        userProfileImage.setOnClickListener {
            navigateToProfile()
        }
        
        searchIcon.setOnClickListener {
            navigateToSearch()
        }
        
        // Setup bottom navigation
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_library -> {
                    navigateToLibrary()
                    false
                }
                R.id.navigation_create -> {
                    navigateToCreateQuiz()
                    false
                }
                R.id.navigation_profile -> {
                    navigateToProfile()
                    false
                }
                else -> false
            }
        }
    }
    
    private fun setupRecyclerViews() {
        // Discover RecyclerView
        discoverAdapter = QuizAdapter(mutableListOf(), object : QuizAdapter.OnQuizClickListener {
            override fun onQuizClick(quiz: Quiz) {
                openQuizDetails(quiz)
            }
            
            override fun onFavoriteClick(quiz: Quiz, position: Int) {
                toggleFavorite(quiz, position)
            }
        })
        discoverRecyclerView.layoutManager = GridLayoutManager(this, 2)
        discoverRecyclerView.adapter = discoverAdapter
        
        // Trending RecyclerView
        trendingAdapter = QuizAdapter(mutableListOf(), object : QuizAdapter.OnQuizClickListener {
            override fun onQuizClick(quiz: Quiz) {
                openQuizDetails(quiz)
            }
            
            override fun onFavoriteClick(quiz: Quiz, position: Int) {
                toggleFavorite(quiz, position)
            }
        })
        trendingRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        trendingRecyclerView.adapter = trendingAdapter
        
        // Top Picks RecyclerView
        topPicksAdapter = QuizAdapter(mutableListOf(), object : QuizAdapter.OnQuizClickListener {
            override fun onQuizClick(quiz: Quiz) {
                openQuizDetails(quiz)
            }
            
            override fun onFavoriteClick(quiz: Quiz, position: Int) {
                toggleFavorite(quiz, position)
            }
        })
        topPicksRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        topPicksRecyclerView.adapter = topPicksAdapter
    }
    
    private fun loadUserData() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        
        // Load user from Firebase
        database.child("users").child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    
                    if (user != null) {
                        // Update UI with user data
                        userName.text = user.name
                        
                        // Load profile image
                        if (user.profileImage.isNotEmpty()) {
                            Glide.with(this@MainActivity)
                                .load(user.profileImage)
                                .circleCrop()
                                .into(userProfileImage)
                        }
                    } else {
                        // Use data from Firebase Auth if user not found in database
                        userName.text = currentUser.displayName
                        
                        if (currentUser.photoUrl != null) {
                            Glide.with(this@MainActivity)
                                .load(currentUser.photoUrl)
                                .circleCrop()
                                .into(userProfileImage)
                        }
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Use data from Firebase Auth as fallback
                    userName.text = currentUser.displayName
                    
                    if (currentUser.photoUrl != null) {
                        Glide.with(this@MainActivity)
                            .load(currentUser.photoUrl)
                            .circleCrop()
                            .into(userProfileImage)
                    }
                }
            })
    }
    
    private fun loadDiscoverQuizzes() {
        database.child("featured").child("discover")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val quizIds = snapshot.children.mapNotNull { it.key }
                    fetchQuizzes(quizIds) { quizzes ->
                        discoverAdapter.updateQuizzes(quizzes)
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
    
    private fun loadTrendingQuizzes() {
        database.child("featured").child("trending")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val quizIds = snapshot.children.mapNotNull { it.key }
                    fetchQuizzes(quizIds) { quizzes ->
                        trendingAdapter.updateQuizzes(quizzes)
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
    
    private fun loadTopPicksQuizzes() {
        database.child("featured").child("topPicks")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val quizIds = snapshot.children.mapNotNull { it.key }
                    fetchQuizzes(quizIds) { quizzes ->
                        topPicksAdapter.updateQuizzes(quizzes)
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
    
    private fun fetchQuizzes(quizIds: List<String>, callback: (List<Quiz>) -> Unit) {
        if (quizIds.isEmpty()) {
            callback(emptyList())
            return
        }
        
        val quizzes = mutableListOf<Quiz>()
        var fetchedCount = 0
        
        for (quizId in quizIds) {
            database.child("quizzes").child(quizId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val quiz = snapshot.getValue(Quiz::class.java)
                        if (quiz != null) {
                            quiz.id = snapshot.key ?: ""
                            
                            // Check if it's favorited by current user
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            if (currentUser != null) {
                                database.child("users").child(currentUser.uid).child("favoriteQuizzes").child(quiz.id)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(favSnapshot: DataSnapshot) {
                                            quiz.isFavorite = favSnapshot.exists()
                                            quizzes.add(quiz)
                                            
                                            fetchedCount++
                                            if (fetchedCount == quizIds.size) {
                                                callback(quizzes)
                                            }
                                        }
                                        
                                        override fun onCancelled(error: DatabaseError) {
                                            fetchedCount++
                                            quizzes.add(quiz)
                                            if (fetchedCount == quizIds.size) {
                                                callback(quizzes)
                                            }
                                        }
                                    })
                            } else {
                                quizzes.add(quiz)
                                fetchedCount++
                                if (fetchedCount == quizIds.size) {
                                    callback(quizzes)
                                }
                            }
                        } else {
                            fetchedCount++
                            if (fetchedCount == quizIds.size) {
                                callback(quizzes)
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        fetchedCount++
                        if (fetchedCount == quizIds.size) {
                            callback(quizzes)
                        }
                    }
                })
        }
    }
    
    private fun openQuizDetails(quiz: Quiz) {
        val intent = Intent(this, QuizDetailsActivity::class.java)
        intent.putExtra("quiz_id", quiz.id)
        startActivity(intent)
    }
    
    private fun toggleFavorite(quiz: Quiz, position: Int) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            // Prompt to login
            Toast.makeText(this, "Please login to favorite quizzes", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userId = currentUser.uid
        val quizRef = database.child("users").child(userId).child("favoriteQuizzes").child(quiz.id)
        
        quizRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isFavorite = snapshot.exists()
                
                if (isFavorite) {
                    // Remove from favorites
                    quizRef.removeValue()
                    
                    // Update favorite count in quiz
                    database.child("quizzes").child(quiz.id).child("favoriteCount")
                        .runTransaction(object : Transaction.Handler {
                            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                                val currentValue = mutableData.getValue(Int::class.java) ?: 0
                                mutableData.value = maxOf(0, currentValue - 1)
                                return Transaction.success(mutableData)
                            }
                            
                            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                                // Not needed
                            }
                        })
                } else {
                    // Add to favorites
                    quizRef.setValue(true)
                    
                    // Update favorite count in quiz
                    database.child("quizzes").child(quiz.id).child("favoriteCount")
                        .runTransaction(object : Transaction.Handler {
                            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                                val currentValue = mutableData.getValue(Int::class.java) ?: 0
                                mutableData.value = currentValue + 1
                                return Transaction.success(mutableData)
                            }
                            
                            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                                // Not needed
                            }
                        })
                }
                
                // Update quiz state in adapters
                quiz.isFavorite = !isFavorite
                
                // Update relevant adapters
                updateQuizInAdapters(quiz)
            }
            
            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }
    
    private fun updateQuizInAdapters(quiz: Quiz) {
        // Update the quiz in all adapters that might contain it
        discoverAdapter.updateQuiz(quiz)
        trendingAdapter.updateQuiz(quiz)
        topPicksAdapter.updateQuiz(quiz)
    }
    
    // Navigation methods
    private fun navigateToProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            // Prompt to login
            Toast.makeText(this, "Please login to view profile", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            return
        }
        
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
    }
    
    private fun navigateToSearch() {
        val intent = Intent(this, SearchActivity::class.java)
        startActivity(intent)
    }
    
    private fun navigateToCreateQuiz() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            // Prompt to login
            Toast.makeText(this, "Please login to create quizzes", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            return
        }
        
        val intent = Intent(this, CreateQuizActivity::class.java)
        startActivity(intent)
    }
    
    private fun navigateToLibrary() {
        val intent = Intent(this, LibraryActivity::class.java)
        startActivity(intent)
    }
    
    // User data class
    data class User(
        var id: String = "",
        val name: String = "",
        val email: String = "",
        val profileImage: String = "",
        val bio: String = "",
        val followerCount: Int = 0,
        val followingCount: Int = 0,
        val quizCount: Int = 0
    )
}