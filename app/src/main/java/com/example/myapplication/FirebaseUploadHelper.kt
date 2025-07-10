package com.example.myapplication

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.example.myapplication.models.Quiz
import com.example.myapplication.QuizQuestion
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseUploadHelper {
    companion object {
        private const val TAG = "FirebaseUploadHelper"
        private val database: DatabaseReference = Firebase.database.reference
        private val storage: StorageReference = FirebaseStorage.getInstance().reference
        
        /**
         * Uploads a quiz with its questions to Firebase
         */
        fun uploadQuiz(quiz: Quiz, questions: List<QuizQuestion>, callback: (Boolean, String?) -> Unit) {
            // Generate a unique ID if not provided
            val quizId = if (quiz.id.isNotEmpty()) quiz.id else database.child("quizzes").push().key ?: return
            
            // Create updated quiz with the ID and question count
            val quizWithId = quiz.copy(id = quizId, questionCount = questions.size)
            
            // Prepare quiz data for Firebase
            val quizValues = mapOf(
                "id" to quizWithId.id,
                "title" to quizWithId.title,
                "description" to quizWithId.description,
                "imageUrl" to quizWithId.imageUrl,
                "authorId" to quizWithId.authorId,
                "authorName" to quizWithId.authorName,
                "timestamp" to quizWithId.timestamp,
                "collection" to quizWithId.collection,
                "category" to quizWithId.category,
                "theme" to quizWithId.theme,
                "visibility" to quizWithId.visibility,
                "questionVisibility" to quizWithId.questionVisibility,
                "keywords" to quizWithId.keywords,
                "questionCount" to quizWithId.questionCount
            )
            
            // Upload the main quiz data first
            database.child("quizzes").child(quizId).setValue(quizValues)
                .addOnSuccessListener {
                    Log.d(TAG, "Quiz metadata uploaded successfully")
                    
                    // Upload all questions
                    uploadQuestions(quizId, questions) { questionsSuccess ->
                        if (questionsSuccess) {
                            // Add to appropriate category
                            addToCategory(quizId, quiz.collection) { categorySuccess ->
                                // Add to featured section
                                addToFeatured(quizId) { featuredSuccess ->
                                    // Update user's created quizzes list
                                    if (quiz.authorId.isNotEmpty()) {
                                        addToUserCreations(quizId, quiz.authorId) { userSuccess ->
                                            callback(true, quizId) // Return success regardless of user update
                                        }
                                    } else {
                                        callback(true, quizId)
                                    }
                                }
                            }
                        } else {
                            Log.e(TAG, "Failed to upload questions")
                            callback(false, quizId)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to upload quiz", e)
                    callback(false, null)
                }
        }
        
        /**
         * Uploads all questions for a quiz
         */
        private fun uploadQuestions(quizId: String, questions: List<QuizQuestion>, callback: (Boolean) -> Unit) {
            val questionRef = database.child("quizzes").child(quizId).child("questions")
            var successCount = 0
            var failCount = 0
            
            // Handle empty questions list
            if (questions.isEmpty()) {
                callback(true)
                return
            }
            
            // Upload each question
            questions.forEachIndexed { index, question ->
                val questionData = mapOf(
                    "text" to question.text,
                    "imageUrl" to question.imageUrl,
                    "options" to question.options
                )
                
                questionRef.child(index.toString()).setValue(questionData)
                    .addOnSuccessListener {
                        successCount++
                        if (successCount + failCount == questions.size) {
                            callback(failCount == 0)
                        }
                    }
                    .addOnFailureListener { e ->
                        failCount++
                        Log.e(TAG, "Failed to upload question $index", e)
                        if (successCount + failCount == questions.size) {
                            callback(failCount == 0)
                        }
                    }
            }
        }
        
        /**
         * Adds quiz to the appropriate category
         */
        private fun addToCategory(quizId: String, category: String, callback: (Boolean) -> Unit) {
            val normalizedCategory = category.lowercase().replace(" ", "_")
            database.child("categories").child(normalizedCategory).child(quizId).setValue(true)
                .addOnSuccessListener {
                    Log.d(TAG, "Added quiz to category: $normalizedCategory")
                    callback(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to add quiz to category", e)
                    callback(false)
                }
        }
        
        /**
         * Adds quiz to the featured section (discover)
         */
        private fun addToFeatured(quizId: String, callback: (Boolean) -> Unit) {
            database.child("featured").child("discover").child(quizId).setValue(true)
                .addOnSuccessListener {
                    Log.d(TAG, "Added quiz to featured section")
                    callback(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to add quiz to featured section", e)
                    callback(false)
                }
        }
        
        /**
         * Adds quiz to the user's created quizzes list
         */
        private fun addToUserCreations(quizId: String, userId: String, callback: (Boolean) -> Unit) {
            database.child("users").child(userId).child("createdQuizzes").child(quizId).setValue(true)
                .addOnSuccessListener {
                    Log.d(TAG, "Added quiz to user's creations")
                    callback(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to add quiz to user's creations", e)
                    callback(false)
                }
        }
    }
    
    private val storage = FirebaseStorage.getInstance()
    
    fun uploadImage(
        context: Context,
        imageUri: Uri,
        folderPath: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val fileExtension = getFileExtension(context, imageUri)
        val fileName = "${UUID.randomUUID()}.$fileExtension"
        val fileRef = storage.reference.child("$folderPath/$fileName")
        
        fileRef.putFile(imageUri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                onFailure(e)
            }
    }
    
    private fun getFileExtension(context: Context, uri: Uri): String {
        val contentResolver = context.contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri)) ?: "jpg"
    }
    
    // Use this function when calling from another suspend function
    suspend fun uploadImageAndGetUrl(uri: Uri, folderPath: String): String {
        val fileRef = storage.reference.child("$folderPath/${UUID.randomUUID()}")
        fileRef.putFile(uri).await()
        return fileRef.downloadUrl.await().toString()
    }
}