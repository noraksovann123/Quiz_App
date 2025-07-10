package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.example.myapplication.models.Quiz

class CategoryQuizzesActivity : AppCompatActivity() {

    private val tag = "CategoryQuizzesActivity"

    private lateinit var backButton: ImageView
    private lateinit var categoryTitle: TextView
    private lateinit var quizzesRecyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var database: DatabaseReference

    private lateinit var quizAdapter: QuizAdapter
    private val quizzes = mutableListOf<Quiz>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_quizzes)

        // Get category from intent
        val category = intent.getStringExtra("category") ?: "General Knowledge"

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference

        // Initialize views
        backButton = findViewById(R.id.backButton)
        categoryTitle = findViewById(R.id.categoryTitle)
        quizzesRecyclerView = findViewById(R.id.quizzesRecyclerView)
        emptyView = findViewById(R.id.emptyView)

        // Setup UI
        categoryTitle.text = category

        // Setup RecyclerView
        quizzesRecyclerView.layoutManager = LinearLayoutManager(this)
        quizAdapter = QuizAdapter(quizzes) { quiz: Quiz ->
            val intent = Intent(this, StartQuizActivity::class.java)
            intent.putExtra("quiz_id", quiz.id)
            startActivity(intent)
        }
        quizzesRecyclerView.adapter = quizAdapter

        // Setup click listeners
        backButton.setOnClickListener {
            finish()
        }

        // Load quizzes for this category
        loadQuizzesForCategory(category)
    }

    private fun loadQuizzesForCategory(category: String) {
        database.child("quizzes")
            .orderByChild("collection")
            .equalTo(category)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    quizzes.clear()

                    for (quizSnapshot in snapshot.children) {
                        try {
                            val id = quizSnapshot.key ?: ""
                            val title = quizSnapshot.child("title").getValue(String::class.java) ?: ""
                            val description = quizSnapshot.child("description").getValue(String::class.java) ?: ""
                            val imageUrl = quizSnapshot.child("imageUrl").getValue(String::class.java) ?: ""
                            val authorId = quizSnapshot.child("authorId").getValue(String::class.java) ?: ""
                            val authorName = quizSnapshot.child("authorName").getValue(String::class.java) ?: ""
                            val authorImageUrl = quizSnapshot.child("authorImageUrl").getValue(String::class.java) ?: ""
                            val questionCount = quizSnapshot.child("questionCount").getValue(Long::class.java)?.toInt() ?: 0
                            val playCount = quizSnapshot.child("playCount").getValue(Long::class.java)?.toInt() ?: 0
                            val favoriteCount = quizSnapshot.child("favoriteCount").getValue(Long::class.java)?.toInt() ?: 0
                            val timestamp = quizSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                            val categoryValue = quizSnapshot.child("category").getValue(String::class.java) 
                                ?: quizSnapshot.child("collection").getValue(String::class.java) 
                                ?: ""
                            
                            val quiz = Quiz(
                                id = id,
                                title = title, 
                                description = description, 
                                imageUrl = imageUrl,
                                authorId = authorId, 
                                authorName = authorName, 
                                authorImageUrl = authorImageUrl,
                                timestamp = timestamp,
                                category = categoryValue,
                                collection = category,
                                questionCount = questionCount,
                                playCount = playCount,
                                favoriteCount = favoriteCount
                            )
                            quizzes.add(quiz)
                        } catch (e: Exception) {
                            Log.e(tag, "Error parsing quiz: ${e.message}")
                        }
                    }

                    // Update UI
                    if (quizzes.isEmpty()) {
                        emptyView.visibility = View.VISIBLE
                        quizzesRecyclerView.visibility = View.GONE
                    } else {
                        emptyView.visibility = View.GONE
                        quizzesRecyclerView.visibility = View.VISIBLE
                        quizAdapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(tag, "Failed to load quizzes: ${error.message}")
                }
            })
    }
}