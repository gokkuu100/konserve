package com.example.konserve

import android.app.Activity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.icu.util.TimeUnit
import android.net.Uri
import android.util.Log
import android.util.Patterns
import com.example.konserve.models.Feedback
import com.example.konserve.models.RedeemedCode
import com.example.konserve.models.Report
import com.example.konserve.models.ReportCase
import com.example.konserve.models.User
import com.example.konserve.models.UserPoints
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import java.util.Properties
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.tasks.await
import io.ktor.client.plugins.logging.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.format.DateTimeFormatter
import java.time.Instant


object Config {
    fun getProperty(context: Context, key: String): String {
        val properties = Properties()
        val inputStream = context.assets.open("config.properties")
        properties.load(inputStream)
        return properties.getProperty(key) ?: ""
    }
}

// Custom HTTP client with extended timeouts
private val httpClient = HttpClient(OkHttp) {
    engine {
        // Configure OkHttp engine with extended timeouts
        config {
            connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            retryOnConnectionFailure(true)
        }
    }

    // Add HTTP timeout plugin
    install(HttpTimeout) {
        requestTimeoutMillis = 60000 // 60 seconds
        connectTimeoutMillis = 60000 // 60 seconds
        socketTimeoutMillis = 60000 // 60 seconds
    }

    // Add logging for debugging
    install(Logging) {
        level = LogLevel.ALL
        logger = object : Logger {
            override fun log(message: String) {
                Log.d("SupabaseHTTP", message)
            }
        }
    }
}



class SupabaseManager(context: Context) {

