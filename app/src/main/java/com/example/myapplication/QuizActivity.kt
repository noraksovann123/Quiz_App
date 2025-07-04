package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class QuizActivity : AppCompatActivity() {
    
    private val TAG = "QuizActivity"
    
    // UI components from quizes.xml
    private lateinit var progressText: TextView
    private lateinit var quizTitle: TextView
    private lateinit var timerText: TextView
    private lateinit var backButton: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var resultBanner: TextView
    private lateinit var questionImage: ImageView
    private lateinit var questionText: TextView
    private lateinit var optionButtons: Array<Button>
    private lateinit var nextButton: Button
    
    // Firebase
    private lateinit var database: DatabaseReference
    
    // Quiz data
    private var quizId: String? = null
    private var quizData: Map<String, Any>? = null
    private var questions: List<Map<String, Any>> = listOf()
    private var currentQuestionIndex = 0
    private var score = 0
    
    // Randomization
    private var shouldRandomize = false
    private var questionOrder: List<Int> = listOf()
    
    // Timer
    private lateinit var countDownTimer: CountDownTimer
    private val questionTimeInSeconds = 30L
    
    // User answers for result display
    private val userAnswers = mutableListOf<Int?>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quizes)
        
        // Get quiz ID and randomization flag from intent
        quizId = intent.getStringExtra("quiz_id")
        shouldRandomize = intent.getBooleanExtra("randomize", false)
        
        if (quizId == null) {
            Toast.makeText(this, "Quiz not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference
        
        // Initialize UI components
        initializeViews()
        
        // Load quiz data from Firebase
        loadQuiz()
    }
    
    private fun initializeViews() {
        progressText = findViewById(R.id.progressText)
        quizTitle = findViewById(R.id.quizTitle)
        timerText = findViewById(R.id.timerText)
        backButton = findViewById(R.id.backButton)
        progressBar = findViewById(R.id.progressBar)
        resultBanner = findViewById(R.id.result_banner)
        questionImage = findViewById(R.id.questionImage)
        questionText = findViewById(R.id.questionText)
        
        // Initialize option buttons
        optionButtons = arrayOf(
            findViewById(R.id.btn_option_1),
            findViewById(R.id.btn_option_2),
            findViewById(R.id.btn_option_3),
            findViewById(R.id.btn_option_4)
        )
        
        nextButton = findViewById(R.id.btn_next)
        
        // Set click listeners
        backButton.setOnClickListener {
            showExitConfirmation()
        }
        
        nextButton.setOnClickListener {
            goToNextQuestion()
        }
        
        // Set click listeners for option buttons
        for (i in optionButtons.indices) {
            val optionIndex = i  // Need to capture i in a final variable for the lambda
            optionButtons[i].setOnClickListener {
                selectAnswer(optionIndex)
            }
        }
    }
    
    private fun loadQuiz() {
        database.child("quizzes").child(quizId!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                @Suppress("UNCHECKED_CAST")
                quizData = snapshot.value as? Map<String, Any>
                
                if (quizData == null) {
                    Toast.makeText(this@QuizActivity, "Quiz not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
                
                quizTitle.text = quizData!!["title"] as? String ?: "Quiz"
                
                // Load questions
                @Suppress("UNCHECKED_CAST")
                val questionsData = quizData!!["questions"] as? Map<String, Any>
                if (questionsData != null) {
                    // Sort questions by index to ensure correct order
                    val sortedQuestions = questionsData.entries
                        .sortedBy { it.key.toIntOrNull() ?: Int.MAX_VALUE }
                        .map { it.value as Map<String, Any> }
                        
                    questions = sortedQuestions
                    
                    // Initialize userAnswers array with nulls (no answer)
                    userAnswers.clear()
                    for (i in questions.indices) {
                        userAnswers.add(null)
                    }
                    
                    // Create question order - either sequential or randomized
                    setupQuestionOrder()
                    
                    startQuiz()
                } else {
                    Toast.makeText(this@QuizActivity, "No questions found for this quiz", Toast.LENGTH_SHORT).show()
                    finish()
                }
                
                // Update play count
                updatePlayCount()
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load quiz", error.toException())
                Toast.makeText(this@QuizActivity, "Failed to load quiz: ${error.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }
    
    private fun setupQuestionOrder() {
        questionOrder = if (shouldRandomize) {
            // Create randomized order
            val indices = List(questions.size) { it }
            indices.shuffled()
        } else {
            // Use sequential order
            List(questions.size) { it }
        }
    }
    
    private fun updatePlayCount() {
        database.child("quizzes").child(quizId!!).child("playCount")
            .runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val currentValue = mutableData.getValue(Int::class.java) ?: 0
                    mutableData.value = currentValue + 1
                    return Transaction.success(mutableData)
                }
                
                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    // Not necessary to handle completion
                }
            })
    }
    
    private fun startQuiz() {
        // Update total question count
        progressText.text = "1/${questions.size}"
        progressBar.max = questions.size
        progressBar.progress = 1
        
        // Show first question
        showQuestion(0)
    }
    
    private fun showQuestion(index: Int) {
        currentQuestionIndex = index
        
        // Get the actual question using the randomized or sequential order
        val actualQuestionIndex = questionOrder[index]
        val question = questions[actualQuestionIndex]
        
        // Reset UI for new question
        resultBanner.visibility = View.GONE
        nextButton.visibility = View.GONE
        
        for (button in optionButtons) {
            button.isEnabled = true
            button.setBackgroundResource(R.drawable.option_button_normal)
        }
        
        // Set question text
        questionText.text = question["text"] as? String ?: "Question"
        
        // Load question image if exists
        val imageUrl = question["imageUrl"] as? String
        if (imageUrl != null && imageUrl.isNotEmpty()) {
            questionImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(imageUrl)
                .centerCrop()
                .into(questionImage)
        } else {
            questionImage.visibility = View.GONE
        }
        
        // Set options
        @Suppress("UNCHECKED_CAST")
        val options = question["options"] as? List<String> ?: listOf()
        for (i in optionButtons.indices) {
            if (i < options.size) {
                optionButtons[i].text = options[i]
                optionButtons[i].visibility = View.VISIBLE
            } else {
                optionButtons[i].visibility = View.GONE
            }
        }
        
        // Update progress
        progressText.text = "${index + 1}/${questions.size}"
        progressBar.progress = index + 1
        
        // Start timer
        startTimer()
    }
    
    private fun startTimer() {
        // Cancel previous timer if it exists
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
        
        timerText.text = questionTimeInSeconds.toString()
        
        countDownTimer = object : CountDownTimer(questionTimeInSeconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                timerText.text = seconds.toString()
                
                // Change color to red when less than 5 seconds left
                if (seconds < 5) {
                    timerText.setTextColor(ContextCompat.getColor(this@QuizActivity, android.R.color.holo_red_light))
                } else {
                    timerText.setTextColor(ContextCompat.getColor(this@QuizActivity, android.R.color.black))
                }
            }
            
            override fun onFinish() {
                timerText.text = "0"
                timerText.setTextColor(ContextCompat.getColor(this@QuizActivity, android.R.color.holo_red_light))
                timeUp()
            }
        }.start()
    }
    
    private fun selectAnswer(selectedOptionIndex: Int) {
        // Stop timer
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
        
        // Disable all buttons
        for (button in optionButtons) {
            button.isEnabled = false
        }
        
        // Get the actual question using the randomized or sequential order
        val actualQuestionIndex = questionOrder[currentQuestionIndex]
        val question = questions[actualQuestionIndex]
        
        // Record user's answer
        userAnswers[actualQuestionIndex] = selectedOptionIndex
        
        // Get correct answer
        val correctIndex = (question["correctOptionIndex"] as Long).toInt()
        
        // Check if answer is correct
        if (selectedOptionIndex == correctIndex) {
            // Correct answer
            score++
            resultBanner.text = "Correct!"
            resultBanner.setBackgroundColor(ContextCompat.getColor(this, R.color.correct_answer))
            optionButtons[selectedOptionIndex].backgroundTintList = 
                ContextCompat.getColorStateList(this, R.color.correct_answer)
        } else {
            // Wrong answer
            resultBanner.text = "Wrong!"
            resultBanner.setBackgroundColor(ContextCompat.getColor(this, R.color.wrong_answer))
            optionButtons[selectedOptionIndex].backgroundTintList = 
                ContextCompat.getColorStateList(this, R.color.wrong_answer)
            
            // Highlight correct answer
            if (correctIndex >= 0 && correctIndex < optionButtons.size) {
                optionButtons[correctIndex].backgroundTintList = 
                    ContextCompat.getColorStateList(this, R.color.correct_answer)
            }
        }
        
        // Show result banner
        resultBanner.visibility = View.VISIBLE
        
        // Show next button if not the last question
        if (currentQuestionIndex < questions.size - 1) {
            nextButton.visibility = View.VISIBLE
        } else {
            // Last question, change next button text
            nextButton.text = "See Results"
            nextButton.visibility = View.VISIBLE
        }
    }
    
    private fun timeUp() {
        // Time's up - highlight correct answer
        val question = questions[currentQuestionIndex]
        val correctOptionIndex = (question["correctOptionIndex"] as Long).toInt()
        
        // Disable all buttons
        for (button in optionButtons) {
            button.isEnabled = false
        }
        
        // Highlight correct answer
        if (correctOptionIndex >= 0 && correctOptionIndex < optionButtons.size) {
            optionButtons[correctOptionIndex].backgroundTintList = 
                ContextCompat.getColorStateList(this, R.color.correct_answer)
        }
        
        // Show time's up message
        resultBanner.text = "Time's Up!"
        resultBanner.setBackgroundColor(ContextCompat.getColor(this, R.color.wrong_answer))
        resultBanner.visibility = View.VISIBLE
        
        // Show next button if not the last question
        if (currentQuestionIndex < questions.size - 1) {
            nextButton.visibility = View.VISIBLE
        } else {
            // Last question, change next button text
            nextButton.text = "See Results"
            nextButton.visibility = View.VISIBLE
        }
    }
    
    private fun goToNextQuestion() {
        currentQuestionIndex++
        
        if (currentQuestionIndex < questions.size) {
            // Show next question
            showQuestion(currentQuestionIndex)
        } else {
            // Quiz completed, show results
            showResults()
        }
    }
    
    private fun showResults() {
        // Navigate to results screen
        val intent = Intent(this, QuizResultActivity::class.java)
        intent.putExtra("quiz_id", quizId)
        intent.putExtra("quiz_title", quizTitle)
        intent.putExtra("score", score)
        intent.putExtra("total_questions", questions.size)
        intent.putExtra("user_answers", ArrayList(userAnswers))
        startActivity(intent)
        finish()
    }
    
    private fun showExitConfirmation() {
        // Show dialog to confirm exit
        AlertDialog.Builder(this)
            .setTitle("Exit Quiz")
            .setMessage("Are you sure you want to exit? Your progress will be lost.")
            .setPositiveButton("Exit") { _, _ -> finish() }
            .setNegativeButton("Continue", null)
            .show()
    }
    
    override fun onBackPressed() {
        showExitConfirmation()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
    }
}