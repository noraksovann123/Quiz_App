package com.example.myapplication

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*
import java.text.SimpleDateFormat

class CreateAccountActivity : AppCompatActivity() {

    // Step 1 Views
    private lateinit var backArrow: ImageView
    private lateinit var fullNameInput: EditText
    private lateinit var dobInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var countrySpinner: Spinner
    private lateinit var ageSpinner: Spinner
    private lateinit var continueButton: Button
    private lateinit var calendarIcon: ImageView
    private lateinit var progressView: View

    // Step 2 Views
    private lateinit var usernameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var eyeIconPassword: ImageView
    private lateinit var eyeIconConfirm: ImageView
    private lateinit var rememberMeCheckbox: CheckBox
    private lateinit var signUpButton: Button
    private lateinit var googleButton: Button

    // Variables
    private var currentStep = 1
    private lateinit var loginFirebase: LoginFirebase
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_account_page_1)

        // Initialize Firebase
        loginFirebase = LoginFirebase()
        loginFirebase.init(this, getString(R.string.web_client_id))

        // Initialize views for step 1
        initializeStepOneViews()
    }

    private fun initializeStepOneViews() {
        backArrow = findViewById(R.id.backArrow)
        fullNameInput = findViewById(R.id.fullNameInput)
        dobInput = findViewById(R.id.dobInput)
        phoneInput = findViewById(R.id.phoneInput)
        countrySpinner = findViewById(R.id.countrySpinner)
        ageSpinner = findViewById(R.id.ageSpinner)
        continueButton = findViewById(R.id.continueButton)
        calendarIcon = findViewById(R.id.calendarIcon)
        progressView = findViewById(R.id.progressView)

        // Setup spinners
        setupSpinners()

        // Setup click listeners for step 1
        setupStepOneListeners()
    }

    private fun setupSpinners() {
        // Setup country spinner
        val countries = resources.getStringArray(R.array.countries_array)
        val countryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countries)
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        countrySpinner.adapter = countryAdapter

        // Setup age spinner
        val ages = Array(100) { (it + 1).toString() }
        val ageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ages)
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ageSpinner.adapter = ageAdapter

        // Default to 18
        ageSpinner.setSelection(17)
    }

    private fun setupStepOneListeners() {
        backArrow.setOnClickListener {
            onBackPressed()
        }

        calendarIcon.setOnClickListener {
            showDatePickerDialog()
        }

        dobInput.setOnClickListener {
            showDatePickerDialog()
        }

        continueButton.setOnClickListener {
            if (validateStepOne()) {
                goToStepTwo()
            }
        }
    }

    private fun showDatePickerDialog() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(Calendar.YEAR, selectedYear)
            calendar.set(Calendar.MONTH, selectedMonth)
            calendar.set(Calendar.DAY_OF_MONTH, selectedDay)
            dobInput.setText(dateFormat.format(calendar.time))
        }, year, month, day).show()
    }

    private fun validateStepOne(): Boolean {
        if (fullNameInput.text.toString().trim().isEmpty()) {
            fullNameInput.error = "Please enter your full name"
            fullNameInput.requestFocus()
            return false
        }

        if (dobInput.text.toString().trim().isEmpty()) {
            dobInput.error = "Please enter your date of birth"
            dobInput.requestFocus()
            return false
        }

        if (phoneInput.text.toString().trim().isEmpty()) {
            phoneInput.error = "Please enter your phone number"
            phoneInput.requestFocus()
            return false
        }

        return true
    }

    private fun goToStepTwo() {
        // Save step 1 data to shared preferences
        saveStepOneData()

        // Switch to step 2
        currentStep = 2
        setContentView(R.layout.create_account_page_2)
        initializeStepTwoViews()
    }

    private fun saveStepOneData() {
        val sharedPrefs = getSharedPreferences("registration_data", MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString("full_name", fullNameInput.text.toString().trim())
            putString("dob", dobInput.text.toString().trim())
            putString("phone", phoneInput.text.toString().trim())
            putString("country", countrySpinner.selectedItem.toString())
            putString("age", ageSpinner.selectedItem.toString())
            apply()
        }
    }

    private fun initializeStepTwoViews() {
        backArrow = findViewById(R.id.backArrow)
        usernameInput = findViewById(R.id.usernameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        eyeIconPassword = findViewById(R.id.eyeIconPassword)
        eyeIconConfirm = findViewById(R.id.eyeIconConfirm)
        rememberMeCheckbox = findViewById(R.id.rememberMeCheckbox)
        signUpButton = findViewById(R.id.signUpButton)
        googleButton = findViewById(R.id.googleButton)
        progressView = findViewById(R.id.progressView)

        // Setup click listeners for step 2
        setupStepTwoListeners()
    }

    private fun setupStepTwoListeners() {
        backArrow.setOnClickListener {
            // Go back to step 1
            currentStep = 1
            setContentView(R.layout.create_account_page_1)
            initializeStepOneViews()

            // Restore saved values
            restoreStepOneValues()
        }

        eyeIconPassword.setOnClickListener {
            togglePasswordVisibility(passwordInput, eyeIconPassword, isPasswordVisible)
            isPasswordVisible = !isPasswordVisible
        }

        eyeIconConfirm.setOnClickListener {
            togglePasswordVisibility(confirmPasswordInput, eyeIconConfirm, isConfirmPasswordVisible)
            isConfirmPasswordVisible = !isConfirmPasswordVisible
        }

        googleButton.setOnClickListener {
            signInWithGoogle()
        }

        signUpButton.setOnClickListener {
            if (validateStepTwo()) {
                createAccount()
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

    private fun restoreStepOneValues() {
        val sharedPrefs = getSharedPreferences("registration_data", MODE_PRIVATE)
        
        fullNameInput.setText(sharedPrefs.getString("full_name", ""))
        dobInput.setText(sharedPrefs.getString("dob", ""))
        phoneInput.setText(sharedPrefs.getString("phone", ""))
        
        // Set country spinner
        val country = sharedPrefs.getString("country", "")
        val countries = resources.getStringArray(R.array.countries_array)
        val countryIndex = countries.indexOf(country)
        if (countryIndex >= 0) {
            countrySpinner.setSelection(countryIndex)
        }
        
        // Set age spinner
        val age = sharedPrefs.getString("age", "18")
        val ageIndex = age?.toIntOrNull()?.minus(1) ?: 17
        if (ageIndex in 0..99) {
            ageSpinner.setSelection(ageIndex)
        }
    }

    private fun validateStepTwo(): Boolean {
        val username = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()

        if (username.isEmpty()) {
            usernameInput.error = "Please enter a username"
            usernameInput.requestFocus()
            return false
        }

        if (email.isEmpty()) {
            emailInput.error = "Please enter your email"
            emailInput.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Please enter a valid email"
            emailInput.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            passwordInput.error = "Please enter a password"
            passwordInput.requestFocus()
            return false
        }

        if (password.length < 6) {
            passwordInput.error = "Password should be at least 6 characters"
            passwordInput.requestFocus()
            return false
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.error = "Please confirm your password"
            confirmPasswordInput.requestFocus()
            return false
        }

        if (password != confirmPassword) {
            confirmPasswordInput.error = "Passwords do not match"
            confirmPasswordInput.requestFocus()
            return false
        }

        return true
    }

    private fun createAccount() {
        showProgress(true)
        
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val username = usernameInput.text.toString().trim()
        
        loginFirebase.createAccountWithEmail(email, password, username) { success, message ->
            runOnUiThread {
                if (success) {
                    // Update user profile with additional information
                    updateUserProfile()
                } else {
                    showProgress(false)
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateUserProfile() {
        val sharedPrefs = getSharedPreferences("registration_data", MODE_PRIVATE)
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            showProgress(false)
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_LONG).show()
            return
        }
        
        val userId = user.uid
        val username = usernameInput.text.toString().trim()
        val fullName = sharedPrefs.getString("full_name", "")
        val dob = sharedPrefs.getString("dob", "")
        val phone = sharedPrefs.getString("phone", "")
        val country = sharedPrefs.getString("country", "")
        val age = sharedPrefs.getString("age", "")
        
        // Create user data map
        val userData = hashMapOf(
            "id" to userId,
            "username" to username,
            "fullName" to (fullName ?: ""),
            "email" to (user.email ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "dateOfBirth" to (dob ?: ""),
            "phoneNumber" to (phone ?: ""),
            "country" to (country ?: ""),
            "age" to (age ?: ""),
            "createdAt" to System.currentTimeMillis(),
            "quizCount" to 0,
            "followersCount" to 0,
            "followingCount" to 0
        )
        
        // Save to Firebase Database
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(userId)
            .setValue(userData)
            .addOnCompleteListener { task ->
                showProgress(false)
                
                if (task.isSuccessful) {
                    // If Remember Me is checked, save login state
                    if (rememberMeCheckbox.isChecked) {
                        val loginPrefs = getSharedPreferences("quizzo_prefs", MODE_PRIVATE)
                        with(loginPrefs.edit()) {
                            putBoolean("is_logged_in", true)
                            putString("user_email", user.email)
                            apply()
                        }
                    }
                    
                    // Show success screen
                    showSuccessScreen()
                } else {
                    Toast.makeText(this, "Failed to update profile: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showSuccessScreen() {
        setContentView(R.layout.create_account_successful)
        
        // Automatically navigate to main activity after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }, 2000)
    }

    private fun signInWithGoogle() {
        showProgress(true)
        loginFirebase.signInWithGoogle(this)
    }

    private fun showProgress(show: Boolean) {
        progressView.visibility = if (show) View.VISIBLE else View.GONE
        
        if (currentStep == 1) {
            continueButton.isEnabled = !show
        } else {
            signUpButton.isEnabled = !show
            googleButton.isEnabled = !show
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == LoginFirebase.RC_SIGN_IN) {
            showProgress(false)
            
            loginFirebase.handleGoogleSignInResult(data) { success, message ->
                if (success) {
                    // Google Sign-In successful, check if user exists
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        // Check if this is a new user
                        FirebaseDatabase.getInstance().reference
                            .child("users")
                            .child(user.uid)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                if (snapshot.exists()) {
                                    // Existing user, go to main activity
                                    navigateToMainActivity()
                                } else {
                                    // New user, add profile data
                                    updateGoogleUserProfile(user.uid)
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error checking user profile", Toast.LENGTH_LONG).show()
                            }
                    }
                } else {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateGoogleUserProfile(userId: String) {
        // Get step 1 data if available
        val sharedPrefs = getSharedPreferences("registration_data", MODE_PRIVATE)
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
        val userData = hashMapOf(
            "id" to userId,
            "username" to (user.displayName ?: "User${System.currentTimeMillis()}"),
            "fullName" to (sharedPrefs.getString("full_name", user.displayName) ?: ""),
            "email" to (user.email ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "dateOfBirth" to (sharedPrefs.getString("dob", "") ?: ""),
            "phoneNumber" to (sharedPrefs.getString("phone", "") ?: ""),
            "country" to (sharedPrefs.getString("country", "") ?: ""),
            "age" to (sharedPrefs.getString("age", "") ?: ""),
            "createdAt" to System.currentTimeMillis(),
            "quizCount" to 0,
            "followersCount" to 0,
            "followingCount" to 0
        )
        
        // Save to Firebase Database
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(userId)
            .setValue(userData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Save login state
                    val loginPrefs = getSharedPreferences("quizzo_prefs", MODE_PRIVATE)
                    with(loginPrefs.edit()) {
                        putBoolean("is_logged_in", true)
                        putString("user_email", user.email)
                        apply()
                    }
                    
                    // Show success screen
                    showSuccessScreen()
                } else {
                    Toast.makeText(this, "Failed to update profile: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    navigateToMainActivity()
                }
            }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}