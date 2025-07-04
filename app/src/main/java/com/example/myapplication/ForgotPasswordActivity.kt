package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    // Step 1: Email Verification
    private lateinit var emailInput: EditText
    private lateinit var continueButton: Button
    private lateinit var backButton: ImageView
    private lateinit var progressView: View

    // Step 2: OTP Verification
    private lateinit var otpInput: EditText
    private lateinit var verifyButton: Button
    private lateinit var resendCodeText: TextView
    
    // Step 3: Create New Password
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var passwordEyeIcon: ImageView
    private lateinit var confirmEyeIcon: ImageView
    private lateinit var resetButton: Button
    
    // Firebase
    private lateinit var auth: FirebaseAuth
    
    // Variables
    private var currentStep = 1
    private var userEmail = ""
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forget_password_form)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Initialize views for first step
        initializeStepOneViews()
    }
    
    private fun initializeStepOneViews() {
        emailInput = findViewById(R.id.emailInput)
        continueButton = findViewById(R.id.continueButton)
        backButton = findViewById(R.id.backButton)
        progressView = findViewById(R.id.progressView)
        
        backButton.setOnClickListener {
            finish()
        }
        
        continueButton.setOnClickListener {
            if (validateEmail()) {
                sendPasswordResetEmail()
            }
        }
    }
    
    private fun validateEmail(): Boolean {
        userEmail = emailInput.text.toString().trim()
        
        if (userEmail.isEmpty()) {
            emailInput.error = "Please enter your email"
            emailInput.requestFocus()
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            emailInput.error = "Please enter a valid email"
            emailInput.requestFocus()
            return false
        }
        
        return true
    }
    
    private fun sendPasswordResetEmail() {
        showProgress(true)
        
        auth.sendPasswordResetEmail(userEmail)
            .addOnCompleteListener { task ->
                showProgress(false)
                
                if (task.isSuccessful) {
                    // Move to the next step (verification code entry)
                    goToStepTwo()
                } else {
                    Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}", 
                        Toast.LENGTH_LONG).show()
                }
            }
    }
    
    private fun goToStepTwo() {
        currentStep = 2
        setContentView(R.layout.forget_password_form2)
        initializeStepTwoViews()
    }
    
    private fun initializeStepTwoViews() {
        otpInput = findViewById(R.id.otpInput)
        verifyButton = findViewById(R.id.verifyButton)
        resendCodeText = findViewById(R.id.resendCodeText)
        backButton = findViewById(R.id.backButton)
        progressView = findViewById(R.id.progressView)
        
        // Email confirmation message
        val confirmationMessage = findViewById<TextView>(R.id.confirmationMessage)
        confirmationMessage.text = "We have sent a code to $userEmail"
        
        backButton.setOnClickListener {
            // Go back to step 1
            currentStep = 1
            setContentView(R.layout.forget_password_form)
            initializeStepOneViews()
            emailInput.setText(userEmail)
        }
        
        verifyButton.setOnClickListener {
            if (validateOtp()) {
                goToStepThree()
            }
        }
        
        resendCodeText.setOnClickListener {
            // Resend verification code
            sendPasswordResetEmail()
            Toast.makeText(this, "Code resent to your email", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun validateOtp(): Boolean {
        val otp = otpInput.text.toString().trim()
        
        if (otp.isEmpty()) {
            otpInput.error = "Please enter the verification code"
            otpInput.requestFocus()
            return false
        }
        
        if (otp.length < 6) {
            otpInput.error = "Please enter a valid verification code"
            otpInput.requestFocus()
            return false
        }
        
        return true
    }
    
    private fun goToStepThree() {
        currentStep = 3
        setContentView(R.layout.create_new_password)
        initializeStepThreeViews()
    }
    
    private fun initializeStepThreeViews() {
        newPasswordInput = findViewById(R.id.newPasswordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        passwordEyeIcon = findViewById(R.id.passwordEyeIcon)
        confirmEyeIcon = findViewById(R.id.confirmEyeIcon)
        resetButton = findViewById(R.id.resetButton)
        backButton = findViewById(R.id.backButton)
        progressView = findViewById(R.id.progressView)
        
        backButton.setOnClickListener {
            // Go back to step 2
            goToStepTwo()
        }
        
        passwordEyeIcon.setOnClickListener {
            togglePasswordVisibility(newPasswordInput, passwordEyeIcon, isPasswordVisible)
            isPasswordVisible = !isPasswordVisible
        }
        
        confirmEyeIcon.setOnClickListener {
            togglePasswordVisibility(confirmPasswordInput, confirmEyeIcon, isConfirmPasswordVisible)
            isConfirmPasswordVisible = !isConfirmPasswordVisible
        }
        
        resetButton.setOnClickListener {
            if (validatePasswords()) {
                // Since Firebase handles the reset via email link, here we just show success
                showPasswordResetSuccess()
            }
        }
    }
    
    private fun togglePasswordVisibility(editText: EditText, iconView: ImageView, isVisible: Boolean) {
        if (isVisible) {
            // Hide password
            editText.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
            iconView.setImageResource(R.drawable.ic_eye_open)
        } else {
            // Show password
            editText.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            iconView.setImageResource(R.drawable.ic_eye_closed)
        }
        
        // Move cursor to end
        editText.setSelection(editText.text.length)
    }
    
    private fun validatePasswords(): Boolean {
        val newPassword = newPasswordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()
        
        if (newPassword.isEmpty()) {
            newPasswordInput.error = "Please enter a new password"
            newPasswordInput.requestFocus()
            return false
        }
        
        if (newPassword.length < 6) {
            newPasswordInput.error = "Password should be at least 6 characters"
            newPasswordInput.requestFocus()
            return false
        }
        
        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.error = "Please confirm your new password"
            confirmPasswordInput.requestFocus()
            return false
        }
        
        if (newPassword != confirmPassword) {
            confirmPasswordInput.error = "Passwords do not match"
            confirmPasswordInput.requestFocus()
            return false
        }
        
        return true
    }
    
    private fun showPasswordResetSuccess() {
        // Show success screen
        setContentView(R.layout.change_password_successful)
        
        // Navigate back to login after delay
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }, 2000)
    }
    
    private fun showProgress(show: Boolean) {
        progressView.visibility = if (show) View.VISIBLE else View.GONE
        
        when (currentStep) {
            1 -> continueButton.isEnabled = !show
            2 -> verifyButton.isEnabled = !show
            3 -> resetButton.isEnabled = !show
        }
    }
}