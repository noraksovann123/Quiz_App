package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signInButton: Button
    private lateinit var forgotPasswordText: TextView
    private lateinit var rememberMeCheckbox: CheckBox
    private lateinit var backButton: ImageView
    private lateinit var togglePasswordVisibility: ImageView
    private lateinit var createAccountText: TextView
    private lateinit var googleSignInButton: Button
    private lateinit var continueAsGuestButton: Button
    private lateinit var progressView: View
    
    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var loginFirebase: LoginFirebase

    private var isPasswordVisible = false
    
    // Replace deprecated startActivityForResult with ActivityResultLauncher
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_form)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        loginFirebase = LoginFirebase()
        loginFirebase.init(this, getString(R.string.web_client_id))

        // Initialize the ActivityResultLauncher
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleGoogleSignInResult(task)
            } else {
                // Google Sign In failed
                Toast.makeText(this, "Google sign in failed or was canceled", Toast.LENGTH_SHORT).show()
                showProgress(false)
            }
        }

        // Check if user is already logged in
        checkLoggedInUser()
        
        // Configure Google Sign-in
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize views
        initializeViews()
        
        // Set click listeners
        setupClickListeners()
    }

    private fun checkLoggedInUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToMainActivity()
        }
    }

    private fun initializeViews() {
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        signInButton = findViewById(R.id.signInButton)
        forgotPasswordText = findViewById(R.id.forgotPasswordText)
        rememberMeCheckbox = findViewById(R.id.rememberMe)
        backButton = findViewById(R.id.backButton)
        togglePasswordVisibility = findViewById(R.id.eyeIcon)
        createAccountText = findViewById(R.id.createAccountText)
        googleSignInButton = findViewById(R.id.googleSignInButton)
        continueAsGuestButton = findViewById(R.id.continueAsGuestButton)
        progressView = findViewById(R.id.progressView)
    }

    private fun setupClickListeners() {
        signInButton.setOnClickListener {
            login()
        }

        forgotPasswordText.setOnClickListener {
            // Show message instead of navigating
            Toast.makeText(this, "Forgot password feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        backButton.setOnClickListener {
            finish()
        }

        togglePasswordVisibility.setOnClickListener {
            togglePasswordVisibility()
        }

        createAccountText.setOnClickListener {
            navigateToCreateAccount()
        }

        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        continueAsGuestButton.setOnClickListener {
            navigateToMainActivityAsGuest()
        }
    }

    private fun login() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        // Validate input
        if (!validateLoginInput(email, password)) {
            return
        }

        // Show progress
        showProgress(true)

        loginFirebase.signInWithEmail(email, password) { success, message ->
            showProgress(false)
            
            if (success) {
                // Save login state if "Remember Me" is checked
                saveLoginState(email)
                
                // Navigate to MainActivity
                navigateToMainActivity()
            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validateLoginInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            emailEditText.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            passwordEditText.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Please enter a valid email"
            emailEditText.requestFocus()
            return false
        }

        return true
    }

    private fun saveLoginState(email: String) {
        if (rememberMeCheckbox.isChecked) {
            val sharedPrefs = getSharedPreferences("quiz_prefs", MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                putBoolean("is_logged_in", true)
                putString("user_email", email)
                apply()
            }
        }
    }

    private fun signInWithGoogle() {
        showProgress(true)
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun navigateToCreateAccount() {
        val intent = Intent(this, CreateAccountActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToMainActivityAsGuest() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("isGuest", true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        
        if (isPasswordVisible) {
            // Show password
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            togglePasswordVisibility.setImageResource(R.drawable.ic_eye_closed)
        } else {
            // Hide password
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            togglePasswordVisibility.setImageResource(R.drawable.ic_eye_open)
        }
        
        // Move cursor to the end of text
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    private fun showProgress(show: Boolean) {
        progressView.visibility = if (show) View.VISIBLE else View.GONE
        signInButton.isEnabled = !show
        googleSignInButton.isEnabled = !show
        continueAsGuestButton.isEnabled = !show
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Google Sign In was successful, authenticate with Firebase
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            // Google Sign In failed
            showProgress(false)
            Toast.makeText(this, "Google sign in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showProgress(false)
                
                if (task.isSuccessful) {
                    // Save login state
                    account.email?.let { saveLoginState(it) }
                    navigateToMainActivity()
                } else {
                    // If sign in fails, display a message to the user
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
}