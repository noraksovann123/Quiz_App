package com.example.myapplication

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*
import java.text.SimpleDateFormat

class CreateAccountActivity : AppCompatActivity() {

    // Page 1 Views
    private lateinit var fullNameInput: EditText
    private lateinit var dobInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var countrySpinner: Spinner
    private lateinit var ageSpinner: Spinner
    private lateinit var continueButton: Button
    private lateinit var calendarIcon: ImageView
    private lateinit var backArrow: ImageView
    private lateinit var progressView: FrameLayout

    // Page 2 Views
    private lateinit var usernameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var rememberMeCheckbox: CheckBox
    private lateinit var signUpButton: Button
    private lateinit var eyeIconPassword: ImageView
    private lateinit var eyeIconConfirm: ImageView

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var loginFirebase: LoginFirebase

    private var currentPage = 1
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    // User data storage
    private var userData = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        loginFirebase = LoginFirebase()
        loginFirebase.init(this, getString(R.string.web_client_id))

        // Show page 1 initially
        showPage1()
    }

    private fun showPage1() {
        setContentView(R.layout.create_account_page_1)
        currentPage = 1
        
        initializePage1Views()
        setupPage1Listeners()
        setupSpinners()
    }

    private fun showPage2() {
        setContentView(R.layout.create_account_page_2)
        currentPage = 2
        
        initializePage2Views()
        setupPage2Listeners()
    }

    private fun initializePage1Views() {
        fullNameInput = findViewById(R.id.fullNameInput)
        dobInput = findViewById(R.id.dobInput)
        phoneInput = findViewById(R.id.phoneInput)
        countrySpinner = findViewById(R.id.countrySpinner)
        ageSpinner = findViewById(R.id.ageSpinner)
        continueButton = findViewById(R.id.continueButton)
        calendarIcon = findViewById(R.id.calendarIcon)
        backArrow = findViewById(R.id.backArrow)
        progressView = findViewById(R.id.progressView)
    }

    private fun initializePage2Views() {
        usernameInput = findViewById(R.id.usernameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        rememberMeCheckbox = findViewById(R.id.rememberMeCheckbox)
        signUpButton = findViewById(R.id.signUpButton)
        eyeIconPassword = findViewById(R.id.eyeIconPassword)
        eyeIconConfirm = findViewById(R.id.eyeIconConfirm)
        progressView = findViewById(R.id.progressView)
        backArrow = findViewById(R.id.backArrow)
    }

    private fun setupPage1Listeners() {
        continueButton.setOnClickListener {
            if (validatePage1()) {
                saveUserDataFromPage1()
                showPage2()
            }
        }

        backArrow.setOnClickListener {
            finish()
        }

        dobInput.setOnClickListener {
            showDatePicker()
        }

        calendarIcon.setOnClickListener {
            showDatePicker()
        }
    }

    private fun setupPage2Listeners() {
        signUpButton.setOnClickListener {
            if (validatePage2()) {
                saveUserDataFromPage2()
                createAccount()
            }
        }

        backArrow.setOnClickListener {
            showPage1()
        }

        eyeIconPassword.setOnClickListener {
            togglePasswordVisibility(passwordInput, eyeIconPassword, isPasswordVisible) { visible ->
                isPasswordVisible = visible
            }
        }

        eyeIconConfirm.setOnClickListener {
            togglePasswordVisibility(confirmPasswordInput, eyeIconConfirm, isConfirmPasswordVisible) { visible ->
                isConfirmPasswordVisible = visible
            }
        }
    }

    private fun setupSpinners() {
        // Country Spinner
        val countries = resources.getStringArray(R.array.countries)
        val countryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countries)
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        countrySpinner.adapter = countryAdapter

        // Age Spinner
        val ages = (13..100).map { it.toString() }.toTypedArray()
        val ageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ages)
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ageSpinner.adapter = ageAdapter
    }

    private fun validatePage1(): Boolean {
        val fullName = fullNameInput.text.toString().trim()
        val dob = dobInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()

        if (fullName.isEmpty()) {
            fullNameInput.error = "Full name is required"
            return false
        }

        if (dob.isEmpty()) {
            dobInput.error = "Date of birth is required"
            return false
        }

        if (phone.isEmpty()) {
            phoneInput.error = "Phone number is required"
            return false
        }

        return true
    }

    private fun validatePage2(): Boolean {
        val username = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val confirmPassword = confirmPasswordInput.text.toString().trim()

        if (username.isEmpty()) {
            usernameInput.error = "Username is required"
            return false
        }

        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Please enter a valid email"
            return false
        }

        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            return false
        }

        if (password.length < 6) {
            passwordInput.error = "Password must be at least 6 characters"
            return false
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.error = "Please confirm your password"
            return false
        }

        if (password != confirmPassword) {
            confirmPasswordInput.error = "Passwords do not match"
            return false
        }

        return true
    }

    private fun saveUserDataFromPage1() {
        userData["fullName"] = fullNameInput.text.toString().trim()
        userData["dateOfBirth"] = dobInput.text.toString().trim()
        userData["phoneNumber"] = phoneInput.text.toString().trim()
        userData["country"] = countrySpinner.selectedItem.toString()
        userData["age"] = ageSpinner.selectedItem.toString()
    }

    private fun saveUserDataFromPage2() {
        userData["username"] = usernameInput.text.toString().trim()
        userData["email"] = emailInput.text.toString().trim()
        userData["password"] = passwordInput.text.toString().trim()
    }

    private fun createAccount() {
        showProgress(true)

        val email = userData["email"]!!
        val password = userData["password"]!!
        val fullName = userData["fullName"]!!

        loginFirebase.createAccountWithEmail(email, password, fullName) { success, message ->
            if (success) {
                saveUserToDatabase()
            } else {
                showProgress(false)
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveUserToDatabase() {
        val currentUser = auth.currentUser ?: return

        val userMap = hashMapOf(
            "id" to currentUser.uid,
            "username" to userData["username"]!!,
            "fullName" to userData["fullName"]!!,
            "email" to userData["email"]!!,
            "photoUrl" to "",
            "dateOfBirth" to userData["dateOfBirth"]!!,
            "phoneNumber" to userData["phoneNumber"]!!,
            "country" to userData["country"]!!,
            "age" to userData["age"]!!,
            "createdAt" to System.currentTimeMillis(),
            "quizCount" to 0,
            "followersCount" to 0,
            "followingCount" to 0,
            "isAuthor" to true // Mark as author since they created an account
        )

        database.reference.child("users").child(currentUser.uid)
            .setValue(userMap)
            .addOnSuccessListener {
                showProgress(false)
                showSuccessPage()
            }
            .addOnFailureListener { e ->
                showProgress(false)
                Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showSuccessPage() {
        setContentView(R.layout.create_account_successful)
        
        // Auto-navigate to MainActivity after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToMainActivity()
        }, 2000)
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dobInput.setText(dateFormat.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun togglePasswordVisibility(editText: EditText, imageView: ImageView, isVisible: Boolean, callback: (Boolean) -> Unit) {
        val newVisibility = !isVisible
        
        if (newVisibility) {
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            imageView.setImageResource(R.drawable.ic_eye_closed)
        } else {
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            imageView.setImageResource(R.drawable.ic_eye_open)
        }
        
        editText.setSelection(editText.text.length)
        callback(newVisibility)
    }

    private fun showProgress(show: Boolean) {
        progressView.visibility = if (show) View.VISIBLE else View.GONE
        
        when (currentPage) {
            1 -> continueButton.isEnabled = !show
            2 -> signUpButton.isEnabled = !show
        }
    }

    override fun onBackPressed() {
        if (currentPage == 2) {
            showPage1()
        } else {
            super.onBackPressed()
        }
    }
}