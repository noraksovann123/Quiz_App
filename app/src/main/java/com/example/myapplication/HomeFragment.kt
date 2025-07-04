package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeFragment : Fragment() {
    private val TAG = "HomeFragment"
    
    // Firebase database
    private lateinit var database: DatabaseReference
    
    // RecyclerViews
    private lateinit var discoverRecyclerView: RecyclerView
    private lateinit var authorsRecyclerView: RecyclerView
    private lateinit var trendingRecyclerView: RecyclerView
    private lateinit var topPicksRecyclerView: RecyclerView
    
    // Empty state views
    private lateinit var emptyDiscover: TextView
    private lateinit var emptyTrending: TextView
    private lateinit var emptyTopPicks: TextView
    
    // Section containers
    private lateinit var authorsSection: ViewGroup
    
    // Adapters
    private lateinit var discoverAdapter: QuizAdapter
    private lateinit var trendingAdapter: QuizAdapter
    private lateinit var topPicksAdapter: QuizAdapter
    private lateinit var authorsAdapter: AuthorsAdapter
    
    // Data
    private val discoverQuizzes = mutableListOf<Quiz>()
    private val trendingQuizzes = mutableListOf<Quiz>()
    private val topPickQuizzes = mutableListOf<Quiz>()
    private val authors = mutableListOf<Author>()
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference
        
        // Initialize views
        initViews(view)
        
        // Set up RecyclerViews
        setupRecyclerViews()
        
        // Set up click listeners
        setupClickListeners(view)
        
        // Load data from Firebase
        loadData()
        
        return view
    }
    
    private fun initViews(view: View) {
        // RecyclerViews
        discoverRecyclerView = view.findViewById(R.id.discoverRecyclerView)
        authorsRecyclerView = view.findViewById(R.id.authorsRecyclerView)
        trendingRecyclerView = view.findViewById(R.id.trendingRecyclerView)
        topPicksRecyclerView = view.findViewById(R.id.topPicksRecyclerView)
        
        // Empty states
        emptyDiscover = view.findViewById(R.id.emptyDiscover)
        emptyTrending = view.findViewById(R.id.emptyTrending)
        emptyTopPicks = view.findViewById(R.id.emptyTopPicks)
        
        // Sections
        authorsSection = view.findViewById(R.id.authorsSection)
        
        // FAB
        view.findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            // Check if user is logged in
            if (FirebaseAuth.getInstance().currentUser != null) {
                startActivity(Intent(requireContext(), CreateQuizActivity::class.java))
            } else {
                Toast.makeText(context, "Please log in to create a quiz", Toast.LENGTH_SHORT).show()
                // You can navigate to login/register screen here
            }
        }
    }
    
    private fun setupRecyclerViews() {
        // Discover RecyclerView
        discoverRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        discoverAdapter = QuizAdapter(discoverQuizzes) { quiz ->
            navigateToQuizDetails(quiz)
        }
        discoverRecyclerView.adapter = discoverAdapter
        
        // Authors RecyclerView
        authorsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        authorsAdapter = AuthorsAdapter(authors) { authorId ->
            navigateToAuthorProfile(authorId)
        }
        authorsRecyclerView.adapter = authorsAdapter
        
        // Trending RecyclerView
        trendingRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        trendingAdapter = QuizAdapter(trendingQuizzes) { quiz ->
            navigateToQuizDetails(quiz)
        }
        trendingRecyclerView.adapter = trendingAdapter
        
        // Top Picks RecyclerView
        topPicksRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        topPicksAdapter = QuizAdapter(topPickQuizzes) { quiz ->
            navigateToQuizDetails(quiz)
        }
        topPicksRecyclerView.adapter = topPicksAdapter
    }
    
    private fun setupClickListeners(view: View) {
        // Search button - opens the search activity with categories
        view.findViewById<ImageView>(R.id.searchButton).setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }
        
        // Notifications button
        view.findViewById<ImageView>(R.id.notificationsButton).setOnClickListener {
            // Handle notifications click - future feature
            Toast.makeText(context, "Notifications coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        // View all buttons
        view.findViewById<TextView>(R.id.viewAllDiscover).setOnClickListener {
            navigateToCategory("discover")
        }
        
        view.findViewById<TextView>(R.id.viewAllAuthors).setOnClickListener {
            navigateToAllAuthors()
        }
        
        view.findViewById<TextView>(R.id.viewAllTrending).setOnClickListener {
            navigateToCategory("trending")
        }
        
        view.findViewById<TextView>(R.id.viewAllTopPicks).setOnClickListener {
            navigateToCategory("topPicks")
        }
        
        // Find friends button
        view.findViewById<View>(R.id.findFriendsButton).setOnClickListener {
            Toast.makeText(context, "Find friends feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadData() {
        // Load discover quizzes
        loadQuizzes("featured/discover", discoverQuizzes, discoverAdapter, discoverRecyclerView, emptyDiscover)
        
        // Load trending quizzes
        loadQuizzes("featured/trending", trendingQuizzes, trendingAdapter, trendingRecyclerView, emptyTrending)
        
        // Load top picks
        loadQuizzes("featured/topPicks", topPickQuizzes, topPicksAdapter, topPicksRecyclerView, emptyTopPicks)
        
        // Load authors - only shows authors who have created quizzes
        loadAuthors()
    }
    
    private fun loadQuizzes(
        path: String,
        quizList: MutableList<Quiz>,
        adapter: QuizAdapter,
        recyclerView: RecyclerView,
        emptyView: TextView
    ) {
        database.child(path).limitToFirst(10).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val quizIds = mutableListOf<String>()
                
                for (quizSnapshot in snapshot.children) {
                    quizSnapshot.key?.let { quizIds.add(it) }
                }
                
                if (quizIds.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    return
                }
                
                recyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
                
                // Fetch full quiz details for each ID
                fetchQuizDetails(quizIds, quizList, adapter)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load quizzes: ${error.message}")
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            }
        })
    }
    
    private fun fetchQuizDetails(
        quizIds: List<String>,
        quizList: MutableList<Quiz>,
        adapter: QuizAdapter
    ) {
        quizList.clear()
        var loadedCount = 0
        
        for (quizId in quizIds) {
            database.child("quizzes").child(quizId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    loadedCount++
                    
                    val title = snapshot.child("title").getValue(String::class.java) ?: ""
                    val description = snapshot.child("description").getValue(String::class.java) ?: ""
                    val imageUrl = snapshot.child("imageUrl").getValue(String::class.java) ?: ""
                    val authorId = snapshot.child("authorId").getValue(String::class.java) ?: ""
                    val authorName = snapshot.child("authorName").getValue(String::class.java) ?: ""
                    val authorImageUrl = snapshot.child("authorImageUrl").getValue(String::class.java) ?: ""
                    val questionCount = snapshot.child("questionCount").getValue(Int::class.java) ?: 0
                    val playCount = snapshot.child("playCount").getValue(Int::class.java) ?: 0
                    val favoriteCount = snapshot.child("favoriteCount").getValue(Int::class.java) ?: 0
                    val shareCount = snapshot.child("shareCount").getValue(Int::class.java) ?: 0
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0
                    val category = snapshot.child("category").getValue(String::class.java) ?: ""
                    
                    val quiz = Quiz(
                        quizId, title, description, imageUrl,
                        authorId, authorName, authorImageUrl,
                        questionCount, playCount, favoriteCount, shareCount,
                        false, false, timestamp, category
                    )
                    
                    quizList.add(quiz)
                    
                    // Update adapter when all quizzes are loaded
                    if (loadedCount >= quizIds.size) {
                        // Sort by timestamp (newest first)
                        quizList.sortByDescending { it.timestamp }
                        adapter.notifyDataSetChanged()
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    loadedCount++
                    Log.e(TAG, "Failed to load quiz details: ${error.message}")
                    
                    // Update adapter when all attempts are complete
                    if (loadedCount >= quizIds.size) {
                        adapter.notifyDataSetChanged()
                    }
                }
            })
        }
    }
    
    private fun loadAuthors() {
        database.child("users").orderByChild("quizCount").limitToLast(10)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    authors.clear()
                    
                    for (authorSnapshot in snapshot.children) {
                        val quizCount = authorSnapshot.child("quizCount").getValue(Int::class.java) ?: 0
                        
                        // Only include authors who have created quizzes
                        if (quizCount > 0) {
                            val authorId = authorSnapshot.key ?: continue
                            val name = authorSnapshot.child("name").getValue(String::class.java) ?: "Unknown"
                            val photoUrl = authorSnapshot.child("photoUrl").getValue(String::class.java) ?: ""
                            
                            authors.add(Author(authorId, name, photoUrl, quizCount))
                        }
                    }
                    
                    // Sort by quiz count (most active authors first)
                    authors.sortByDescending { it.quizCount }
                    
                    // Update UI based on whether there are authors to show
                    if (authors.isEmpty()) {
                        authorsSection.visibility = View.GONE
                    } else {
                        authorsSection.visibility = View.VISIBLE
                        authorsAdapter.notifyDataSetChanged()
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load authors: ${error.message}")
                    authorsSection.visibility = View.GONE
                }
            })
    }
    
    private fun navigateToQuizDetails(quiz: Quiz) {
        val intent = Intent(requireContext(), StartQuizActivity::class.java)
        intent.putExtra("quiz_id", quiz.id)
        startActivity(intent)
    }
    
    private fun navigateToAuthorProfile(authorId: String) {
        // This would be implemented in a real app
        Toast.makeText(context, "Author profile coming soon!", Toast.LENGTH_SHORT).show()
    }
    
    private fun navigateToCategory(category: String) {
        // This would be implemented in a real app
        Toast.makeText(context, "View all $category coming soon!", Toast.LENGTH_SHORT).show()
    }
    
    private fun navigateToAllAuthors() {
        // This would be implemented in a real app
        Toast.makeText(context, "View all authors coming soon!", Toast.LENGTH_SHORT).show()
    }
    
    // Data class for authors
    data class Author(
        val id: String,
        val name: String,
        val photoUrl: String,
        val quizCount: Int
    )
}