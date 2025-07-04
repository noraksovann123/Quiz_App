package com.example.myapplication

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

// Add this data class for Quiz questions
data class QuizQuestion(
    val text: String = "",
    val imageUrl: String = "",
    val options: List<String> = listOf(),
    val correctOptionIndex: Int = 0
)

// Add this data class for Quiz
data class Quiz(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorImageUrl: String = "",
    val timestamp: Long = 0,
    val collection: String = "General Knowledge",
    val theme: String = "default",
    val visibility: String = "public",
    val questionVisibility: String = "reveal",
    val keywords: List<String> = listOf(),
    val questionCount: Int = 0,
    val playCount: Int = 0,
    val favoriteCount: Int = 0,
    val shareCount: Int = 0,
    var isFavorite: Boolean = false
)

class CreateQuizActivity : AppCompatActivity() {
    // UI Elements
    private lateinit var closeButton: ImageButton
    private lateinit var questionInput: EditText
    private lateinit var addImageButton: ImageButton
    private lateinit var questionImageView: ImageView
    private lateinit var option1Radio: RadioButton
    private lateinit var option2Radio: RadioButton
    private lateinit var option3Radio: RadioButton
    private lateinit var option4Radio: RadioButton
    private lateinit var option1Input: EditText
    private lateinit var option2Input: EditText
    private lateinit var option3Input: EditText
    private lateinit var option4Input: EditText
    private lateinit var addQuestionButton: Button
    private lateinit var doneButton: Button
    
    // Adding missing views
    private lateinit var coverImageContainer: View
    private lateinit var timeButton: Button
    private lateinit var pointsButton: Button
    private lateinit var quizTypeButton: Button
    private lateinit var addMoreQuestionButton: Button
    private lateinit var blueAnswerLayout: View
    private lateinit var redAnswerLayout: View
    private lateinit var orangeAnswerLayout: View
    private lateinit var greenAnswerLayout: View
    
    // Variables for answer text fields
    private lateinit var blueAnswerText: EditText
    private lateinit var redAnswerText: EditText
    private lateinit var orangeAnswerText: EditText
    private lateinit var greenAnswerText: EditText
    
    // Variables
    private var selectedImageUri: Uri? = null
    private var questionImageUrl: String = ""
    private val questions = mutableListOf<QuizQuestion>()
    
    // Quiz details
    private var quizTitle: String = ""
    private var quizDescription: String = ""
    private var quizCategory: String = ""
    private var quizCoverImageUrl: String = ""
    
