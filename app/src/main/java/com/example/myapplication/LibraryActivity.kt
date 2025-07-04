package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LibraryActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var categorySpinner: Spinner
    private lateinit var searchIcon: ImageView
    private lateinit var filterIcon: ImageView
    private lateinit var sortIcon: ImageView
    private lateinit var randomizeCheckbox: TextView  // We'll use TextView with clickable styling
    private lateinit var recyclerView: RecyclerView
    private lateinit var noQuizzesText: TextView
    private lateinit var progressBar: ProgressBar
    
    private lateinit var quizAdapter: QuizAdapter
    private lateinit var database: DatabaseReference
    
    private var currentTab = 0  // 0: All, 1: Favorites, 2: History
    private var currentCategory = "All"
    private var isRandomized = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)
        
        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference
        
        // Initialize views
        initializeViews()
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Setup category spinner
        setupCategorySpinner()
        
        // Load initial data
        loadQuizzes()
    }
    
    private fun initializeViews() {
        tabLayout = findViewById(R.id.tabLayout)
        categorySpinner = findViewById(R.id.categorySpinner)
        searchIcon = findViewById(R.id.searchIcon)
        filterIcon = findViewById(R.id.filterIcon)
        sortIcon = findViewById(R.id.sortIcon)
        randomizeCheckbox = findViewById(R.id.randomizeCheckbox)
        recyclerView = findViewById(R.id.recyclerView)
        noQuizzesText = findViewById(R.id.noQuizzesText)
        progressBar = findViewById(R.id.progressBar)
        
        // Back button
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }
        
        // Set click listeners
        searchIcon.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }
        
        filterIcon.setOnClickListener {
            showFilterDialog()
        }
        
        sortIcon.setOnClickListener {
            showSortDialog()
        }
        
        randomizeCheckbox.setOnClickListener {
            toggleRandomize()
        }
        
        // Setup tab layout
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                loadQuizzes()
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
    
    private fun setupRecyclerView() {
        quizAdapter = QuizAdapter(mutableListOf(), object : QuizAdapter.OnQuizClickListener {
            override fun onQuizClick(quiz: Quiz) {
                openQuizDetails(quiz, isRandomized)
            }
            
            override fun onFavoriteClick(quiz: Quiz, position: Int) {
                toggleFavorite(quiz, position)
            }
        })
        
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = quizAdapter
    }
    
    private fun setupCategorySpinner() {
        // Get categories from resources
        val categories = mutableListOf("All") + resources.getStringArray(R.array.categories).toList()
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        categorySpinner.adapter = adapter
        
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentCategory = parent.getItemAtPosition(position).toString()
                loadQuizzes()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    
    private fun loadQuizzes() {
        progressBar.visibility = View.VISIBLE
        noQuizzesText.visibility = View.GONE
        
        when (currentTab) {
            0 -> loadAllQuizzes()
            1 -> loadFavoriteQuizzes()
            2 -> loadHistoryQuizzes()
        }
    }
    
    private fun loadAllQuizzes() {
        if (currentCategory == "All") {
            // Load all quizzes
            database.child("quizzes")
                .limitToFirst(100)  // Limit to avoid too much data
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        handleQuizzesSnapshot(snapshot)
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        showError("Failed to load quizzes")
                    }
                })
        } else {
            // Load quizzes by category
            database.child("categories").child(currentCategory)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val quizIds = snapshot.children.mapNotNull { it.key }
                        if (quizIds.isEmpty()) {
                            showNoQuizzes()
                            return
                        }
                        
                        fetchQuizzesByIds(quizIds)
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        showError("Failed to load category quizzes")
                    }
                })
        }
    }
    
    private fun loadFavoriteQuizzes() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            showNoQuizzes("Please login to view favorites")
            return
        }
        
        database.child("users").child(currentUser.uid).child("favoriteQuizzes")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val quizIds = snapshot.children.mapNotNull { it.key }
                    if (quizIds.isEmpty()) {
                        showNoQuizzes("No favorite quizzes yet")
                        return
                    }
                    
                    fetchQuizzesByIds(quizIds)
                }
                
                override fun onCancelled(error: DatabaseError) {
                    showError("Failed to load favorite quizzes")
                }
            })
    }
    
    private fun loadHistoryQuizzes() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            showNoQuizzes("Please login to view history")
            return
        }
        
        database.child("quiz_results").child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val quizIds = snapshot.children.mapNotNull { it.key }
                    if (quizIds.isEmpty()) {
                        showNoQuizzes("No quiz history yet")
                        return
                    }
                    
                    fetchQuizzesByIds(quizIds)
                }
                
                override fun onCancelled(error: DatabaseError) {
                    showError("Failed to load quiz history")
                }
            })
    }
    
    private fun fetchQuizzesByIds(quizIds: List<String>) {
        if (quizIds.isEmpty()) {
            showNoQuizzes()
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
                            
                            // Filter by category if needed
                            if (currentCategory == "All" || quiz.collection == currentCategory) {
                                // Check if it's favorited by current user
                                checkFavoriteStatus(quiz) {
                                    quizzes.add(quiz)
                                    
                                    fetchedCount++
                                    if (fetchedCount == quizIds.size) {
                                        displayQuizzes(quizzes)
                                    }
                                }
                            } else {
                                fetchedCount++
                                if (fetchedCount == quizIds.size) {
                                    displayQuizzes(quizzes)
                                }
                            }
                        } else {
                            fetchedCount++
                            if (fetchedCount == quizIds.size) {
                                displayQuizzes(quizzes)
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        fetchedCount++
                        if (fetchedCount == quizIds.size) {
                            displayQuizzes(quizzes)
                        }
                    }
                })
        }
    }
    
    private fun handleQuizzesSnapshot(snapshot: DataSnapshot) {
        val quizzes = mutableListOf<Quiz>()
        
        for (quizSnapshot in snapshot.children) {
            val quiz = quizSnapshot.getValue(Quiz::class.java)
            if (quiz != null) {
                quiz.id = quizSnapshot.key ?: ""
                
                // Filter by category if needed
                if (currentCategory == "All" || quiz.collection == currentCategory) {
                    quizzes.add(quiz)
                }
            }
        }
        
        // Check favorite status for each quiz
        if (quizzes.isEmpty()) {
            showNoQuizzes()
            return
        }
        
        var checkedCount = 0
        for (quiz in quizzes) {
            checkFavoriteStatus(quiz) {
                checkedCount++
                if (checkedCount == quizzes.size) {
                    displayQuizzes(quizzes)
                }
            }
        }
    }
    
    private fun checkFavoriteStatus(quiz: Quiz, callback: () -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            quiz.isFavorite = false
            callback()
            return
        }
        
        database.child("users").child(currentUser.uid).child("favoriteQuizzes").child(quiz.id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    quiz.isFavorite = snapshot.exists()
                    callback()
                }
                
                override fun onCancelled(error: DatabaseError) {
                    quiz.isFavorite = false
                    callback()
                }
            })
    }
    
    private fun displayQuizzes(quizzes: List<Quiz>) {
        progressBar.visibility = View.GONE
        
        if (quizzes.isEmpty()) {
            showNoQuizzes()
            return
        }
        
        noQuizzesText.visibility = View.GONE
        quizAdapter.updateQuizzes(quizzes)
    }
    
    private fun showNoQuizzes(message: String = "No quizzes found") {
        progressBar.visibility = View.GONE
        noQuizzesText.visibility = View.VISIBLE
        noQuizzesText.text = message
        quizAdapter.updateQuizzes(emptyList())
    }
    
    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        quizAdapter.updateQuizzes(emptyList())
    }
    
    private fun openQuizDetails(quiz: Quiz, randomize: Boolean) {
        val intent = Intent(this, QuizDetailsActivity::class.java)
        intent.putExtra("quiz_id", quiz.id)
        intent.putExtra("randomize", randomize)  // Pass randomization flag
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
                
                // Update quiz state
                quiz.isFavorite = !isFavorite
                quizAdapter.notifyItemChanged(position)
                
                // If we're in favorites tab and removing from favorites, reload
                if (currentTab == 1 && isFavorite) {
                    loadQuizzes()
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }
    
    private fun toggleRandomize() {
        isRandomized = !isRandomized
        
        // Update visual indicator
        randomizeCheckbox.text = if (isRandomized) "✓ Randomize" else "□ Randomize"
        Toast.makeText(
            this, 
            if (isRandomized) "Questions will be randomized" else "Questions will be in order", 
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun showFilterDialog() {
        val options = arrayOf("All", "Easy", "Medium", "Hard")
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Filter by Difficulty")
            .setItems(options) { _, which ->
                val selectedDifficulty = options[which]
                Toast.makeText(this, "Selected: $selectedDifficulty", Toast.LENGTH_SHORT).show()
                // Implement difficulty filtering here (not in JSON schema yet)
            }
            .show()
    }
    
    private fun showSortDialog() {
        val options = arrayOf("Newest First", "Oldest First", "Most Popular", "Highest Rated")
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Sort By")
            .setItems(options) { _, which ->
                val selectedSort = options[which]
                Toast.makeText(this, "Sorting by: $selectedSort", Toast.LENGTH_SHORT).show()
                
                // Sort the current list based on selection
                val currentQuizzes = quizAdapter.getQuizzes().toMutableList()
                
                when (which) {
                    0 -> currentQuizzes.sortByDescending { it.timestamp }
                    1 -> currentQuizzes.sortBy { it.timestamp }
                    2 -> currentQuizzes.sortByDescending { it.playCount }
                    3 -> currentQuizzes.sortByDescending { it.favoriteCount }
                }
                
                quizAdapter.updateQuizzes(currentQuizzes)
            }
            .show()
    }
}