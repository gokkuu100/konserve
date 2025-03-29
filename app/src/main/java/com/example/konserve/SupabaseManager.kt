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
import com.example.konserve.models.Report
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import java.io.InputStream
import java.util.Properties
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.tasks.await
import io.ktor.client.plugins.logging.*


object Config {
    fun getProperty(context: Context, key: String): String {
        val properties = Properties()
        val inputStream = context.assets.open("config.properties")
        properties.load(inputStream)
        return properties.getProperty(key) ?: ""
    }
}



class SupabaseManager(context: Context) {

    private val supabaseUrl = Config.getProperty(context, "SUPABASE_URL")
    private val supabaseKey = Config.getProperty(context, "SUPABASE_KEY")

    private val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(50, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(50, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(50, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL  // Logs everything (headers, body, etc.)
        }
    }

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = supabaseKey

    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
        install(Realtime)

        httpEngine = httpClient.engine

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
            // Sign in to Supabase with Google token
            val session = client.auth.signInWith(Google) {
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
                } catch (e: Exception) {
                    // User doesn't exist in your users table, create a new entry
                    val email = user.email ?: ""
                    val fullName = user.userMetadata?.get("full_name") as? String ?: ""

                    val userData = mapOf(
                        "user_id" to userId,
                        "email" to email,
                        "full_name" to fullName,
                        "created_at" to System.currentTimeMillis(),
                        "points" to 0
                    )

                    withContext(Dispatchers.IO) {
                        client.postgrest["users"].insert(userData)
                    }
                }

                onComplete(true, null)
            } else {
                onComplete(false, "Failed to get user ID")
            }
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Supabase Google Sign-In failed: ${e.message}")
            onComplete(false, "Authentication failed: ${e.message}")
        }
    }

    companion object {
        const val REQUEST_CODE_GOOGLE_SIGN_IN = 1001
    }

    // Register User
    suspend fun registerUser(email: String, password: String, fullName: String, onComplete: (Boolean, String?) -> Unit) {
        try {
            Log.d("SupabaseDebug", "Attempting to register user: $email")

            val user = client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            user?.let {
                val userData = mapOf(
                    "user_id" to it.id,
                    "email" to email,
                    "full_name" to fullName,
                    "created_at" to System.currentTimeMillis(),
                    "reward_points" to 0
                )

                withContext(Dispatchers.IO) {
                    client.postgrest["users"].insert(userData)
                }
                Log.d("SupabaseDebug", "User registration successful: ${it.id}")
                onComplete(true, null)
            } ?: run {
                Log.e("SupabaseDebug", "Registration failed: User object is null")
                onComplete(false, "Registration failed")
            }
        } catch (e: Exception) {
            Log.e("SupabaseDebug", "Error during registration: ${e.localizedMessage}", e)
            onComplete(false, e.localizedMessage ?: "Unknown error")
        }
    }

    // Login User
    suspend fun loginUser(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            onComplete(true, null)
        } catch (e: Exception) {
            onComplete(false, e.localizedMessage ?: "Login failed")
        }
    }

    // Get Current User
    suspend fun getCurrentUser(): String? {
        return try {
            client.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }

    // Get User Data
    suspend fun getUserData(userId: String, onComplete: (Map<String, Any>?, String?) -> Unit) {
        try {
            val response = withContext(Dispatchers.IO) {
                client.postgrest["users"]
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeSingle<Map<String, Any>>()
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
                client.storage.from("your_bucket_name").upload(
                    path = fileName,
                    file = tempFile  // Pass the File object instead of ByteArray
                )
            }

            // Construct the public URL manually
            val bucketName = "your_bucket_name"
            val supabaseUrl = client.supabaseUrl
            "$supabaseUrl/storage/v1/object/public/$bucketName/$fileName"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Save Report to Supabase Database
    suspend fun saveReport(reportData: Map<String, Any>): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                client.postgrest["reports"].insert(reportData)
            }
            true  // If no exception, insertion was successful
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

    suspend fun fetchUserPoints(userId: String, onComplete: (Int?, String?) -> Unit) {
        try {
            val response = withContext(Dispatchers.IO) {
                client.postgrest["users"]
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeSingle<Map<String, Any>>()
            }
            val points = response["points"] as? Int ?: 0
            onComplete(points, null)
        } catch (e: Exception) {
            onComplete(null, e.localizedMessage ?: "Error fetching user points")
        }
    }

    // Update user points
    suspend fun updateUserPoints(userId: String, points: Int, onComplete: (Boolean, String?) -> Unit) {
        try {
            withContext(Dispatchers.IO) {
                client.postgrest["users"]
                    .update(mapOf("points" to points)) {
                        filter {
                            eq("user_id", userId)
                        }
                    }
            }
            onComplete(true, null)
        } catch (e: Exception) {
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
                    .decodeList<Map<String, Any>>()
            }
            val redeemedCodes = response.map {
                Pair(it["code"] as String, it["points"] as Int)
            }
            onComplete(redeemedCodes, null)
        } catch (e: Exception) {
            onComplete(null, e.localizedMessage ?: "Error fetching redeemed codes")
        }
    }

    // Redeem code
    suspend fun redeemCode(userId: String, code: String, points: Int, onComplete: (Boolean, String?) -> Unit) {
        try {
            withContext(Dispatchers.IO) {
                client.postgrest["redeemed_codes"].insert(
                    mapOf(
                        "user_id" to userId,
                        "code" to code,
                        "points" to points,
                        "redeemed_at" to System.currentTimeMillis()
                    )
                )
            }
            onComplete(true, null)
        } catch (e: Exception) {
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
