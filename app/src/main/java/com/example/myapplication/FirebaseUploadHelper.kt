package com.example.myapplication

import android.net.Uri
import android.util.Log
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.*

class FirebaseUploadHelper {
    companion object {
        private const val TAG = "FirebaseUploadHelper"
        private val database = Firebase.database.reference
        private val storage = Firebase.storage.reference
        
        /**
         * Uploads a quiz with its questions to Firebase
         * 
         * @param quiz The quiz metadata
         * @param questions List of questions for the quiz
         * @param callback Called with (success, quizId) when complete
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
                    "options" to question.options,
                    "correctOptionIndex" to question.correctOptionIndex
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
        
        /**
         * Uploads an image to Firebase Storage and returns the download URL
         * 
         * @param uri The local URI of the image
         * @param path The storage path (e.g., "quiz_covers" or "quiz_questions")
         * @param callback Called with the download URL or null if upload failed
         */
        fun uploadImage(uri: Uri, path: String, callback: (String?) -> Unit) {
            val filename = UUID.randomUUID().toString()
            val fileRef = storage.child("$path/$filename.jpg")
            
            fileRef.putFile(uri)
                .addOnSuccessListener {
                    Log.d(TAG, "Image uploaded successfully")
                    fileRef.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            Log.d(TAG, "Download URL: $downloadUri")
                            callback(downloadUri.toString())
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to get download URL", e)
                            callback(null)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to upload image", e)
                    callback(null)
                }
        }
        
        /**
         * Updates an existing quiz
         */
        fun updateQuiz(quiz: Quiz, callback: (Boolean) -> Unit) {
            if (quiz.id.isEmpty()) {
                Log.e(TAG, "Cannot update quiz with empty ID")
                callback(false)
                return
            }
            
            val quizValues = mapOf(
                "title" to quiz.title,
                "description" to quiz.description,
                "imageUrl" to quiz.imageUrl,
                "collection" to quiz.collection,
                "theme" to quiz.theme,
                "visibility" to quiz.visibility,
                "questionVisibility" to quiz.questionVisibility,
                "keywords" to quiz.keywords
            )
            
            database.child("quizzes").child(quiz.id).updateChildren(quizValues)
                .addOnSuccessListener {
                    Log.d(TAG, "Quiz updated successfully")
                    callback(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update quiz", e)
                    callback(false)
                }
        }
        
        /**
         * Deletes a quiz and its references
         */
        fun deleteQuiz(quizId: String, category: String, authorId: String, callback: (Boolean) -> Unit) {
            // Create a map of all updates to perform
            val updates = hashMapOf<String, Any?>()
            
            // Remove from quizzes node
            updates["quizzes/$quizId"] = null
            
            // Remove from category
            val normalizedCategory = category.lowercase().replace(" ", "_")
            updates["categories/$normalizedCategory/$quizId"] = null
            
            // Remove from featured sections
            updates["featured/discover/$quizId"] = null
            updates["featured/trending/$quizId"] = null
            updates["featured/topPicks/$quizId"] = null
            
            // Remove from user's created quizzes
            if (authorId.isNotEmpty()) {
                updates["users/$authorId/createdQuizzes/$quizId"] = null
            }
            
            // Perform all deletions in a single update
            database.updateChildren(updates)
                .addOnSuccessListener {
                    Log.d(TAG, "Quiz and all references deleted successfully")
                    callback(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to delete quiz", e)
                    callback(false)
                }
        }
    }
}