    // Image picker result
    private val getImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                questionImageView.setImageURI(uri)
                questionImageView.visibility = View.VISIBLE
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_quiz)
        
        // Get quiz details from intent extras (if available)
        intent.extras?.let { bundle ->
            quizTitle = bundle.getString("quiz_title", "")
            quizDescription = bundle.getString("quiz_description", "")
            quizCategory = bundle.getString("quiz_category", "")
            quizCoverImageUrl = bundle.getString("quiz_cover_image", "")
        }
        
        // If no extras, these would be collected in this activity
        if (quizTitle.isEmpty()) {
            showQuizDetailsDialog()
        }
        
        initViews()
        setupListeners()
    }
    
    private fun initViews() {
        closeButton = findViewById(R.id.closeButton)
        questionInput = findViewById(R.id.questionInput)
        addImageButton = findViewById(R.id.addImageButton)
        questionImageView = findViewById(R.id.questionImageView)
        option1Radio = findViewById(R.id.option1Radio)
        option2Radio = findViewById(R.id.option2Radio)
        option3Radio = findViewById(R.id.option3Radio)
        option4Radio = findViewById(R.id.option4Radio)
        option1Input = findViewById(R.id.option1Input)
        option2Input = findViewById(R.id.option2Input)
        option3Input = findViewById(R.id.option3Input)
        option4Input = findViewById(R.id.option4Input)
        addQuestionButton = findViewById(R.id.addQuestionButton)
        doneButton = findViewById(R.id.doneButton)
        
        // Initialize the missing views
        try {
            coverImageContainer = findViewById(R.id.coverImageContainer)
            timeButton = findViewById(R.id.timeButton)
            pointsButton = findViewById(R.id.pointsButton)
            quizTypeButton = findViewById(R.id.quizTypeButton)
            addMoreQuestionButton = findViewById(R.id.addMoreQuestionButton)
            blueAnswerLayout = findViewById(R.id.blueAnswerLayout)
            redAnswerLayout = findViewById(R.id.redAnswerLayout)
            orangeAnswerLayout = findViewById(R.id.orangeAnswerLayout)
            greenAnswerLayout = findViewById(R.id.greenAnswerLayout)
            
            blueAnswerText = findViewById(R.id.blueAnswerText)
            redAnswerText = findViewById(R.id.redAnswerText)
            orangeAnswerText = findViewById(R.id.orangeAnswerText)
            greenAnswerText = findViewById(R.id.greenAnswerText)
        } catch (e: Exception) {
            // Some views might not exist in your current layout, that's fine
        }
    }
    
    private fun setupListeners() {
        // Close button
        closeButton.setOnClickListener {
            if (questions.isEmpty() && questionInput.text.toString().trim().isEmpty()) {
                finish()
            } else {
                showExitConfirmationDialog()
            }
        }
        
        // Image button
        addImageButton.setOnClickListener {
            showImagePickerDialog()
        }
        
        // Radio button handling
        val radioClickListener = View.OnClickListener {
            option1Radio.isChecked = it.id == R.id.option1Radio
            option2Radio.isChecked = it.id == R.id.option2Radio
            option3Radio.isChecked = it.id == R.id.option3Radio
            option4Radio.isChecked = it.id == R.id.option4Radio
        }
        
        option1Radio.setOnClickListener(radioClickListener)
        option2Radio.setOnClickListener(radioClickListener)
        option3Radio.setOnClickListener(radioClickListener)
        option4Radio.setOnClickListener(radioClickListener)
        
        // Add question button
        addQuestionButton.setOnClickListener {
            if (validateQuestion()) {
                if (selectedImageUri != null) {
                    uploadQuestionImage { success ->
                        if (success) {
                            saveCurrentQuestion()
                            clearForm()
                            Toast.makeText(this, "Question added", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    saveCurrentQuestion()
                    clearForm()
                    Toast.makeText(this, "Question added", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Done button
        doneButton.setOnClickListener {
            if (questions.isEmpty() && !validateQuestion()) {
                Toast.makeText(this, "Please add at least one question", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Save current question if valid
            if (validateQuestion(false)) {
                if (selectedImageUri != null) {
                    uploadQuestionImage { success ->
                        if (success) {
                            saveCurrentQuestion()
                            finishQuizCreation()
                        } else {
                            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    saveCurrentQuestion()
                    finishQuizCreation()
                }
            } else {
                // No valid current question, finish with existing questions
                finishQuizCreation()
            }
        }
    }
    
    private fun showImagePickerDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_image_picker, null)
        dialog.setContentView(view)
        
        val onlineMediaButton: Button = view.findViewById(R.id.onlineMediaButton)
        val galleryButton: Button = view.findViewById(R.id.galleryButton)
        val cameraButton: Button = view.findViewById(R.id.cameraButton)
        
        galleryButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            getImage.launch(intent)
            dialog.dismiss()
        }
        
        cameraButton.setOnClickListener {
            // Camera implementation would go here
            dialog.dismiss()
        }
        
        onlineMediaButton.setOnClickListener {
            // Online media implementation would go here
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun validateQuestion(showToast: Boolean = true): Boolean {
        // Question text
        if (questionInput.text.toString().trim().isEmpty()) {
            if (showToast) Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // At least two options
        if (option1Input.text.toString().trim().isEmpty() || option2Input.text.toString().trim().isEmpty()) {
            if (showToast) Toast.makeText(this, "Please enter at least 2 options", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Correct answer selected
        if (!option1Radio.isChecked && !option2Radio.isChecked && 
            !option3Radio.isChecked && !option4Radio.isChecked) {
            if (showToast) Toast.makeText(this, "Please select the correct answer", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun uploadQuestionImage(callback: (Boolean) -> Unit) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Uploading image...")
        progressDialog.setCancelable(false)
        progressDialog.show()
        
        selectedImageUri?.let { uri ->
            FirebaseUploadHelper.uploadImage(uri, "quiz_questions") { url ->
                progressDialog.dismiss()
                
                if (url != null) {
                    questionImageUrl = url
                    callback(true)
                } else {
                    callback(false)
                }
            }
        } ?: run {
            progressDialog.dismiss()
            callback(false)
        }
    }
    
    private fun saveCurrentQuestion() {
        // Get all non-empty options
        val options = mutableListOf<String>()
        
        val option1 = option1Input.text.toString().trim()
        val option2 = option2Input.text.toString().trim()
        val option3 = option3Input.text.toString().trim()
        val option4 = option4Input.text.toString().trim()
        
        if (option1.isNotEmpty()) options.add(option1)
        if (option2.isNotEmpty()) options.add(option2)
        if (option3.isNotEmpty()) options.add(option3)
        if (option4.isNotEmpty()) options.add(option4)
        
        // Get correct option
        val correctOptionIndex = when {
            option1Radio.isChecked -> 0
            option2Radio.isChecked -> 1
            option3Radio.isChecked -> 2
            option4Radio.isChecked -> 3
            else -> 0
        }
        
        // Create and add question
        val question = QuizQuestion(
            text = questionInput.text.toString().trim(),
            imageUrl = questionImageUrl,
            options = options,
            correctOptionIndex = correctOptionIndex
        )
        
        questions.add(question)
    }
    
    private fun clearForm() {
        questionInput.text.clear()
        option1Input.text.clear()
        option2Input.text.clear()
        option3Input.text.clear()
        option4Input.text.clear()
        option1Radio.isChecked = false
        option2Radio.isChecked = false
        option3Radio.isChecked = false
        option4Radio.isChecked = false
        questionImageView.setImageURI(null)
        questionImageView.visibility = View.GONE
        selectedImageUri = null
        questionImageUrl = ""
    }
    
    private fun finishQuizCreation() {
        // Ensure we have quiz details
        if (quizTitle.isEmpty() || quizDescription.isEmpty() || quizCategory.isEmpty()) {
            showQuizDetailsDialog()
            return
        }
        
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Creating quiz...")
        progressDialog.setCancelable(false)
        progressDialog.show()
        
        // Get current user info
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val userId = currentUser?.uid ?: "anonymous"
        val userName = currentUser?.displayName ?: "Anonymous User"
        val userPhotoUrl = currentUser?.photoUrl?.toString() ?: ""
        
        // Create quiz object
        val quiz = Quiz(
            title = quizTitle,
            description = quizDescription,
            imageUrl = quizCoverImageUrl,
            authorId = userId,
            authorName = userName,
            authorImageUrl = userPhotoUrl,
            timestamp = System.currentTimeMillis(),
            collection = quizCategory
        )
        
        // Save to Firebase using FirebaseUploadHelper
        FirebaseUploadHelper.uploadQuiz(quiz, questions) { success, quizId ->
            progressDialog.dismiss()
            
            if (success && quizId != null) {
                Toast.makeText(this, "Quiz created successfully!", Toast.LENGTH_LONG).show()
                
                // Return to previous screen
                val resultIntent = Intent()
                resultIntent.putExtra("quiz_id", quizId)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Failed to create quiz", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showQuizDetailsDialog() {
        // Create dialog view
        val dialogView = layoutInflater.inflate(R.layout.dialog_quiz_details, null)
        
        val titleInput = dialogView.findViewById<EditText>(R.id.quizTitleInput)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.quizDescriptionInput)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.categorySpinner)
        
        // Setup the spinner
        val categories = arrayOf("History", "Science", "Math", "Geography", "Entertainment", 
                               "Sports", "Technology", "Literature", "Art", "General Knowledge")
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
        
        // Pre-fill if we have existing values
        if (quizTitle.isNotEmpty()) titleInput.setText(quizTitle)
        if (quizDescription.isNotEmpty()) descriptionInput.setText(quizDescription)
        if (quizCategory.isNotEmpty()) {
            val position = categories.indexOf(quizCategory)
            if (position >= 0) categorySpinner.setSelection(position)
        }
        
        // Show the dialog
        AlertDialog.Builder(this)
            .setTitle("Quiz Details")
            .setView(dialogView)
            .setPositiveButton("Continue") { _, _ ->
                quizTitle = titleInput.text.toString().trim()
                quizDescription = descriptionInput.text.toString().trim()
                quizCategory = categorySpinner.selectedItem.toString()
                
                // Validate
                if (quizTitle.isEmpty() || quizDescription.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    showQuizDetailsDialog() // Show again if validation fails
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                if (questions.isEmpty()) {
                    finish() // No questions yet, just exit
                }
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Discard Quiz")
            .setMessage("Are you sure you want to discard this quiz? All questions will be lost.")
            .setPositiveButton("Discard") { _, _ -> finish() }
            .setNegativeButton("Keep Editing", null)
            .show()
    }
    
    override fun onBackPressed() {
        if (questions.isEmpty() && questionInput.text.toString().trim().isEmpty()) {
            super.onBackPressed()
        } else {
            showExitConfirmationDialog()
        }
    }
}