    private val supabaseUrl = Config.getProperty(context, "SUPABASE_URL")
    private val supabaseKey = Config.getProperty(context, "SUPABASE_KEY")


    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = supabaseKey
    ) {
        install(Auth) {
            // Add this configuration to persist session
            autoSaveToStorage = true
            autoLoadFromStorage = true
        }
        install(Postgrest)
        install(Storage)
        install(Realtime)
    }

    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    fun initGoogleSignIn(context: Context) {
        oneTapClient = Identity.getSignInClient(context)

        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId("YOUR_WEB_CLIENT_ID") // Replace with your web client ID from Google Cloud Console
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }

    suspend fun signInWithGoogle(activity: Activity, onComplete: (Boolean, String?) -> Unit) {
        try {
            val result = oneTapClient.beginSignIn(signInRequest).await()
            activity.startIntentSenderForResult(
                result.pendingIntent.intentSender,
                REQUEST_CODE_GOOGLE_SIGN_IN,
                null,
                0,
                0,
                0
            )
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "Google Sign-In failed: ${e.message}")
            onComplete(false, "Google Sign-In failed: ${e.message}")
        }
    }

    suspend fun handleGoogleSignInResult(idToken: String, onComplete: (Boolean, String?) -> Unit) {
        try {
            Log.d("GoogleAuth", "Processing Google Sign-In with token")

            val googleIdToken = googleId
            
            // Sign in to Supabase with Google token
            val session = client.auth.signInWith(IDToken) {
                this.idToken = idToken
            }

            // Check if user exists in your users table
            val user = client.auth.currentUserOrNull()
            val userId = user?.id
            if (userId != null) {
                try {
                    // Check if user exists in your custom users table
                    client.postgrest["users"]
                        .select {
                            filter {
                                eq("user_id", userId)
                            }
                        }
                        .decodeSingle<Map<String, Any>>()
                    
                    // User exists, just return success
                    Log.d("GoogleAuth", "User already exists in database")
                    onComplete(true, null)
                    
                } catch (e: Exception) {
                    // User doesn't exist in your users table, create a new entry
                    Log.d("GoogleAuth", "Creating new user in database")
                    val email = user.email ?: ""
                    val fullName = user.userMetadata?.get("full_name") as? String 
                        ?: user.userMetadata?.get("name") as? String 
                        ?: email.substringBefore("@")  // Fallback to email username

                    // Create a User object
                    val newUser = User(
                        user_id = userId,
                        full_name = fullName,
                        email = email,
                        created_at = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        reward_points = 0
                    )

                    withContext(Dispatchers.IO) {
                        client.postgrest["users"].insert(newUser)
                        Log.d("GoogleAuth", "New user inserted: $fullName, $email")
                    }

                    onComplete(true, null)
                }
            } else {
                Log.e("GoogleAuth", "Failed to get user ID")
                onComplete(false, "Failed to get user ID")
            }
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Supabase Google Sign-In failed: ${e.message}", e)
            onComplete(false, "Authentication failed: ${e.message}")
        }
    }

    companion object {
        const val REQUEST_CODE_GOOGLE_SIGN_IN = 1001
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Function to check if an email is already registered
    suspend fun isEmailRegistered(email: String): Boolean {
        return try {
            val response = client.auth.signInWith(Email) {
                this.email = email
                this.password = "dummy_password"  // Dummy password to check if email exists
            }
            response != null  // If login succeeds, email exists
        } catch (e: Exception) {
            false  // If error occurs (e.g., invalid login), email is not registered
        }
    }

    suspend fun registerUser(email: String, password: String, fullName: String, onComplete: (Boolean, String?) -> Unit) {
        try {
            // Validate email format
            if (!isValidEmail(email)) {
                onComplete(false, "Invalid email format")
                return
            }

            // Check if email is already registered
            if (isEmailRegistered(email)) {
                onComplete(false, "Email is already registered. Try logging in.")
                return
            }

            Log.d("SupabaseDebug", "Attempting to register user: $email")

            val sanitizedEmail = email.trim().lowercase()

            // Register user with Supabase Auth
            val result = client.auth.signUpWith(Email) {
                this.email = sanitizedEmail
                this.password = password
            }

            Log.d("SupabaseDebug", "Sign-up response: $result")

            // Fetch the current user ID from Supabase Auth
            val userId = client.auth.currentUserOrNull()?.id
            if (userId == null) {
                Log.e("SupabaseDebug", "Registration failed: User ID not found")
                onComplete(false, "Registration failed")
                return
            }

            // Create a User object
            val newUser = User(
                user_id = userId,
                full_name = fullName,
                email = sanitizedEmail,
                created_at = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                reward_points = 0
            )

            // Convert User object to JSON string for serialization
            val userJson = Json.encodeToString(newUser)

            // Insert into Supabase Users table
            withContext(Dispatchers.IO) {
                val response = client.postgrest["users"].insert(newUser)
                Log.d("SupabaseDebug", "User inserted into database: $response")
            }

            onComplete(true, null)
        } catch (e: Exception) {
            Log.e("SupabaseDebug", "Error during registration: ${e.localizedMessage}", e)
            onComplete(false, e.localizedMessage ?: "Unknown error")
        }
    }

    // Login User
    suspend fun loginUser(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        try {
            client.auth.signInWith(Email) { // Sign in the user
                this.email = email
                this.password = password
            }

            // Get session & user from result
            val session = client.auth.currentSessionOrNull()  // Fetch session
            val user = client.auth.currentUserOrNull()  // Fetch logged-in user

            if (session != null && user != null) {
                Log.d("SupabaseDebug", "Login successful: ${user.id}")
                onComplete(true, null)
            } else {
                Log.e("SupabaseDebug", "Login failed: No session or user returned")
                onComplete(false, "Login failed. No session or user returned.")
            }
        } catch (e: Exception) {
            Log.e("SupabaseDebug", "Error during login: ${e.localizedMessage}", e)
            onComplete(false, e.localizedMessage ?: "Unknown error")
        }
    }

    // Get Current User
    suspend fun getCurrentUser(): String? {
        try {
            // Wait until session is loaded from storage
            var session = client.auth.currentSessionOrNull()
            var retries = 5
            while (session == null && retries > 0) {
                delay(300L)
                session = client.auth.currentSessionOrNull()
                retries--
            }

            if (session != null && session.expiresAt.toEpochMilliseconds() < System.currentTimeMillis()) {
                client.auth.refreshCurrentSession()
            }

            val currentUser = client.auth.currentUserOrNull()
            Log.d("Supabase", "Current User: ${currentUser}")
            return currentUser?.id
        } catch (e: Exception) {
            Log.e("Supabase", "Error fetching current user: ${e.localizedMessage}")
            return null
        }
    }


    // Get User Data
    suspend fun getUserData(userId: String, onComplete: (User?, String?) -> Unit) {
        try {
            val response = withContext(Dispatchers.IO) {
                client.postgrest["users"]
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeSingle<User>()
            }
            onComplete(response, null)
        } catch (e: Exception) {
            onComplete(null, e.localizedMessage ?: "Error fetching user data")
        }
    }

    suspend fun uploadImage(context: Context, fileName: String, fileBytes: ByteArray): String? {
        return try {
            val tempFile = File(context.cacheDir, fileName)  // Create a temp file
            FileOutputStream(tempFile).use { it.write(fileBytes) }  // Write ByteArray to file

            withContext(Dispatchers.IO) {
                client.storage.from("reportcaseimages").upload(
                    path = fileName,
                    file = tempFile  // Pass the File object instead of ByteArray
                )
            }

            // Construct the public URL manually
            val bucketName = "reportcaseimages"
            val supabaseUrl = client.supabaseUrl
            "$supabaseUrl/storage/v1/object/public/$bucketName/$fileName"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun uploadProfilePhoto(context: Context, userId: String, fileBytes: ByteArray): String? {
        return try {
            val fileName = "$userId.jpg"
            val bucketName = "profileimages"
            val path = "$bucketName/$fileName"

            val tempFile = File(context.cacheDir, fileName)
            FileOutputStream(tempFile).use { it.write(fileBytes) }

            withContext(Dispatchers.IO) {
                client.storage.from(bucketName).upload(path, tempFile)
            }

            // Construct public URL
            val supabaseUrl = client.supabaseUrl
            "$supabaseUrl/storage/v1/object/public/$path"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Save Report to Supabase Database
    suspend fun saveReportCase(reportCase: ReportCase): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                client.postgrest["report_case"].insert(reportCase)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveFeedback(feedback: Feedback): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                client.postgrest["feedback"].insert(feedback)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun uriToByteArray(context: Context, uri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { it.readBytes() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchReports(onComplete: (List<Report>?, String?) -> Unit) {
        try {
            val reports = withContext(Dispatchers.IO) {
                client.postgrest["reports"]
                    .select()
                    .decodeList<Report>()
            }
            onComplete(reports, null)
        } catch (e: Exception) {
            onComplete(null, e.localizedMessage ?: "Error fetching reports")
        }
    }

    fun fetchUserPoints(userId: String, callback: (Int?, Exception?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.postgrest["users"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingle<User>()  // ✅ Use properly serialized class

                val points = response.reward_points

                withContext(Dispatchers.Main) {
                    callback(points, null)
                }
            } catch (e: Exception) {
                Log.e("SupabaseManager", "Error fetching user points", e)
                withContext(Dispatchers.Main) {
                    callback(null, e)
                }
            }
        }
    }


    // Update user points
    suspend fun updateUserPoints(userId: String, points: Int, onComplete: (Boolean, String?) -> Unit) {
        try {
            withContext(Dispatchers.IO) {
                client.postgrest["users"]
                    .update(mapOf("reward_points" to points)) {
                        filter {
                            eq("user_id", userId)
                        }
                    }
            }
            onComplete(true, null)
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Error updating user points", e)
            onComplete(false, e.localizedMessage ?: "Error updating user points")
        }
    }

    // Fetch redeemed codes
    suspend fun fetchRedeemedCodes(userId: String, onComplete: (List<Pair<String, Int>>?, String?) -> Unit) {
        try {
            val response = withContext(Dispatchers.IO) {
                client.postgrest["redeemed_codes"]
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<RedeemedCode>()  // ✅ Use the data class
            }

            val redeemedCodes = response.map { Pair(it.code, it.points) }

            onComplete(redeemedCodes, null)
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Error fetching redeemed codes", e)
            onComplete(null, e.localizedMessage ?: "Error fetching redeemed codes")
        }
    }

    suspend fun redeemCode(userId: String, code: String, points: Int, onComplete: (Boolean, String?) -> Unit) {
        try {
            val redeemedCode = RedeemedCode(
                user_id = userId,
                code = code,
                points = points,
                redeemed_at = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            )

            withContext(Dispatchers.IO) {
                client.postgrest["redeemed_codes"]
                    .insert(redeemedCode)  // ✅ No need for manual JSON conversion
            }

            onComplete(true, null)
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Error redeeming code", e)
            onComplete(false, e.localizedMessage ?: "Error redeeming code")
        }
    }


    // Logout User
    suspend fun logoutUser(onComplete: (Boolean, String?) -> Unit) {
        try {
            client.auth.signOut()
            onComplete(true, null)
        } catch (e: Exception) {
            onComplete(false, e.localizedMessage ?: "Logout failed")
        }
    }
}
