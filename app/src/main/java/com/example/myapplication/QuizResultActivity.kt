package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class QuizResultActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var quizTitleText: TextView
    private lateinit var scoreText: TextView
    private lateinit var percentageText: TextView
    private lateinit var resultMessageText: TextView
    private lateinit var tryAgainButton: Button
    private lateinit var homeButton: Button

    // Quiz data
    private var quizId: String? = null
    private var quizTitle: String? = null
    private var score: Int = 0
    private var totalQuestions: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_result)
        
        // Get score from intent
        quizId = intent.getStringExtra("quiz_id")
        quizTitle = intent.getStringExtra("quiz_title")
        score = intent.getIntExtra("score", 0)
        totalQuestions = intent.getIntExtra("total_questions", 0)
        
        // Initialize views
        initViews()
        
        // Calculate percentage
        val percentage = if (totalQuestions > 0) {
            (score * 100) / totalQuestions
        } else {
            0
        }
        
        // Update UI
        quizTitleText.text = quizTitle ?: "Quiz"
        scoreText.text = "$score/$totalQuestions"
        percentageText.text = "$percentage%"
        resultMessageText.text = getResultMessage(percentage)
        
        // Set up buttons
        setupClickListeners()
        
        // Save result to Firebase
        if (quizId != null) {
            saveResult(quizId!!, score, totalQuestions)
        }
    }
    
    private fun initViews() {
        quizTitleText = findViewById(R.id.quizTitleText)
        scoreText = findViewById(R.id.scoreText)
        percentageText = findViewById(R.id.percentageText)
        resultMessageText = findViewById(R.id.resultMessageText)
        tryAgainButton = findViewById(R.id.tryAgainButton)
        homeButton = findViewById(R.id.homeButton)
    }
    
    private fun setupClickListeners() {
        tryAgainButton.setOnClickListener {
            // Start the quiz again
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("quiz_id", quizId)
            intent.putExtra("quiz_title", quizTitle)
            startActivity(intent)
            finish()
        }
        
        homeButton.setOnClickListener {
            // Go back to home
            finish()
        }
    }
    
    private fun getResultMessage(percentage: Int): String {
        return when {
            percentage >= 90 -> "Excellent! You're a quiz master!"
            percentage >= 70 -> "Great job! You know your stuff!"
            percentage >= 50 -> "Not bad! Keep learning!"
            else -> "Practice makes perfect! Try again!"
        }
    }
    
    private fun saveResult(quizId: String, score: Int, totalQuestions: Int) {
        // Get current user
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        // Save result if user is logged in
        if (currentUser != null) {
            val database = FirebaseDatabase.getInstance().reference
            val resultRef = database
                .child("users")
                .child(currentUser.uid)
                .child("results")
                .child(quizId)
            
            val resultData = HashMap<String, Any>()
            resultData["score"] = score
            resultData["totalQuestions"] = totalQuestions
            resultData["timestamp"] = System.currentTimeMillis()
            resultData["percentage"] = if (totalQuestions > 0) (score * 100) / totalQuestions else 0
            
            resultRef.setValue(resultData)
        }
    }
}