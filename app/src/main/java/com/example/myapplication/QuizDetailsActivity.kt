package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Date

class QuizDetailsActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var quizTitleTextView: TextView
    private lateinit var quizImageView: ImageView
    private lateinit var authorNameTextView: TextView
    private lateinit var questionCountTextView: TextView
    private lateinit var playCountTextView: TextView
    private lateinit var favoriteCountTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var playButton: Button
    private lateinit var favoriteButton: ImageView
    private lateinit var shareButton: ImageView
    private lateinit var progressBar: ProgressBar

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var currentQuiz: Quiz? = null
    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_details)

        // Initialize Firebase components
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // Initialize views
        initializeViews()
        
        // Get quiz ID from intent
        val quizId = intent.getStringExtra("quiz_id")
        if (quizId != null) {
            loadQuizDetails(quizId)
            checkIfFavorite(quizId)
        } else {
            Toast.makeText(this, "Error loading quiz", Toast.LENGTH_SHORT).show()
            finish()
        }
        
        // Set up click listeners
        setupClickListeners(quizId ?: "")
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        quizTitleTextView = findViewById(R.id.quizTitle)
        quizImageView = findViewById(R.id.quizImage)
        authorNameTextView = findViewById(R.id.authorName)
        questionCountTextView = findViewById(R.id.questionCount)
        playCountTextView = findViewById(R.id.playCount)
        favoriteCountTextView = findViewById(R.id.favoriteCount)
        descriptionTextView = findViewById(R.id.description)
        playButton = findViewById(R.id.playButton)
        favoriteButton = findViewById(R.id.favoriteButton)
        shareButton = findViewById(R.id.shareButton)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners(quizId: String) {
        backButton.setOnClickListener {
            finish()
        }

        playButton.setOnClickListener {
            navigateToPlayQuiz(quizId)
        }

        favoriteButton.setOnClickListener {
            toggleFavorite(quizId)
        }

        shareButton.setOnClickListener {
            shareQuiz(quizId)
        }
    }

    private fun loadQuizDetails(quizId: String) {
        progressBar.visibility = View.VISIBLE
        
        database.child("quizzes").child(quizId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    progressBar.visibility = View.GONE
                    
                    val quiz = snapshot.getValue(Quiz::class.java)
                    if (quiz != null) {
                        // Store current quiz
                        currentQuiz = quiz
                        quiz.id = quizId
                        
                        // Update UI with quiz details
                        updateUI(quiz)
                    } else {
                        Toast.makeText(this@QuizDetailsActivity, "Quiz not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@QuizDetailsActivity, "Error loading quiz: ${error.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }
    
    private fun updateUI(quiz: Quiz) {
        quizTitleTextView.text = quiz.title
        authorNameTextView.text = "by ${quiz.authorName}"
        questionCountTextView.text = "${quiz.questionCount} questions"
        playCountTextView.text = "${quiz.playCount} plays"
        favoriteCountTextView.text = "${quiz.favoriteCount} favorites"
        descriptionTextView.text = quiz.description
        
        // Load quiz image
        if (quiz.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(quiz.imageUrl)
                .placeholder(R.drawable.quiz_placeholder)
                .into(quizImageView)
        } else {
            quizImageView.setImageResource(R.drawable.quiz_placeholder)
        }
    }

    private fun checkIfFavorite(quizId: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database.child("users").child(currentUser.uid).child("favoriteQuizzes").child(quizId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        isFavorite = snapshot.exists()
                        updateFavoriteButton()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                    }
                })
        }
    }

    private fun updateFavoriteButton() {
        favoriteButton.setImageResource(
            if (isFavorite) R.drawable.ic_favorite
            else R.drawable.ic_favorite_border
        )
    }

    private fun toggleFavorite(quizId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to favorite quizzes", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userFavoritesRef = database.child("users").child(currentUser.uid)
            .child("favoriteQuizzes").child(quizId)
        
        val quizRef = database.child("quizzes").child(quizId)
        
        if (isFavorite) {
            // Remove from favorites
            userFavoritesRef.removeValue()
                .addOnSuccessListener {
                    isFavorite = false
                    updateFavoriteButton()
                    
                    // Decrement favorite count
                    quizRef.child("favoriteCount").get().addOnSuccessListener { snapshot ->
                        val currentCount = snapshot.getValue(Int::class.java) ?: 0
                        if (currentCount > 0) {
                            quizRef.child("favoriteCount").setValue(currentCount - 1)
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update favorites", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Add to favorites
            userFavoritesRef.setValue(true)
                .addOnSuccessListener {
                    isFavorite = true
                    updateFavoriteButton()
                    
                    // Increment favorite count
                    quizRef.child("favoriteCount").get().addOnSuccessListener { snapshot ->
                        val currentCount = snapshot.getValue(Int::class.java) ?: 0
                        quizRef.child("favoriteCount").setValue(currentCount + 1)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update favorites", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun navigateToPlayQuiz(quizId: String) {
        // Increment play count
        val quizRef = database.child("quizzes").child(quizId)
        quizRef.child("playCount").get().addOnSuccessListener { snapshot ->
            val currentCount = snapshot.getValue(Int::class.java) ?: 0
            quizRef.child("playCount").setValue(currentCount + 1)
            
            // Navigate to play quiz screen
            val intent = Intent(this, StartQuizActivity::class.java)
            intent.putExtra("quiz_id", quizId)
            startActivity(intent)
        }
    }

    private fun shareQuiz(quizId: String) {
        val quiz = currentQuiz ?: return
        
        // Create share intent
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Check out this quiz: ${quiz.title}\n\nhttps://quizapp.example.com/quiz/$quizId")
            type = "text/plain"
        }
        
        // Update share count
        val quizRef = database.child("quizzes").child(quizId)
        quizRef.child("shareCount").get().addOnSuccessListener { snapshot ->
            val currentCount = snapshot.getValue(Int::class.java) ?: 0
            quizRef.child("shareCount").setValue(currentCount + 1)
        }
        
        // Show share dialog
        startActivity(Intent.createChooser(shareIntent, "Share Quiz"))
    }
}