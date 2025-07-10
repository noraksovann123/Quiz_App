package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    
    private val SPLASH_DELAY = 2000L // 2 seconds
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Find the logo ImageView
        val logoImageView = findViewById<ImageView>(R.id.appLogo)
        
        // Create fade-in animation
        val fadeIn = AlphaAnimation(0.0f, 1.0f)
        fadeIn.duration = 1000
        
        // Start the animation
        logoImageView.startAnimation(fadeIn)
        
        // Navigate to WelcomeActivity after delay
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this@SplashActivity, WelcomeActivity::class.java)
            startActivity(intent)
            finish() // Close SplashActivity so it's removed from the back stack
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, SPLASH_DELAY)
    }
}
