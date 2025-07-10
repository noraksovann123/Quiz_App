package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class LibraryFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recentRecyclerView: RecyclerView
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var createdRecyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var progressBar: ProgressBar
    
    private lateinit var recentAdapter: QuizAdapters
    private lateinit var favoritesAdapter: QuizAdapters
    private lateinit var createdAdapter: QuizAdapters
    
    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_library, container, false)
        
        // Initialize views
        initViews(view)
        
        // Setup recycler views
        setupRecyclerViews()
        
        // Load data
        loadLibraryData()
        
        return view
    }
    
    private fun initViews(view: View) {
        tabLayout = view.findViewById(R.id.tabLayout)
        recentRecyclerView = view.findViewById(R.id.recentRecyclerView)
        favoritesRecyclerView = view.findViewById(R.id.favoritesRecyclerView)
        createdRecyclerView = view.findViewById(R.id.createdRecyclerView)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        progressBar = view.findViewById(R.id.progressBar)
        
        // Set tab listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                updateVisibleSection(tab.position)
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
    
    private fun setupRecyclerViews() {
        // Recent quizzes
        recentAdapter = QuizAdapters(emptyList()) { quiz ->
            navigateToQuizDetails(quiz)
        }
        recentRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recentRecyclerView.adapter = recentAdapter
        
        // Favorite quizzes
        favoritesAdapter = QuizAdapters(emptyList()) { quiz ->
            navigateToQuizDetails(quiz)
        }
        favoritesRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        favoritesRecyclerView.adapter = favoritesAdapter
        
        // Created quizzes
        createdAdapter = QuizAdapters(emptyList()) { quiz ->
            navigateToQuizDetails(quiz)
        }
        createdRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        createdRecyclerView.adapter = createdAdapter
    }
    
    private fun loadLibraryData() {
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            showEmptyState(getString(R.string.no_items_found, "library items"))
            return
        }
        
        showLoading(true)
        
        // Load recent quizzes
        loadRecentQuizzes(currentUser.uid)
        
        // Load favorite quizzes
        loadFavoriteQuizzes(currentUser.uid)
        
        // Load created quizzes
        loadCreatedQuizzes(currentUser.uid)
    }
    
    private fun loadRecentQuizzes(userId: String) {
        database.child("user_activity").child(userId).child("recent_quizzes")
            .limitToLast(10)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val quizIds = mutableListOf<String>()
                    
                    for (child in snapshot.children) {
                        child.key?.let { quizIds.add(it) }
                    }
                    
                    if (quizIds.isEmpty()) {
                        // No recent quizzes
                        recentRecyclerView.visibility = View.GONE
                        return
                    }
                    
                    fetchQuizzesById(quizIds) { quizzes ->
                        recentAdapter.updateData(quizzes)
                        recentRecyclerView.visibility = View.VISIBLE
                        showLoading(false)
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    recentRecyclerView.visibility = View.GONE
                }
            })
    }
    
    private fun loadFavoriteQuizzes(userId: String) {
        database.child("user_activity").child(userId).child("favorites")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val quizIds = mutableListOf<String>()
                    
                    for (child in snapshot.children) {
                        child.key?.let { quizIds.add(it) }
                    }
                    
                    if (quizIds.isEmpty()) {
                        // No favorite quizzes
                        favoritesRecyclerView.visibility = View.GONE
                        return
                    }
                    
                    fetchQuizzesById(quizIds) { quizzes ->
                        favoritesAdapter.updateData(quizzes)
                        favoritesRecyclerView.visibility = View.VISIBLE
                        showLoading(false)
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    favoritesRecyclerView.visibility = View.GONE
                }
            })
    }
    
    private fun loadCreatedQuizzes(userId: String) {
        database.child("user_activity").child(userId).child("created")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val quizIds = mutableListOf<String>()
                    
                    for (child in snapshot.children) {
                        child.key?.let { quizIds.add(it) }
                    }
                    
                    if (quizIds.isEmpty()) {
                        // No created quizzes
                        createdRecyclerView.visibility = View.GONE
                        return
                    }
                    
                    fetchQuizzesById(quizIds) { quizzes ->
                        createdAdapter.updateData(quizzes)
                        createdRecyclerView.visibility = View.VISIBLE
                        showLoading(false)
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    createdRecyclerView.visibility = View.GONE
                }
            })
    }
    
    private fun fetchQuizzesById(quizIds: List<String>, callback: (List<Quiz>) -> Unit) {
        val quizzes = mutableListOf<Quiz>()
        var loadedCount = 0
        
        for (quizId in quizIds) {
            database.child("quizzes").child(quizId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val id = snapshot.key ?: ""
                        val title = snapshot.child("title").getValue(String::class.java) ?: ""
                        val description = snapshot.child("description").getValue(String::class.java) ?: ""
                        val imageUrl = snapshot.child("imageUrl").getValue(String::class.java) ?: ""
                        val authorId = snapshot.child("authorId").getValue(String::class.java) ?: ""
                        val authorName = snapshot.child("authorName").getValue(String::class.java) ?: ""
                        val authorImageUrl = snapshot.child("authorImageUrl").getValue(String::class.java) ?: ""
                        val questionCount = snapshot.child("questionCount").getValue(Long::class.java)?.toInt() ?: 0
                        val playCount = snapshot.child("playCount").getValue(Long::class.java)?.toInt() ?: 0
                        val favoriteCount = snapshot.child("favoriteCount").getValue(Long::class.java)?.toInt() ?: 0
                        val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                        val category = snapshot.child("collection").getValue(String::class.java) ?: ""
                        
                        val quiz = Quiz(
                            id = id,
                            title = title, 
                            description = description, 
                            imageUrl = imageUrl,
                            authorId = authorId, 
                            authorName = authorName, 
                            authorImageUrl = authorImageUrl,
                            timestamp = timestamp,
                            category = category,
                            questionCount = questionCount,
                            playCount = playCount,
                            favoriteCount = favoriteCount
                        )
                        
                        quizzes.add(quiz)
                        loadedCount++
                        
                        if (loadedCount >= quizIds.size) {
                            // Sort by timestamp (newest first)
                            quizzes.sortByDescending { it.timestamp }
                            callback(quizzes)
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        loadedCount++
                        if (loadedCount >= quizIds.size) {
                            quizzes.sortByDescending { it.timestamp }
                            callback(quizzes)
                        }
                    }
                })
        }
    }
    
    private fun updateVisibleSection(tabPosition: Int) {
        when (tabPosition) {
            0 -> { // Recent
                recentRecyclerView.visibility = View.VISIBLE
                favoritesRecyclerView.visibility = View.GONE
                createdRecyclerView.visibility = View.GONE
            }
            1 -> { // Favorites
                recentRecyclerView.visibility = View.GONE
                favoritesRecyclerView.visibility = View.VISIBLE
                createdRecyclerView.visibility = View.GONE
            }
            2 -> { // Created
                recentRecyclerView.visibility = View.GONE
                favoritesRecyclerView.visibility = View.GONE
                createdRecyclerView.visibility = View.VISIBLE
            }
        }
        
        // Check if current section is empty
        val isEmpty = when (tabPosition) {
            0 -> recentAdapter.itemCount == 0
            1 -> favoritesAdapter.itemCount == 0
            2 -> createdAdapter.itemCount == 0
            else -> true
        }
        
        // Show empty state if needed
        emptyStateText.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }
    
    private fun navigateToQuizDetails(quiz: Quiz) {
        val intent = Intent(requireContext(), StartQuizActivity::class.java)
        intent.putExtra("quiz_id", quiz.id)
        startActivity(intent)
    }
    
    private fun showEmptyState(message: String = "Your library is empty") {
        emptyStateText.text = message
        emptyStateText.visibility = View.VISIBLE
        recentRecyclerView.visibility = View.GONE
        favoritesRecyclerView.visibility = View.GONE
        createdRecyclerView.visibility = View.GONE
        showLoading(false)
    }
    
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this fragment
        loadLibraryData()
    }
}