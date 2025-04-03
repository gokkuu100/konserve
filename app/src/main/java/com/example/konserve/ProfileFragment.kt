package com.example.konserve

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.konserve.models.User
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.*

class ProfileFragment : Fragment() {

    private lateinit var supabaseManager: SupabaseManager
    private var imageUri: Uri? = null

    private lateinit var profileImage: ImageView
    private lateinit var editProfileImgBtn: ImageButton
    private lateinit var userNameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var genderEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageView

    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        supabaseManager = SupabaseManager(requireContext())

        profileImage = view.findViewById(R.id.profile_image)
        editProfileImgBtn = view.findViewById(R.id.edit_profile_image)
        userNameEditText = view.findViewById(R.id.username)
        phoneEditText = view.findViewById(R.id.phone)
        genderEditText = view.findViewById(R.id.gender)
        addressEditText = view.findViewById(R.id.address)
        saveButton = view.findViewById(R.id.save_button)
        backButton = view.findViewById(R.id.backButton)

        CoroutineScope(Dispatchers.IO).launch {
            val userId = supabaseManager.getCurrentUser()
            Log.d("ProfileFragment", "Fetched user ID: $userId")
            userId?.let { loadUserData(it) }
        }

        setFieldsEditable(false)

        listOf(userNameEditText, phoneEditText, genderEditText, addressEditText).forEach {
            it.setOnClickListener { setFieldsEditable(true) }
        }

        editProfileImgBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            imagePickerLauncher.launch(Intent.createChooser(intent, "Select Picture"))
        }

        saveButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val userId = supabaseManager.getCurrentUser()
                userId?.let { saveUserData(it) }
            }
        }

        backButton.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun setFieldsEditable(editable: Boolean) {
        listOf(userNameEditText, phoneEditText, genderEditText, addressEditText).forEach {
            it.isFocusableInTouchMode = editable
            it.isFocusable = editable
        }
    }

    private suspend fun loadUserData(userId: String) {
        supabaseManager.getUserData(userId) { user, error ->
            CoroutineScope(Dispatchers.Main).launch {
                if (user != null) {
                    currentUser = user
                    Log.d("ProfileFragment", "User loaded: ${user.full_name}")

                    userNameEditText.setText(user.full_name ?: "")
                    phoneEditText.setText(user.phone ?: "")
                    genderEditText.setText(user.gender ?: "")
                    addressEditText.setText(user.address ?: "")

                    if (user.imageUrl.isNotEmpty()) {
                        loadProfileImage(user.imageUrl)
                    }
                } else {
                    Log.e("ProfileFragment", "Error: $error")
                    Toast.makeText(requireContext(), error ?: "Error loading data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadProfileImage(imageUrl: String) {
        Glide.with(this@ProfileFragment).load(imageUrl).into(profileImage)
    }

    private suspend fun saveUserData(userId: String) {
        val updatedUser = currentUser?.copy(
            full_name = userNameEditText.text.toString(),
            phone = phoneEditText.text.toString(),
            gender = genderEditText.text.toString(),
            address = addressEditText.text.toString()
        ) ?: return

        try {
            supabaseManager.client.postgrest["users"].update(updatedUser) { filter { eq("user_id", userId) } }

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }

            uploadProfileImage(userId)

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadProfileImage(userId: String) {
        imageUri?.let { uri ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val byteArray = inputStream!!.readBytes()
                    val path = "profile-images/$userId.jpg"

                    supabaseManager.client.storage
                        .from("profile-images")
                        .upload(path, byteArray)

                    val imageUrl = "https://your-storage-url/$path"

                    supabaseManager.client.postgrest["users"].update(
                        mapOf("imageUrl" to imageUrl)
                    ) { filter { eq("user_id", userId) } }

                    withContext(Dispatchers.Main) {
                        Glide.with(this@ProfileFragment).load(imageUrl).into(profileImage)
                        Toast.makeText(requireContext(), "Profile image uploaded successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Failed to upload profile image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data?.data != null) {
            imageUri = result.data?.data
            profileImage.setImageURI(imageUri)
        }
    }
}
