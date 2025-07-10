package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import com.example.myapplication.Toast

class ProfileFragment : Fragment() {

    // UI elements
    private lateinit var editProfileButton: ImageView
    private lateinit var profileImage: CircleImageView
    private lateinit var usernameText: TextView
    private lateinit var fullNameText: TextView
    private lateinit var emailText: TextView
    private lateinit var quizCountText: TextView
    private lateinit var followersCountText: TextView
    private lateinit var followingCountText: TextView
    private lateinit var logoutButton: Button
    private lateinit var progressView: View
    private lateinit var progressBar: ProgressBar

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    // Image picker
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadProfileImage(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize views
        initViews(view)

        // Setup listeners
        setupListeners()

        // Load user data
        loadUserProfile()

        return view
    }

    private fun initViews(view: View) {
        editProfileButton = view.findViewById(R.id.editProfileButton)
        profileImage = view.findViewById(R.id.profileImage)
        usernameText = view.findViewById(R.id.usernameText)
        fullNameText = view.findViewById(R.id.fullNameText)
        emailText = view.findViewById(R.id.emailText)
        quizCountText = view.findViewById(R.id.quizCountText)
        followersCountText = view.findViewById(R.id.followersCountText)
        followingCountText = view.findViewById(R.id.followingCountText)
        logoutButton = view.findViewById(R.id.logoutButton)
        progressView = view.findViewById(R.id.progressView)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        editProfileButton.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        profileImage.setOnClickListener {
            getContent.launch("image/*")
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            // Navigate back to login screen
            val intent = Intent(requireContext(), WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun loadUserProfile() {
        // Check if user is logged in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        showLoading(true)

        database.reference.child("users").child(currentUser.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    showLoading(false)

                    if (snapshot.exists()) {
                        // Get user data
                        val username = snapshot.child("username").getValue(String::class.java)
                            ?: currentUser.displayName
                            ?: "User"
                        val fullName = snapshot.child("fullName").getValue(String::class.java) ?: ""
                        val email = snapshot.child("email").getValue(String::class.java) ?: currentUser.email ?: ""
                        val photoUrl = snapshot.child("photoUrl").getValue(String::class.java) ?: ""
                        val quizCount = snapshot.child("quizCount").getValue(Long::class.java)?.toInt() ?: 0
                        val followersCount = snapshot.child("followersCount").getValue(Long::class.java)?.toInt() ?: 0
                        val followingCount = snapshot.child("followingCount").getValue(Long::class.java)?.toInt() ?: 0

                        // Update UI with String.format() to handle locale settings
                        usernameText.text = username
                        fullNameText.text = fullName
                        emailText.text = email
                        quizCountText.text = String.format("%d", quizCount)
                        followersCountText.text = String.format("%d", followersCount)
                        followingCountText.text = String.format("%d", followingCount)

                        // Load profile image
                        if (photoUrl.isNotEmpty()) {
                            Glide.with(requireContext())
                                .load(photoUrl)
                                .placeholder(R.drawable.default_profile_image)
                                .into(profileImage)
                        }
                    } else {
                        createBasicProfile(currentUser.uid)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showLoading(false)
                    Toast.show(requireContext(), "Error: ${error.message}")
                }
            })
    }

    private fun createBasicProfile(userId: String) {
        val user = auth.currentUser ?: return

        val userData = hashMapOf(
            "id" to userId,
            "username" to (user.displayName ?: "User"),
            "fullName" to (user.displayName ?: ""),
            "email" to (user.email ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "createdAt" to System.currentTimeMillis(),
            "quizCount" to 0,
            "followersCount" to 0,
            "followingCount" to 0
        )

        database.reference.child("users").child(userId)
            .setValue(userData)
            .addOnSuccessListener {
                loadUserProfile()
            }
            .addOnFailureListener { e ->
                Toast.show(requireContext(), "Failed to create profile: ${e.message}")
            }
    }

    private fun uploadProfileImage(imageUri: Uri) {
        val currentUser = auth.currentUser ?: return

        showLoading(true)

        val storageRef = storage.reference.child("profile_images").child("${currentUser.uid}.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    database.reference.child("users").child(currentUser.uid)
                        .child("photoUrl")
                        .setValue(uri.toString())
                        .addOnSuccessListener {
                            showLoading(false)
                            Toast.show(requireContext(), "Profile image updated")
                        }
                        .addOnFailureListener { e ->
                            showLoading(false)
                            Toast.show(requireContext(), "Failed to update profile: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.show(requireContext(), "Upload failed: ${e.message}")
            }
    }

    private fun showLoading(show: Boolean) {
        progressView.visibility = if (show) View.VISIBLE else View.GONE
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), WelcomeActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }
}