package com.example.konserve

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ReportDetailedActivity : AppCompatActivity() {
    private lateinit var reportImage: ImageView
    private lateinit var reportTitle: TextView
    private lateinit var reportAuthor: TextView
    private lateinit var reportDate: TextView
    private lateinit var reportDescription: TextView
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_detail)

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
        FirebaseFirestore.getInstance().collection("reports")
            .document(reportId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val title = document.getString("title") ?: ""
                    val description = document.getString("description") ?: ""
                    val imageUrl = document.getString("imageUrl")
                    val author = document.getString("author") ?: "Unknown Author"
                    val date = document.getTimestamp("date")?.toDate() ?: Date()

                    reportTitle.text = title
                    reportAuthor.text = "By $author"
                    reportDate.text = formatDate(date)
                    reportDescription.text = description

                    imageUrl?.let {
                        Glide.with(this)
                            .load(it)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.error_image)
                            .into(reportImage)
                    }
                }
            }
    }

    private fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        return formatter.format(date)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}