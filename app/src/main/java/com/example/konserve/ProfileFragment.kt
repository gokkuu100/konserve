package com.example.konserve

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProfileFragment : Fragment() {

    private lateinit var firebaseManager: FirebaseManager
    private var imageUri: Uri? = null

    private lateinit var profileImage: ImageView
    private lateinit var editProfileImgBtn: ImageButton
    private lateinit var userNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var genderEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var saveButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        firebaseManager = FirebaseManager(auth, firestore)

        profileImage = view.findViewById(R.id.profile_image)
        editProfileImgBtn = view.findViewById(R.id.edit_profile_image)
        userNameEditText = view.findViewById(R.id.username)
        emailEditText = view.findViewById(R.id.email)
        phoneEditText = view.findViewById(R.id.phone)
        genderEditText = view.findViewById(R.id.gender)
        addressEditText = view.findViewById(R.id.address)
        saveButton = view.findViewById(R.id.save_button)

        val currentUser = auth.currentUser
        currentUser?.let {
            loadUserData(it.uid)
        }

        setFieldsEditable(false)

        // Set up onClick listeners to make fields editable when clicked
        userNameEditText.setOnClickListener { setFieldsEditable(true) }
        emailEditText.setOnClickListener { setFieldsEditable(true) }
        phoneEditText.setOnClickListener { setFieldsEditable(true) }
        genderEditText.setOnClickListener { setFieldsEditable(true) }
        addressEditText.setOnClickListener { setFieldsEditable(true) }

        editProfileImgBtn.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            imagePickerLauncher.launch(Intent.createChooser(intent, "Select Picture"))
        }

        saveButton.setOnClickListener {
            currentUser?.let {
                saveUserData(it.uid)
            }
        }
    }

    private fun setFieldsEditable(editable: Boolean) {
        userNameEditText.isFocusableInTouchMode = editable
        userNameEditText.isFocusable = editable

        emailEditText.isFocusableInTouchMode = editable
        emailEditText.isFocusable = editable

        phoneEditText.isFocusableInTouchMode = editable
        phoneEditText.isFocusable = editable

        genderEditText.isFocusableInTouchMode = editable
        genderEditText.isFocusable = editable

        addressEditText.isFocusableInTouchMode = editable
        addressEditText.isFocusable = editable
    }

    private fun loadUserData(userId: String) {
        firebaseManager.getUserData(userId) { data, error ->
            if (error != null) {
                Toast.makeText(requireContext(), "Error loading data: $error", Toast.LENGTH_SHORT).show()
            } else if (data != null) {
                userNameEditText.setText(data["fullName"] as? String ?: "")
                emailEditText.setText(data["email"] as? String ?: "")
                phoneEditText.setText(data["phone"] as? String ?: "")
                genderEditText.setText(data["gender"] as? String ?: "")
                addressEditText.setText(data["address"] as? String ?: "")

                loadProfileImage(userId)
            }
        }
    }

    private fun loadProfileImage(userId: String) {
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/$userId.jpg")
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this)
                .load(uri)
                .into(profileImage)
        }.addOnFailureListener {
            profileImage.setImageResource(R.drawable.avatar)
        }
    }

    private fun saveUserData(userId: String) {
        val updatedData = hashMapOf(
            "fullName" to userNameEditText.text.toString(),
            "email" to emailEditText.text.toString(),
            "phone" to phoneEditText.text.toString(),
            "gender" to genderEditText.text.toString(),
            "address" to addressEditText.text.toString()
        )

        firebaseManager.firestore.collection("users").document(userId)
            .set(updatedData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                uploadProfileImage(userId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadProfileImage(userId: String) {
        imageUri?.let {
            val ref = FirebaseStorage.getInstance().reference.child("profile_images/$userId.jpg")
            ref.putFile(it)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Profile image uploaded successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to upload profile image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null && result.data?.data != null) {
            imageUri = result.data?.data
            profileImage.setImageURI(imageUri)
        }
    }
}
