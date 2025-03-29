package com.example.konserve

import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ReportDetailedActivity : AppCompatActivity() {
    private lateinit var reportImage: ImageView
    private lateinit var reportTitle: TextView
    private lateinit var reportAuthor: TextView
    private lateinit var reportDate: TextView
    private lateinit var reportDescription: TextView
    private lateinit var backButton: ImageView

    private lateinit var supabaseManager: SupabaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_detail)

        supabaseManager = SupabaseManager(this)

        // Initialize views
        reportImage = findViewById(R.id.reportDetailImage)
        reportTitle = findViewById(R.id.reportDetailTitle)
        reportAuthor = findViewById(R.id.reportDetailAuthor)
        reportDate = findViewById(R.id.reportDetailDate)
        reportDescription = findViewById(R.id.reportDetailDescription)
        backButton = findViewById(R.id.backButton)

        // Setup action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Report Details"

        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Get report ID from intent
        val reportId = intent.getStringExtra("REPORT_ID")

        if (reportId != null) {
            loadReportDetails(reportId)
        } else {
            finish()
        }
    }

    private fun loadReportDetails(reportId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    supabaseManager.client.postgrest["reports"]
                        .select {
                            filter { eq("id", reportId) }
                        }
                        .decodeSingle<Map<String, Any>>()
                }

                withContext(Dispatchers.Main) {
                    reportTitle.text = response["title"] as? String ?: ""
                    reportAuthor.text = "By " + (response["author"] as? String ?: "Unknown Author")
                    reportDate.text = formatDate(response["date"] as? String ?: "")
                    reportDescription.text = response["description"] as? String ?: ""

                    val imageUrl = response["imageUrl"] as? String
                    imageUrl?.let {
                        Glide.with(this@ReportDetailedActivity)
                            .load(it)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.error_image)
                            .into(reportImage)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ReportDetailedActivity, "Error loading report: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            "Unknown Date"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
