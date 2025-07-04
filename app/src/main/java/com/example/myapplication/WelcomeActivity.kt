package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    private lateinit var viewFlipper: ViewFlipper
    private lateinit var getStartedButton: Button
    private lateinit var loginButton: Button
    private lateinit var skipButton: TextView
    private lateinit var dots: Array<View>
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        
        auth = FirebaseAuth.getInstance()
        
        // Check if user is already signed in
        if (auth.currentUser != null) {
            navigateToMainActivity()
            return
        }
        
        initializeViews()
        setupListeners()
    }
    
    private fun initializeViews() {
        viewFlipper = findViewById(R.id.viewFlipper)
        getStartedButton = findViewById(R.id.getStartedButton)
        loginButton = findViewById(R.id.loginButton)
        skipButton = findViewById(R.id.skipButton)
        
        // Initialize indicator dots
        dots = arrayOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3)
        )
        
        // Set initial active dot
        updateDots(0)
        
        // Set animation for flipper
        viewFlipper.inAnimation = android.view.animation.AnimationUtils.loadAnimation(
            this, R.anim.slide_in_right
        )
        viewFlipper.outAnimation = android.view.animation.AnimationUtils.loadAnimation(
            this, R.anim.slide_out_left
        )
    }
    
    private fun setupListeners() {
        // Next button on each welcome screen
        findViewById<ImageView>(R.id.nextButton1).setOnClickListener {
            viewFlipper.showNext()
            updateDots(1)
        }
        
        findViewById<ImageView>(R.id.nextButton2).setOnClickListener {
            viewFlipper.showNext()
            updateDots(2)
        }
        
        // Get started button
        getStartedButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        
        // Login button
        loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        
        // Skip button
        skipButton.setOnClickListener {
            // Skip to main activity as guest
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("isGuest", true)
            startActivity(intent)
            finish()
        }
    }
    
    private fun updateDots(activePosition: Int) {
        for (i in dots.indices) {
            dots[i].setBackgroundResource(
                if (i == activePosition) R.drawable.dot_active
                else R.drawable.dot_inactive
            )
        }
    }
    
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}