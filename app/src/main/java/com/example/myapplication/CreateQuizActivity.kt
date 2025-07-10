package com.example.myapplication

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myapplication.databinding.ActivityCreateQuizBinding
import com.example.myapplication.databinding.DialogEditQuestionBinding
import com.example.myapplication.databinding.FragmentQuizDetailsBinding
import com.example.myapplication.databinding.FragmentQuizQuestionsBinding
import com.example.myapplication.models.Quiz
import com.example.myapplication.QuizOption
import com.example.myapplication.QuizQuestion
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CreateQuizActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateQuizBinding
    private lateinit var quizDetailFragment: QuizDetailFragment
    private lateinit var quizQuestionsFragment: QuizQuestionsFragment
    
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val firebaseUploadHelper = FirebaseUploadHelper()
    
    private var quizId: String = ""
    private var quizCoverImageUri: Uri? = null
    private val questions = mutableListOf<QuizQuestion>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Generate unique quiz ID
        quizId = UUID.randomUUID().toString()
        
        setupViewPager()
        setupToolbar()
    }
    
    private fun setupViewPager() {
        quizDetailFragment = QuizDetailFragment()
        quizQuestionsFragment = QuizQuestionsFragment()
        
        val pagerAdapter = QuizPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Details"
                1 -> "Questions"
                else -> ""
            }
        }.attach()
        
        binding.fabAddQuestion.setOnClickListener {
            if (binding.viewPager.currentItem == 1) {
                showAddQuestionDialog()
            } else {
                binding.viewPager.currentItem = 1
            }
        }
    }
    
    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            showExitConfirmationDialog()
        }
        
        binding.btnSave.setOnClickListener {
            if (validateQuiz()) {
                saveQuizToFirebase()
            }
        }
    }
    
    private fun validateQuiz(): Boolean {
        // Get quiz details
        val title = quizDetailFragment.getQuizTitle()
        val description = quizDetailFragment.getQuizDescription()
        
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a quiz title", Toast.LENGTH_SHORT).show()
            binding.viewPager.currentItem = 0
            return false
        }
        
        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter a quiz description", Toast.LENGTH_SHORT).show()
            binding.viewPager.currentItem = 0
            return false
        }
        
        if (questions.isEmpty()) {
            Toast.makeText(this, "Please add at least one question", Toast.LENGTH_SHORT).show()
            binding.viewPager.currentItem = 1
            return false
        }
        
        return true
    }
    
    private fun saveQuizToFirebase() {
        showProgress(true)
        
        lifecycleScope.launch {
            try {
                // Step 1: Upload cover image if selected
                val imageUrl = if (quizCoverImageUri != null) {
                    uploadImageAndGetUrl(quizCoverImageUri!!, "quiz_covers")
                } else {
                    "" // Default placeholder or empty
                }
                
                // Step 2: Get current user info
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Toast.makeText(this@CreateQuizActivity, "You must be logged in to create a quiz", Toast.LENGTH_SHORT).show()
                    showProgress(false)
                    return@launch
                }
                
                val userSnapshot = database.reference
                    .child("users")
                    .child(currentUser.uid)
                    .get()
                    .await()
                
                val authorName = userSnapshot.child("fullName").value as? String ?: "Anonymous"
                val authorImageUrl = userSnapshot.child("photoUrl").value as? String ?: ""
                
                // Step 3: Create quiz object
                val quiz = Quiz(
                    id = quizId,
                    title = quizDetailFragment.getQuizTitle(),
                    description = quizDetailFragment.getQuizDescription(),
                    authorId = currentUser.uid,
                    authorName = authorName,
                    authorImageUrl = authorImageUrl,
                    imageUrl = imageUrl,
                    collection = quizDetailFragment.getSelectedCategory(),
                    questionCount = questions.size,
                    keywords = quizDetailFragment.getKeywords(),
                    theme = quizDetailFragment.getSelectedTheme(),
                    visibility = quizDetailFragment.getSelectedVisibility(),
                    questionVisibility = quizDetailFragment.getQuestionVisibility(),
                    timestamp = System.currentTimeMillis(),
                    favoriteCount = 0,
                    shareCount = 0,
                    playCount = 0
                )
                
                // Step 4: Save quiz metadata
                database.reference
                    .child("quizzes")
                    .child(quizId)
                    .setValue(quiz)
                    .await()
                
                // Step 5: Save quiz questions
                val questionsMap = mutableMapOf<String, Any>()
                questions.forEach { question ->
                    questionsMap[question.id] = question
                }
                
                database.reference
                    .child("quiz_questions")
                    .child(quizId)
                    .setValue(questionsMap)
                    .await()
                
                // Step 6: Update user's created quizzes
                database.reference
                    .child("user_activity")
                    .child(currentUser.uid)
                    .child("created")
                    .child(quizId)
                    .setValue(System.currentTimeMillis())
                    .await()
                
                // Step 7: Update user's quiz count
                val currentQuizCount = userSnapshot.child("quizCount").getValue(Int::class.java) ?: 0
                database.reference
                    .child("users")
                    .child(currentUser.uid)
                    .child("quizCount")
                    .setValue(currentQuizCount + 1)
                    .await()
                
                // NEW: Step 8: Add to featured section for better visibility
                // This will make sure the quiz appears in the HomeFragment immediately
                
                // Add to featured/discover
                val discoverQuizData = mapOf(
                    "id" to quizId,
                    "timestamp" to System.currentTimeMillis()
                )
                
                database.reference
                    .child("featured")
                    .child("discover")
                    .child(quizId)
                    .setValue(discoverQuizData)
                    .await()
                
                // Add to trending section if it's a public quiz
                if (quiz.visibility == "public") {
                    val trendingQuizData = mapOf(
                        "id" to quizId,
                        "authorName" to authorName,
                        "imageUrl" to imageUrl,
                        "playCount" to 0,
                        "score" to 0,
                        "title" to quiz.title
                    )
                    
                    database.reference
                        .child("featured")
                        .child("trending_quizzes")
                        .child(quizId)
                        .setValue(trendingQuizData)
                        .await()
                }
                
                showProgress(false)
                Toast.makeText(this@CreateQuizActivity, "Quiz created successfully!", Toast.LENGTH_LONG).show()
                
                // Return to previous screen or navigate to home
                finish()
                
            } catch (e: Exception) {
                showProgress(false)
                Toast.makeText(this@CreateQuizActivity, "Failed to create quiz: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun uploadImageAndGetUrl(imageUri: Uri, folder: String): String {
        return try {
            val storageRef = storage.reference.child("$folder/${UUID.randomUUID()}")
            storageRef.putFile(imageUri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw e
        }
    }
    
    private fun showAddQuestionDialog(questionToEdit: QuizQuestion? = null) {
        val dialogBinding = DialogEditQuestionBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
        
        // Set dialog title based on edit/add mode
        dialogBinding.dialogTitle.text = if (questionToEdit == null) "Add Question" else "Edit Question"
        
        // If editing, populate fields
        var questionImageUri: Uri? = null
        if (questionToEdit != null) {
            dialogBinding.etQuestionText.setText(questionToEdit.text)
            if (questionToEdit.imageUrl.isNotEmpty()) {
                dialogBinding.tvImageStatus.text = "Image selected"
            }
            
            // Populate options
            val options = questionToEdit.options
            options["option1"]?.let { 
                dialogBinding.etOption1.setText(it.text)
                dialogBinding.rbOption1.isChecked = it.isCorrect
            }
            options["option2"]?.let { 
                dialogBinding.etOption2.setText(it.text)
                dialogBinding.rbOption2.isChecked = it.isCorrect
            }
            options["option3"]?.let { 
                dialogBinding.etOption3.setText(it.text)
                dialogBinding.rbOption3.isChecked = it.isCorrect
            }
            options["option4"]?.let { 
                dialogBinding.etOption4.setText(it.text)
                dialogBinding.rbOption4.isChecked = it.isCorrect
            }
        }
        
        // Set up image picker
        val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                questionImageUri = it
                dialogBinding.tvImageStatus.text = "Image selected"
            }
        }
        
        dialogBinding.btnAddImage.setOnClickListener {
            getContent.launch("image/*")
        }
        
        // Group radio buttons
        val radioButtons = listOf(
            dialogBinding.rbOption1,
            dialogBinding.rbOption2,
            dialogBinding.rbOption3,
            dialogBinding.rbOption4
        )
        
        radioButtons.forEach { radioButton ->
            radioButton.setOnClickListener {
                radioButtons.forEach { rb ->
                    rb.isChecked = rb == radioButton
                }
            }
        }
        
        // Cancel button
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // Save button
        dialogBinding.btnSaveQuestion.setOnClickListener {
            // Validate inputs
            val questionText = dialogBinding.etQuestionText.text.toString().trim()
            val option1Text = dialogBinding.etOption1.text.toString().trim()
            val option2Text = dialogBinding.etOption2.text.toString().trim()
            val option3Text = dialogBinding.etOption3.text.toString().trim()
            val option4Text = dialogBinding.etOption4.text.toString().trim()
            
            if (questionText.isEmpty()) {
                dialogBinding.etQuestionText.error = "Question is required"
                return@setOnClickListener
            }
            
            if (option1Text.isEmpty() || option2Text.isEmpty()) {
                Toast.makeText(this, "At least first two options are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Check if a correct answer is selected
            val correctAnswerSelected = radioButtons.any { it.isChecked }
            if (!correctAnswerSelected) {
                Toast.makeText(this, "Please select a correct answer", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Create options map
            val options = mutableMapOf<String, QuizOption>()
            
            if (option1Text.isNotEmpty()) {
                options["option1"] = QuizOption(option1Text, dialogBinding.rbOption1.isChecked)
            }
            
            if (option2Text.isNotEmpty()) {
                options["option2"] = QuizOption(option2Text, dialogBinding.rbOption2.isChecked)
            }
            
            if (option3Text.isNotEmpty()) {
                options["option3"] = QuizOption(option3Text, dialogBinding.rbOption3.isChecked)
            }
            
            if (option4Text.isNotEmpty()) {
                options["option4"] = QuizOption(option4Text, dialogBinding.rbOption4.isChecked)
            }
            
            // Show loading
            dialogBinding.btnSaveQuestion.isEnabled = false
            dialogBinding.btnSaveQuestion.text = "Saving..."
            
            // Handle image upload if needed
            if (questionImageUri != null) {
                firebaseUploadHelper.uploadImage(
                    context = this,
                    imageUri = questionImageUri!!,
                    folderPath = "question_images",
                    onSuccess = { imageUrl ->
                        saveQuestion(questionToEdit, questionText, imageUrl, options)
                        dialog.dismiss()
                    },
                    onFailure = { exception ->
                        dialogBinding.btnSaveQuestion.isEnabled = true
                        dialogBinding.btnSaveQuestion.text = "Save"
                        Toast.makeText(this, "Failed to upload image: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // No image to upload, save question directly
                val imageUrl = questionToEdit?.imageUrl ?: ""
                saveQuestion(questionToEdit, questionText, imageUrl, options)
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
    
    private fun saveQuestion(
        questionToEdit: QuizQuestion?,
        questionText: String,
        imageUrl: String,
        options: Map<String, QuizOption>
    ) {
        if (questionToEdit == null) {
            // Add new question
            val questionId = UUID.randomUUID().toString()
            val newQuestion = QuizQuestion(
                id = questionId,
                text = questionText,
                imageUrl = imageUrl,
                options = options
            )
            questions.add(newQuestion)
        } else {
            // Update existing question
            val index = questions.indexOfFirst { it.id == questionToEdit.id }
            if (index >= 0) {
                questions[index] = questionToEdit.copy(
                    text = questionText,
                    imageUrl = imageUrl,
                    options = options
                )
            }
        }
        
        // Refresh questions list
        quizQuestionsFragment.updateQuestions(questions)
    }
    
    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Discard Changes")
            .setMessage("Are you sure you want to exit? Any unsaved changes will be lost.")
            .setPositiveButton("Exit") { _, _ ->
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showProgress(show: Boolean) {
        binding.progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    override fun onBackPressed() {
        showExitConfirmationDialog()
    }
    
    // ViewPager adapter
    inner class QuizPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 2
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> quizDetailFragment
                1 -> quizQuestionsFragment
                else -> throw IllegalStateException("Invalid position $position")
            }
        }
    }
    
    // Quiz Details Fragment
    class QuizDetailFragment : Fragment(R.layout.fragment_quiz_details) {
        private var _binding: FragmentQuizDetailsBinding? = null
        private val binding get() = _binding!!
        private var quizCoverImageUri: Uri? = null
        
        private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                quizCoverImageUri = it
                binding.quizCoverImage.setImageURI(it)
                binding.quizCoverImage.visibility = View.VISIBLE
            }
        }
        
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            _binding = FragmentQuizDetailsBinding.bind(view)
            
            setupCategoryDropdown()
            
            // Image selection
            binding.btnSelectImage.setOnClickListener {
                getContent.launch("image/*")
            }
        }
        
        private fun setupCategoryDropdown() {
            val categories = listOf(
                "History", "Science", "Geography", "Mathematics", 
                "Literature", "Sports", "Entertainment", "Technology", "Art", "General Knowledge"
            )
            
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
            binding.categoryDropdown.setAdapter(adapter)
        }
        
        fun getQuizTitle(): String = binding.etQuizTitle.text.toString().trim()
        
        fun getQuizDescription(): String = binding.etQuizDescription.text.toString().trim()
        
        fun getSelectedCategory(): String = binding.categoryDropdown.text.toString().trim()
        
        fun getKeywords(): List<String> {
            val keywordsText = binding.etKeywords.text.toString().trim()
            return if (keywordsText.isNotEmpty()) {
                keywordsText.split(",").map { it.trim() }
            } else {
                emptyList()
            }
        }
        
        fun getSelectedTheme(): String {
            return when {
                binding.rbThemeClassic.isChecked -> "classic"
                binding.rbThemeDark.isChecked -> "dark"
                binding.rbThemeTravel.isChecked -> "travel"
                else -> "classic"
            }
        }
        
        fun getSelectedVisibility(): String {
            return if (binding.rbVisibilityPublic.isChecked) "public" else "private"
        }
        
        fun getQuestionVisibility(): String {
            return if (binding.rbShowAll.isChecked) "reveal" else "one_by_one"
        }
        
        fun getQuizCoverImageUri(): Uri? = quizCoverImageUri
        
        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }
    
    // Questions Fragment
    class QuizQuestionsFragment : Fragment(R.layout.fragment_quiz_questions) {
        private var _binding: FragmentQuizQuestionsBinding? = null
        private val binding get() = _binding!!
        
        private lateinit var adapter: QuestionAdapter
        
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            _binding = FragmentQuizQuestionsBinding.bind(view)
            
            setupRecyclerView()
        }
        
        private fun setupRecyclerView() {
            adapter = QuestionAdapter(
                questions = mutableListOf(),
                onEditQuestion = { question ->
                    (activity as? CreateQuizActivity)?.showAddQuestionDialog(question)
                },
                onDeleteQuestion = { question ->
                    confirmDeleteQuestion(question)
                }
            )
            
            binding.questionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.questionsRecyclerView.adapter = adapter
            
            updateEmptyState()
        }
        
        fun updateQuestions(questions: List<QuizQuestion>) {
            adapter.updateQuestions(questions)
            updateEmptyState()
        }
        
        private fun confirmDeleteQuestion(question: QuizQuestion) {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Question")
                .setMessage("Are you sure you want to delete this question?")
                .setPositiveButton("Delete") { _, _ ->
                    val activity = activity as? CreateQuizActivity
                    activity?.questions?.removeAll { it.id == question.id }
                    activity?.let { updateQuestions(it.questions) }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        private fun updateEmptyState() {
            if (adapter.itemCount == 0) {
                binding.emptyStateText.visibility = View.VISIBLE
                binding.questionsRecyclerView.visibility = View.GONE
            } else {
                binding.emptyStateText.visibility = View.GONE
                binding.questionsRecyclerView.visibility = View.VISIBLE
            }
        }
        
        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }
}