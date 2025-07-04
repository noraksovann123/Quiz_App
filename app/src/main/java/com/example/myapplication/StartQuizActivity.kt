package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class StartQuizActivity : AppCompatActivity() {
    private val TAG = "StartQuizActivity"

    // UI Elements
    private lateinit var closeButton: ImageView
    private lateinit var starButton: ImageView
    private lateinit var menuButton: ImageView
    
    private lateinit var quizImage: ImageView
    private lateinit var quizTitle: TextView
    
    private lateinit var questionCountText: TextView
    private lateinit var playedCountText: TextView
    private lateinit var favoritesCountText: TextView
    private lateinit var sharedCountText: TextView
    
    private lateinit var authorImage: CircleImageView
    private lateinit var authorNameText: TextView
    private lateinit var authorUsernameText: TextView
    private lateinit var followButton: Button
    
    private lateinit var descriptionText: TextView
    
    private lateinit var playSoloButton: Button
    private lateinit var playWithFriendsButton: Button
    
    // Firebase
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    
    // Quiz data
    private var quizId: String? = null
    private var quiz: Quiz? = null
    private var isFollowing = false
    private var isFavorite = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.start_quiz)
        
        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
        
        // Get quiz ID from intent
        quizId = intent.getStringExtra("quiz_id")
        
        if (quizId == null) {
            Toast.makeText(this, "Quiz not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Initialize views
        initViews()
        
        // Set click listeners
        setupClickListeners()
        
        // Load quiz data
        loadQuizData()
    }
    
    private fun initViews() {
        // Top bar
        closeButton = findViewById(R.id.closeButton)
        starButton = findViewById(R.id.starButton)
        menuButton = findViewById(R.id.menuButton)
        
        // Quiz info
        quizImage = findViewById(R.id.quizImage)
        quizTitle = findViewById(R.id.quizTitle)
        
        // Stats
        questionCountText = findViewById(R.id.questionCountText)
        playedCountText = findViewById(R.id.playedCountText)
        favoritesCountText = findViewById(R.id.favoritesCountText)
        sharedCountText = findViewById(R.id.sharedCountText)
        
        // Author
        authorImage = findViewById(R.id.authorImage)
        authorNameText = findViewById(R.id.authorNameText)
        authorUsernameText = findViewById(R.id.authorUsernameText)
        followButton = findViewById(R.id.followButton)
        
        // Description
        descriptionText = findViewById(R.id.descriptionText)
        
        // Buttons
        playSoloButton = findViewById(R.id.playSoloButton)
        playWithFriendsButton = findViewById(R.id.playWithFriendsButton)
    }
    
    private fun setupClickListeners() {
        // Close button
        closeButton.setOnClickListener {
            finish()
        }
        
        // Star button (favorite)
        starButton.setOnClickListener {
            if (auth.currentUser != null) {
                toggleFavorite()
            } else {
                Toast.makeText(this, "Please log in to favorite quizzes", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Menu button
        menuButton.setOnClickListener {
            // Show options menu - for future implementation
            Toast.makeText(this, "Menu options coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Follow button
        followButton.setOnClickListener {
            if (auth.currentUser != null) {
                toggleFollow()
            } else {
                Toast.makeText(this, "Please log in to follow authors", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Play solo button - starts the actual quiz game
        playSoloButton.setOnClickListener {
            startQuiz()
        }
        
        // Play with friends button
        playWithFriendsButton.setOnClickListener {
            Toast.makeText(this, "Multiplayer feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadQuizData() {
        // Show loading state (you could add a ProgressBar)
        
        // Fetch quiz data from Firebase
        database.child("quizzes").child(quizId!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Hide loading state
                
                // Parse quiz data
                val title = snapshot.child("title").getValue(String::class.java) ?: ""
                val description = snapshot.child("description").getValue(String::class.java) ?: ""
                val imageUrl = snapshot.child("imageUrl").getValue(String::class.java) ?: ""
                val authorId = snapshot.child("authorId").getValue(String::class.java) ?: ""
                val authorName = snapshot.child("authorName").getValue(String::class.java) ?: ""
                val authorImageUrl = snapshot.child("authorImageUrl").getValue(String::class.java) ?: ""
                val questionCount = snapshot.child("questionCount").getValue(Int::class.java) ?: 0
                val playCount = snapshot.child("playCount").getValue(Int::class.java) ?: 0
                val favoriteCount = snapshot.child("favoriteCount").getValue(Int::class.java) ?: 0
                val shareCount = snapshot.child("shareCount").getValue(Int::class.java) ?: 0
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0
                val category = snapshot.child("category").getValue(String::class.java) ?: ""
                
                quiz = Quiz(
                    quizId!!, title, description, imageUrl,
                    authorId, authorName, authorImageUrl,
                    questionCount, playCount, favoriteCount, shareCount,
                    false, false, timestamp, category
                )
                
                displayQuizData()
                
                // Also fetch author details and user specific data (following, favorites)
                checkUserData()
            }
            
            override fun onCancelled(error: DatabaseError) {
                // Hide loading state
                Log.e(TAG, "Error loading quiz: ${error.message}")
                Toast.makeText(this@StartQuizActivity, "Failed to load quiz", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }
    
    private fun checkUserData() {
        val currentUser = auth.currentUser
        
        if (currentUser != null && quiz != null) {
            // Check if user has favorited this quiz
            database.child("users")
                .child(currentUser.uid)
                .child("favorites")
                .child(quizId!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        isFavorite = snapshot.exists()
                        updateFavoriteButton()
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error checking favorite status: ${error.message}")
                    }
                })
            
            // Check if user is following the author
            val authorId = quiz!!.authorId
            database.child("users")
                .child(currentUser.uid)
                .child("following")
                .child(authorId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        isFollowing = snapshot.exists()
                        updateFollowButton()
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error checking following status: ${error.message}")
                    }
                })
        }
    }
    
    private fun displayQuizData() {
        quiz?.let { quiz ->
            // Set quiz title
            quizTitle.text = quiz.title
            
            // Load quiz image
            if (quiz.imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(quiz.imageUrl)
                    .placeholder(R.drawable.quiz_placeholder)
                    .error(R.drawable.quiz_placeholder)
                    .centerCrop()
                    .into(quizImage)
            }
            
            // Set quiz stats
            questionCountText.text = quiz.questionCount.toString()
            playedCountText.text = formatNumber(quiz.playCount)
            favoritesCountText.text = formatNumber(quiz.favoriteCount)
            sharedCountText.text = formatNumber(quiz.shareCount)
            
            // Set author info
            authorNameText.text = quiz.authorName
            authorUsernameText.text = "@${quiz.authorId.take(10).lowercase().replace(" ", "_")}"
            
            // Load author image
            if (quiz.authorImageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(quiz.authorImageUrl)
                    .placeholder(R.drawable.author_placeholder)
                    .error(R.drawable.author_placeholder)
                    .circleCrop()
                    .into(authorImage)
            }
            
            // Set description
            descriptionText.text = quiz.description.ifEmpty { "No description available." }
        }
    }
    
    private fun formatNumber(number: Int): String {
        return when {
            number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
            number >= 1000 -> String.format("%.1fk", number / 1000.0)
            else -> number.toString()
        }
    }
    
    private fun updateFavoriteButton() {
        // Set star button appearance based on favorite status
        starButton.setImageResource(if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star)
    }
    
    private fun toggleFavorite() {
        isFavorite = !isFavorite
        updateFavoriteButton()
        
        val currentUser = auth.currentUser ?: return
        
        if (isFavorite) {
            // Add to favorites
            database.child("users")
                .child(currentUser.uid)
                .child("favorites")
                .child(quizId!!)
                .setValue(true)
            
            // Increment favorite count
            database.child("quizzes")
                .child(quizId!!)
                .child("favoriteCount")
                .runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        var count = currentData.getValue(Int::class.java) ?: 0
                        count++
                        currentData.value = count
                        return Transaction.success(currentData)
                    }
                    
                    override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                        if (error != null) {
                            Log.e(TAG, "Error updating favorite count: ${error.message}")
                        } else if (committed) {
                            // Update the displayed count
                            val newCount = snapshot?.getValue(Int::class.java) ?: 0
                            favoritesCountText.text = formatNumber(newCount)
                        }
                    }
                })
        } else {
            // Remove from favorites
            database.child("users")
                .child(currentUser.uid)
                .child("favorites")
                .child(quizId!!)
                .removeValue()
            
            // Decrement favorite count
            database.child("quizzes")
                .child(quizId!!)
                .child("favoriteCount")
                .runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        var count = currentData.getValue(Int::class.java) ?: 0
                        if (count > 0) count--
                        currentData.value = count
                        return Transaction.success(currentData)
                    }
                    
                    override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                        if (error != null) {
                            Log.e(TAG, "Error updating favorite count: ${error.message}")
                        } else if (committed) {
                            // Update the displayed count
                            val newCount = snapshot?.getValue(Int::class.java) ?: 0
                            favoritesCountText.text = formatNumber(newCount)
                        }
                    }
                })
        }
    }
    
    private fun updateFollowButton() {
        if (isFollowing) {
            followButton.text = "Following"
            followButton.setBackgroundResource(R.drawable.button_following)
            followButton.setTextColor(resources.getColor(R.color.primary))
        } else {
            followButton.text = "Follow"
            followButton.setBackgroundResource(R.drawable.button_follow)
            followButton.setTextColor(resources.getColor(android.R.color.white))
        }
    }
    
    private fun toggleFollow() {
        quiz?.let { quiz ->
            val currentUser = auth.currentUser ?: return
            
            // Don't allow following yourself
            if (currentUser.uid == quiz.authorId) {
                Toast.makeText(this, "You cannot follow yourself", Toast.LENGTH_SHORT).show()
                return
            }
            
            isFollowing = !isFollowing
            updateFollowButton()
            
            if (isFollowing) {
                // Follow the author
                database.child("users")
                    .child(currentUser.uid)
                    .child("following")
                    .child(quiz.authorId)
                    .setValue(true)
                
                // Add follower to author's followers
                database.child("users")
                    .child(quiz.authorId)
                    .child("followers")
                    .child(currentUser.uid)
                    .setValue(true)
                
                // Increment follower count
                database.child("users")
                    .child(quiz.authorId)
                    .child("followerCount")
                    .runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            var count = currentData.getValue(Int::class.java) ?: 0
                            count++
                            currentData.value = count
                            return Transaction.success(currentData)
                        }
                        
                        override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                            if (error != null) {
                                Log.e(TAG, "Error updating follower count: ${error.message}")
                            }
                        }
                    })
            } else {
                // Unfollow the author
                database.child("users")
                    .child(currentUser.uid)
                    .child("following")
                    .child(quiz.authorId)
                    .removeValue()
                
                // Remove follower from author's followers
                database.child("users")
                    .child(quiz.authorId)
                    .child("followers")
                    .child(currentUser.uid)
                    .removeValue()
                
                // Decrement follower count
                database.child("users")
                    .child(quiz.authorId)
                    .child("followerCount")
                    .runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            var count = currentData.getValue(Int::class.java) ?: 0
                            if (count > 0) count--
                            currentData.value = count
                            return Transaction.success(currentData)
                        }
                        
                        override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                            if (error != null) {
                                Log.e(TAG, "Error updating follower count: ${error.message}")
                            }
                        }
                    })
            }
        }
    }
    
    private fun startQuiz() {
        quiz?.let { quiz ->
            // Increment play count
            incrementPlayCount(quiz.id)
            
            // Start the quiz activity
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("quiz_id", quiz.id)
            intent.putExtra("quiz_title", quiz.title)
            startActivity(intent)
        }
    }
    
    private fun incrementPlayCount(quizId: String) {
        database.child("quizzes").child(quizId).child("playCount")
            .runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    var count = currentData.getValue(Int::class.java) ?: 0
                    count++
                    currentData.value = count
                    return Transaction.success(currentData)
                }
                
                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    if (error != null) {
                        Log.e(TAG, "Error incrementing play count: ${error.message}")
                    }
                }
            })
    }
}