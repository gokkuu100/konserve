package com.example.konserve

import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.konserve.models.Report
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.Instant
import java.time.format.DateTimeFormatter
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

        if (!reportId.isNullOrBlank()) {
            try {
                val reportIdInt = reportId.toInt()
                loadReportDetails(reportIdInt)
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Invalid Report ID", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            finish()
        }
    }

    private fun loadReportDetails(reportId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    supabaseManager.client.postgrest.from("reports")
                        .select {
                            filter { eq("id", reportId.toInt()) }
                        }
                        .decodeSingle<Report>()
                }

                withContext(Dispatchers.Main) {
                    reportTitle.text = response.title
                    reportAuthor.text = "By " + (response.author ?: "Unknown Author")
                    reportDate.text = formatDate(response.date)
                    reportDescription.text = response.description

                    Glide.with(this@ReportDetailedActivity)
                        .load(response.imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(reportImage)
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
            val correctedTimestamp = dateString.replace(" ", "T").substringBefore("+") + "Z"
            val instant = Instant.parse(correctedTimestamp)
            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.getDefault())
            zonedDateTime.format(formatter)
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
