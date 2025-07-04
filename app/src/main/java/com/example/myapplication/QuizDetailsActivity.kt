package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class QuizDetailsActivity : AppCompatActivity() {
    private lateinit var backButton: ImageButton
    private lateinit var quizTitleInput: EditText
    private lateinit var quizDescriptionInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var addCoverImageButton: ImageButton
    private lateinit var coverImageView: ImageView
    private lateinit var createQuestionsButton: Button
    
    private var coverImageUri: Uri? = null
    
    private val categories = listOf(
        "History", "Science", "Math", "Geography", "Entertainment", 
        "Sports", "Technology", "Literature", "Art", "General Knowledge"
    )
    
    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                coverImageUri = uri
                coverImageView.setImageURI(uri)
                coverImageView.visibility = View.VISIBLE
            }
        }
    }
    
    private val createQuestionsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val quizId = result.data?.getStringExtra("quiz_id")
                if (quizId != null) {
                    // Quiz created successfully, go back to main activity
                    val resultIntent = Intent()
                    resultIntent.putExtra("quiz_id", quizId)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            }
        }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_details)
        
        initializeViews()
        setupCategorySpinner()
        setupListeners()
    }
    
    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        quizTitleInput = findViewById(R.id.quizTitleInput)
        quizDescriptionInput = findViewById(R.id.quizDescriptionInput)
        categorySpinner = findViewById(R.id.categorySpinner)
        addCoverImageButton = findViewById(R.id.addCoverImageButton)
        coverImageView = findViewById(R.id.coverImageView)
        createQuestionsButton = findViewById(R.id.createQuestionsButton)
    }
    
    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }
    
    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }
        
        addCoverImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            getContent.launch(intent)
        }
        
        createQuestionsButton.setOnClickListener {
            if (validateInputs()) {
                if (coverImageUri != null) {
                    // Show loading indicator
                    val progressBar = ProgressBar(this)
                    progressBar.visibility = View.VISIBLE
                    val rootLayout = findViewById<LinearLayout>(android.R.id.content).getChildAt(0) as LinearLayout
                    rootLayout.addView(progressBar)
                    
                    // Upload the cover image first, then proceed
                    FirebaseHelper.uploadImage(coverImageUri!!, "quiz_covers") { imageUrl ->
                        runOnUiThread {
                            rootLayout.removeView(progressBar)
                            navigateToCreateQuestions(imageUrl ?: "")
                        }
                    }
                } else {
                    navigateToCreateQuestions("")
                }
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        if (quizTitleInput.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter a quiz title", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (quizDescriptionInput.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter a quiz description", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun navigateToCreateQuestions(coverImageUrl: String) {
        val intent = Intent(this, CreateQuizActivity::class.java)
        intent.putExtra("quiz_title", quizTitleInput.text.toString().trim())
        intent.putExtra("quiz_description", quizDescriptionInput.text.toString().trim())
        intent.putExtra("quiz_category", categorySpinner.selectedItem.toString())
        intent.putExtra("quiz_cover_image", coverImageUrl)
        createQuestionsLauncher.launch(intent)
    }
}