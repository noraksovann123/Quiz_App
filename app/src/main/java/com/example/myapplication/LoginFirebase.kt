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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class LoginFirebase {
    private val TAG = "LoginFirebase"
    
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    
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
    
    fun createAccountWithEmail(email: String, password: String, username: String, callback: (Boolean, String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update user profile
                    val user = auth.currentUser
                    if (user != null) {
                        // Update display name
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .build()
                        
                        user.updateProfile(profileUpdates)
                            .addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    callback(true, "Account created successfully")
                                } else {
                                    Log.w(TAG, "updateProfile:failure", profileTask.exception)
                                    callback(false, "Failed to set display name: ${profileTask.exception?.message ?: "Unknown error"}")
                                }
                            }
                    } else {
                        callback(false, "User is null after registration")
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    callback(false, "Registration failed: ${task.exception?.message ?: "Unknown error"}")
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
            // Google Sign In failed
            Log.w(TAG, "Google sign in failed", e)
            callback(false, "Google sign in failed: ${e.message ?: "Unknown error"}")
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error during Google sign in", e)
            callback(false, "Error during Google sign in: ${e.message ?: "Unknown error"}")
        }
    }
    
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount, callback: (Boolean, String) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    val user = auth.currentUser
                    if (user != null) {
                        callback(true, "Successfully signed in as ${user.displayName}")
                    } else {
                        callback(false, "User is null after sign in")
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    callback(false, "Authentication failed: ${task.exception?.message ?: "Unknown error"}")
                }
            }
    }
    
    companion object {
        const val RC_SIGN_IN = 9001
    }
}