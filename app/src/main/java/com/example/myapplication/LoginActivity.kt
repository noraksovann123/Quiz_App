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
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var loginFirebase: LoginFirebase
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signInButton: Button
    private lateinit var forgotPasswordText: TextView
    private lateinit var rememberMeCheckbox: CheckBox
    private lateinit var backButton: ImageView
    private lateinit var togglePasswordVisibility: ImageView
    private lateinit var createAccountText: TextView
    private lateinit var googleSignInButton: Button
    private lateinit var progressView: View

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_form)

        // Check if user is already logged in
        checkLoggedInUser()

        // Initialize Firebase Auth
        loginFirebase = LoginFirebase()
        loginFirebase.init(this, getString(R.string.web_client_id))

        // Initialize views
        initializeViews()
        
        // Set click listeners
        setupClickListeners()
    }

    private fun checkLoggedInUser() {
        val sharedPrefs = getSharedPreferences("quizzo_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPrefs.getBoolean("is_logged_in", false)
        
        if (isLoggedIn) {
            // Check if Firebase session is still valid
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                navigateToMainActivity()
            }
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
        progressView = findViewById(R.id.progressView)
    }

    private fun setupClickListeners() {
        signInButton.setOnClickListener {
            login()
        }

        forgotPasswordText.setOnClickListener {
            navigateToForgotPassword()
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
            runOnUiThread {
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
            val sharedPrefs = getSharedPreferences("quizzo_prefs", MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                putBoolean("is_logged_in", true)
                putString("user_email", email)
                apply()
            }
        }
    }

    private fun signInWithGoogle() {
        showProgress(true)
        loginFirebase.signInWithGoogle(this)
    }

    private fun navigateToForgotPassword() {
        val intent = Intent(this, ForgotPasswordActivity::class.java)
        startActivity(intent)
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

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        
        if (isPasswordVisible) {
            // Show password
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            togglePasswordVisibility.setImageResource(R.drawable.ic_eye_closed)
        } else {
            // Hide password
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
            togglePasswordVisibility.setImageResource(R.drawable.ic_eye_open)
        }
        
        // Move cursor to the end of text
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    private fun showProgress(show: Boolean) {
        progressView.visibility = if (show) View.VISIBLE else View.GONE
        signInButton.isEnabled = !show
        googleSignInButton.isEnabled = !show
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handle the Google Sign-In result
        if (requestCode == LoginFirebase.RC_SIGN_IN) {
            showProgress(false)
            loginFirebase.handleGoogleSignInResult(data) { success, message ->
                if (success) {
                    // Save login state
                    val account = GoogleSignIn.getLastSignedInAccount(this)
                    account?.email?.let { email ->
                        saveLoginState(email)
                    }
                    
                    navigateToMainActivity()
                } else {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}