package com.example.myapplication

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditProfileActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var fullNameInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var dobInput: EditText
    private lateinit var countrySpinner: Spinner
    private lateinit var ageSpinner: Spinner
    private lateinit var calendarIcon: ImageView
    private lateinit var saveButton: Button
    private lateinit var progressView: View

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize views
        initializeViews()

        // Setup spinners
        setupSpinners()

        // Setup listeners
        setupListeners()

        // Load current user data
        loadUserData()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        fullNameInput = findViewById(R.id.fullNameInput)
        usernameInput = findViewById(R.id.usernameInput)
        phoneInput = findViewById(R.id.phoneInput)
        dobInput = findViewById(R.id.dobInput)
        countrySpinner = findViewById(R.id.countrySpinner)
        ageSpinner = findViewById(R.id.ageSpinner)
        calendarIcon = findViewById(R.id.calendarIcon)
        saveButton = findViewById(R.id.saveButton)
        progressView = findViewById(R.id.progressView)
    }

    private fun setupSpinners() {
        // Setup country spinner
        val countries = resources.getStringArray(R.array.countries)
        val countryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countries)
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        countrySpinner.adapter = countryAdapter

        // Setup age spinner
        val ages = Array(100) { (it + 1).toString() }
        val ageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ages)
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ageSpinner.adapter = ageAdapter
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            onBackPressed()
        }

        calendarIcon.setOnClickListener {
            showDatePickerDialog()
        }

        dobInput.setOnClickListener {
            showDatePickerDialog()
        }

        saveButton.setOnClickListener {
            if (validateInput()) {
                saveProfileChanges()
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

    private fun loadUserData() {
        showProgress(true)

        val userId = auth.currentUser?.uid ?: return

        database.reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    showProgress(false)

                    if (snapshot.exists()) {
                        val username = snapshot.child("username").getValue(String::class.java) ?: ""
                        val fullName = snapshot.child("fullName").getValue(String::class.java) ?: ""
                        val phoneNumber = snapshot.child("phoneNumber").getValue(String::class.java) ?: ""
                        val dob = snapshot.child("dateOfBirth").getValue(String::class.java) ?: ""
                        val country = snapshot.child("country").getValue(String::class.java) ?: ""
                        val age = snapshot.child("age").getValue(String::class.java) ?: ""

                        // Set values to input fields
                        usernameInput.setText(username)
                        fullNameInput.setText(fullName)
                        phoneInput.setText(phoneNumber)
                        dobInput.setText(dob)

                        // Set country spinner
                        val countries = resources.getStringArray(R.array.countries)
                        val countryIndex = countries.indexOf(country)
                        if (countryIndex >= 0) {
                            countrySpinner.setSelection(countryIndex)
                        }

                        // Set age spinner
                        val ageIndex = age.toIntOrNull()?.minus(1) ?: 17
                        if (ageIndex in 0..99) {
                            ageSpinner.setSelection(ageIndex)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showProgress(false)
                    Toast.makeText(this@EditProfileActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun validateInput(): Boolean {
        if (fullNameInput.text.toString().trim().isEmpty()) {
            fullNameInput.error = "Please enter your full name"
            fullNameInput.requestFocus()
            return false
        }

        if (usernameInput.text.toString().trim().isEmpty()) {
            usernameInput.error = "Please enter a username"
            usernameInput.requestFocus()
            return false
        }

        if (phoneInput.text.toString().trim().isEmpty()) {
            phoneInput.error = "Please enter your phone number"
            phoneInput.requestFocus()
            return false
        }

        if (dobInput.text.toString().trim().isEmpty()) {
            dobInput.error = "Please enter your date of birth"
            dobInput.requestFocus()
            return false
        }

        return true
    }
    private fun saveProfileChanges() {
        showProgress(true)

        val userId = auth.currentUser?.uid ?: return
        val username = usernameInput.text.toString().trim()
        val fullName = fullNameInput.text.toString().trim()
        val phoneNumber = phoneInput.text.toString().trim()
        val dob = dobInput.text.toString().trim()
        val country = countrySpinner.selectedItem.toString()
        val age = ageSpinner.selectedItem.toString()

        // Create updates map
        val updates = HashMap<String, Any>()
        updates["username"] = username
        updates["fullName"] = fullName
        updates["phoneNumber"] = phoneNumber
        updates["dateOfBirth"] = dob
        updates["country"] = country
        updates["age"] = age

        // Update database
        database.reference.child("users").child(userId)
            .updateChildren(updates)
            .addOnCompleteListener { task ->
                showProgress(false)

                if (task.isSuccessful) {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to update profile: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showProgress(show: Boolean) {
        progressView.visibility = if (show) View.VISIBLE else View.GONE
        saveButton.isEnabled = !show
    }
}