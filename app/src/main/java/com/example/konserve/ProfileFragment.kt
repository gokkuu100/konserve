package com.example.konserve

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        supabaseManager.getUserData(userId) { data, error ->
            CoroutineScope(Dispatchers.Main).launch {
                if (data != null) {
                    userNameEditText.setText(data["full_name"] as? String ?: "")
                    phoneEditText.setText(data["phone"] as? String ?: "")
                    genderEditText.setText(data["gender"] as? String ?: "")
                    addressEditText.setText(data["address"] as? String ?: "")
                    loadProfileImage(userId)
                } else {
                    Toast.makeText(requireContext(), error ?: "Error loading data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun loadProfileImage(userId: String) {
        val imageUrl = "https://your-storage-url/profile-images/$userId.jpg"
        withContext(Dispatchers.Main) {
            Glide.with(this@ProfileFragment).load(imageUrl).into(profileImage)
        }
    }

    private suspend fun saveUserData(userId: String) {
        val updatedData = mapOf(
            "full_name" to userNameEditText.text.toString(),
            "phone" to phoneEditText.text.toString(),
            "gender" to genderEditText.text.toString(),
            "address" to addressEditText.text.toString()
        )

        supabaseManager.client.postgrest["users"].update(updatedData) { filter { eq("user_id", userId) } }

        withContext(Dispatchers.Main) {
            Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            uploadProfileImage(userId)
        }
    }

    private fun uploadProfileImage(userId: String) {
        imageUri?.let { uri ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val byteArray = inputStream!!.readBytes() // Convert InputStream to ByteArray
                    supabaseManager.client.storage
                        .from("profile-images")
                        .upload("$userId.jpg", byteArray)

                    withContext(Dispatchers.Main) {
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