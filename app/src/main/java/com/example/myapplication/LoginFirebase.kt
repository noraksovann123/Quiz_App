package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class LoginFirebase {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        const val RC_SIGN_IN = 9001
        private const val TAG = "LoginFirebase"
    }

    fun init(context: Context, webClientId: String) {
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun createAccountWithEmail(email: String, password: String, name: String, callback: (Boolean, String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Update user profile with name
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                // Create user entry in the database
                                saveUserToDatabase(user.uid, name, email)
                                callback(true, "Account created successfully")
                            } else {
                                callback(true, "Account created but profile could not be updated")
                            }
                        }
                } else {
                    // If registration fails, display a message to the user.
                    callback(false, task.exception?.message ?: "Registration failed")
                }
            }
    }

    private fun saveUserToDatabase(userId: String, name: String, email: String) {
        val database = FirebaseDatabase.getInstance().reference
        val userValues = hashMapOf(
            "id" to userId,
            "username" to name,
            "fullName" to name,
            "email" to email,
            "photoUrl" to "",
            "createdAt" to System.currentTimeMillis()
        )

        database.child("users").child(userId).setValue(userValues)
    }

    fun signInWithEmail(email: String, password: String, callback: (Boolean, String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    callback(true, "Login successful")
                } else {
                    callback(false, task.exception?.message ?: "Login failed")
                }
            }
    }

    fun signInWithGoogle(activity: Activity) {
        val signInIntent = googleSignInClient.signInIntent
        activity.startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    fun handleGoogleSignInResult(data: Intent?, callback: (Boolean, String) -> Unit) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account, callback)
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign in failed", e)
            callback(false, "Google sign in failed: ${e.statusCode}")
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount, callback: (Boolean, String) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Save user data to database if new user
                    val user = auth.currentUser
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false

                    if (isNewUser && user != null) {
                        // Create a new user entry in the database
                        val database = FirebaseDatabase.getInstance().reference
                        val userValues = hashMapOf(
                            "id" to user.uid,
                            "username" to (user.displayName ?: ""),
                            "fullName" to (user.displayName ?: ""),
                            "email" to (user.email ?: ""),
                            "photoUrl" to (user.photoUrl?.toString() ?: ""),
                            "createdAt" to System.currentTimeMillis()
                        )

                        database.child("users").child(user.uid).setValue(userValues)
                    }

                    callback(true, "Google sign in successful")
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    callback(false, task.exception?.message ?: "Authentication failed")
                }
            }
    }
}