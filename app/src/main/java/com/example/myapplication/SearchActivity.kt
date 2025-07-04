package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.*

class SearchActivity : AppCompatActivity() {
    private val TAG = "SearchActivity"
    
    // UI components
    private lateinit var backButton: ImageView
    private lateinit var searchInput: EditText
    private lateinit var clearButton: ImageView
    private lateinit var categoriesTitle: TextView
    private lateinit var categoriesRecyclerView: RecyclerView
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var emptyResultsView: View
    
    // Firebase
    private lateinit var database: DatabaseReference
    
    // Adapters
    private lateinit var categoriesAdapter: CategoriesAdapter
    private lateinit var searchResultsAdapter: QuizAdapter
    
    // Data
    private val searchResults = mutableListOf<Quiz>()
    
    // Categories list
    private val categories = listOf(
        Category("History", R.drawable.category_history),
        Category("Science", R.drawable.category_science),
        Category("Math", R.drawable.category_math),
        Category("Geography", R.drawable.category_geography),
        Category("Entertainment", R.drawable.category_entertainment),
        Category("Sports", R.drawable.category_sports),
        Category("Technology", R.drawable.category_technology),
        Category("Literature", R.drawable.category_literature),
        Category("Art", R.drawable.category_art),
        Category("General Knowledge", R.drawable.category_general)
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        
        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference
        
        // Initialize views
        initViews()
        
        // Set up RecyclerViews
        setupRecyclerViews()
        
        // Set up click listeners
        setupClickListeners()
        
        // Set up search functionality
        setupSearch()
    }
    
    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        searchInput = findViewById(R.id.searchInput)
        clearButton = findViewById(R.id.clearButton)
        categoriesTitle = findViewById(R.id.categoriesTitle)
        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView)
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView)
        emptyResultsView = findViewById(R.id.emptyResults)
    }
    
    private fun setupRecyclerViews() {
        // Categories RecyclerView
        categoriesRecyclerView.layoutManager = GridLayoutManager(this, 2)
        categoriesAdapter = CategoriesAdapter(categories) { category ->
            navigateToCategory(category)
        }
        categoriesRecyclerView.adapter = categoriesAdapter
        
        // Search Results RecyclerView
        resultsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchResultsAdapter = QuizAdapter(searchResults) { quiz ->
            navigateToQuizDetails(quiz)
        }
        resultsRecyclerView.adapter = searchResultsAdapter
        
        // Initially hide results list
        resultsRecyclerView.visibility = View.GONE
        emptyResultsView.visibility = View.GONE
    }
    
    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }
        
        clearButton.setOnClickListener {
            searchInput.text.clear()
        }
    }
    
    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                
                if (query.isEmpty()) {
                    // Show categories, hide results
                    categoriesTitle.visibility = View.VISIBLE
                    categoriesRecyclerView.visibility = View.VISIBLE
                    resultsRecyclerView.visibility = View.GONE
                    emptyResultsView.visibility = View.GONE
                    clearButton.visibility = View.INVISIBLE
                } else {
                    // Show clear button
                    clearButton.visibility = View.VISIBLE
                    
                    // Hide categories, show results
                    categoriesTitle.visibility = View.GONE
                    categoriesRecyclerView.visibility = View.GONE
                    performSearch(query)
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun performSearch(query: String) {
        // Search Firebase for quizzes matching the query
        val searchQuery = query.lowercase()
        
        database.child("quizzes")
            .orderByChild("title")
            .startAt(searchQuery)
            .endAt(searchQuery + "\uf8ff") // \uf8ff is a high code point to match all strings that start with the query
            .limitToFirst(20)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    searchResults.clear()
                    
                    for (quizSnapshot in snapshot.children) {
                        val quizId = quizSnapshot.key ?: continue
                        val title = quizSnapshot.child("title").getValue(String::class.java) ?: ""
                        val description = quizSnapshot.child("description").getValue(String::class.java) ?: ""
                        val imageUrl = quizSnapshot.child("imageUrl").getValue(String::class.java) ?: ""
                        val authorId = quizSnapshot.child("authorId").getValue(String::class.java) ?: ""
                        val authorName = quizSnapshot.child("authorName").getValue(String::class.java) ?: ""
                        val authorImageUrl = quizSnapshot.child("authorImageUrl").getValue(String::class.java) ?: ""
                        val questionCount = quizSnapshot.child("questionCount").getValue(Int::class.java) ?: 0
                        val playCount = quizSnapshot.child("playCount").getValue(Int::class.java) ?: 0
                        val favoriteCount = quizSnapshot.child("favoriteCount").getValue(Int::class.java) ?: 0
                        val shareCount = quizSnapshot.child("shareCount").getValue(Int::class.java) ?: 0
                        
                        // Add to search results
                        searchResults.add(
                            Quiz(
                                id = quizId,
                                title = title,
                                description = description,
                                imageUrl = imageUrl,
                                authorId = authorId,
                                authorName = authorName,
                                authorImageUrl = authorImageUrl,
                                questionCount = questionCount,
                                playCount = playCount,
                                favoriteCount = favoriteCount,
                                shareCount = shareCount
                            )
                        )
                    }
                    
                    // Update results RecyclerView
                    if (searchResults.isEmpty()) {
                        resultsRecyclerView.visibility = View.GONE
                        emptyResultsView.visibility = View.VISIBLE
                    } else {
                        resultsRecyclerView.visibility = View.VISIBLE
                        emptyResultsView.visibility = View.GONE
                    }
                    searchResultsAdapter.notifyDataSetChanged()
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Database error: ${error.message}")
                }
            })
    }
    
    private fun navigateToCategory(category: Category) {
        val intent = Intent(this, CategoryQuizzesActivity::class.java)
        intent.putExtra("category", category.name)
        startActivity(intent)
    }
    
    private fun navigateToQuizDetails(quiz: Quiz) {
        val intent = Intent(this, StartQuizActivity::class.java)
        intent.putExtra("quiz_id", quiz.id)
        startActivity(intent)
    }
    
    // Category data class
    data class Category(val name: String, val iconRes: Int)
}