package com.example.konserve

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.example.konserve.models.Report
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HomeFragment : Fragment() {
    private lateinit var reportsRecyclerView: RecyclerView
    private lateinit var menuIcon: ImageView
    private lateinit var featuredReportContainer: FrameLayout
    private lateinit var featuredReportImage: ImageView
    private lateinit var featuredReportTitle: TextView
    private lateinit var featuredReportAuthor: TextView
    private lateinit var featuredReportDate: TextView
    private var popupWindow: PopupWindow? = null
    private lateinit var supabaseManager: SupabaseManager
    private lateinit var reportsAdapter: ReportsAdapter
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize Supabase Manager
        supabaseManager = SupabaseManager(requireContext())

        // Initialize views
        reportsRecyclerView = view.findViewById(R.id.reportsRecyclerView)
        menuIcon = view.findViewById(R.id.menuIcon)
        progressBar = view.findViewById(R.id.progressBar)
        featuredReportContainer = view.findViewById(R.id.featuredReportContainer)
        featuredReportImage = view.findViewById(R.id.featuredReportImage)
        featuredReportTitle = view.findViewById(R.id.featuredReportTitle)
        featuredReportAuthor = view.findViewById(R.id.featuredReportAuthor)
        featuredReportDate = view.findViewById(R.id.featuredReportDate)

        setupRecyclerView()
        setupMenuIcon()
        fetchReports()

        return view
    }

    private fun formatDate(timestamp: Instant): String {
        val date = Date.from(timestamp) // Convert Instant to Date
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault()) // Use system's timezone
        return formatter.format(timestamp)
    }

    private fun setupRecyclerView() {
        reportsAdapter = ReportsAdapter(object : ReportClickListener {
            override fun onReportClick(report: Report) {
                navigateToReportDetail(report)
            }
        })
        reportsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reportsAdapter
        }
    }

    private fun setupMenuIcon() {
        menuIcon.setOnClickListener { showMenu(it) }
    }

    private fun fetchReports() {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.Main).launch {
            supabaseManager.fetchReports { reports, error ->
                progressBar.visibility = View.GONE

                if (error != null) {
                    showError("Error fetching reports: $error")
                    return@fetchReports
                }

                reports?.let {
                    if (it.isNotEmpty()) {
                        setupFeaturedReport(it[0])
                        reportsAdapter.submitList(it.subList(1, it.size))
                    } else {
                        reportsAdapter.submitList(emptyList())
                    }
                }
            }
        }
    }

    private fun setupFeaturedReport(report: Report) {
        featuredReportTitle.text = report.title
        featuredReportAuthor.text = "By " + (report.author ?: "Unknown Author")
        featuredReportDate.text = formatDate(report.date)

        Glide.with(requireContext())
            .load(report.imageUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .into(featuredReportImage)

        featuredReportContainer.setOnClickListener {
            navigateToReportDetail(report)
        }
    }

    private fun navigateToReportDetail(report: Report) {
        val intent = Intent(requireContext(), ReportDetailedActivity::class.java).apply {
            putExtra("REPORT_ID", report.id)
        }
        startActivity(intent)
    }

    private fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(date)
    }

    private fun showMenu(anchorView: View) {
        // Inflate menu layout
        val menuView = LayoutInflater.from(requireContext())
            .inflate(R.layout.menu_popup, null)
        // Create popup window
        popupWindow = PopupWindow(
            menuView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 20f
            setBackgroundDrawable(null)
        }
        // Setup menu item clicks
        menuView.findViewById<View>(R.id.profileMenuItem).setOnClickListener {
            popupWindow?.dismiss()
            // Navigate to ProfileFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, ProfileFragment())
                .addToBackStack(null)
                .commit()
        }
        menuView.findViewById<View>(R.id.reportMenuItem).setOnClickListener {
            popupWindow?.dismiss()
            // Navigate to ReportMenuFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, ReportMenuFragment())
                .addToBackStack(null)
                .commit()
        }
        menuView.findViewById<View>(R.id.talkMenuItem).setOnClickListener {
            popupWindow?.dismiss()
            // Navigate to TalkMenuFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, TalkMenuFragment())
                .addToBackStack(null)
                .commit()
        }
        menuView.findViewById<View>(R.id.aboutMenuItem).setOnClickListener {
            popupWindow?.dismiss()
            // Navigate to AboutMenuFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, AboutMenuFragment())
                .addToBackStack(null)
                .commit()
        }
        menuView.findViewById<View>(R.id.logoutMenuItem).setOnClickListener {
            popupWindow?.dismiss()
            // Handle logout
            // Implement Supabase logout if needed
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
        // Show popup
        popupWindow?.showAsDropDown(anchorView)
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        popupWindow?.dismiss()
    }
}

interface ReportClickListener {
    fun onReportClick(report: Report)
}

class ReportsAdapter(private val clickListener: ReportClickListener) :
    ListAdapter<Report, ReportsAdapter.ReportViewHolder>(ReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_card, parent, false)
        return ReportViewHolder(view)
    }

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.reportTitle)
        private val thumbnailView: ImageView = itemView.findViewById(R.id.reportThumbnail)
        private val authorView: TextView = itemView.findViewById(R.id.reportAuthor)
        private val dateView: TextView = itemView.findViewById(R.id.reportDate)

        fun bind(report: Report, clickListener: ReportClickListener) {
            titleView.text = report.title
            authorView.text = "By " + (report.author ?: "Unknown Author")
            dateView.text =  formatDate(Date.from(report.date))

            Glide.with(itemView.context)
                .load(report.imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(thumbnailView)

            itemView.setOnClickListener {
                clickListener.onReportClick(report)
            }
        }

        private fun formatDate(date: Date): String {
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return formatter.format(date)
        }
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }
}

class ReportDiffCallback : DiffUtil.ItemCallback<Report>() {
    override fun areItemsTheSame(oldItem: Report, newItem: Report): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Report, newItem: Report): Boolean {
        return oldItem == newItem
    }
}