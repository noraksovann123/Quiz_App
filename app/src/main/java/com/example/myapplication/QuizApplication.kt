package com.example.myapplication

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class QuizApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Enable Firebase offline capabilities
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}