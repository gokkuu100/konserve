package com.example.konserve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import android.transition.AutoTransition
import android.transition.TransitionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.konserve.models.Report
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent

class HomeFragment : Fragment() {
    private lateinit var reportsRecyclerView: RecyclerView
    private lateinit var menuIcon: ImageView
    private var popupWindow: PopupWindow? = null
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var reportsAdapter: ReportsAdapter
    private lateinit var progressBar: ProgressBar
    private var reportListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        // Initialize Firebase Manager
        firebaseManager = FirebaseManager(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
        
        // Initialize views
        reportsRecyclerView = view.findViewById(R.id.reportsRecyclerView)
        menuIcon = view.findViewById(R.id.menuIcon)
        progressBar = view.findViewById(R.id.progressBar)
        
        setupRecyclerView()
        setupMenuIcon()
        fetchReports()
        
        return view
    }

    private fun setupRecyclerView() {
        reportsAdapter = ReportsAdapter()
        reportsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reportsAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupMenuIcon() {
        menuIcon.setOnClickListener { showMenu(it) }
    }

    private fun fetchReports() {
        progressBar.visibility = View.VISIBLE
        
        reportListener = firebaseManager.fetchReports { reports, error ->
            progressBar.visibility = View.GONE
            
            if (error != null) {
                showError("Error fetching reports: $error")
                return@fetchReports
            }
            
            reports?.let {
                reportsAdapter.submitList(it)
            }
        }
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
            FirebaseAuth.getInstance().signOut()
            // Navigate to LoginActivity
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
        reportListener?.remove()
        popupWindow?.dismiss()
    }
}

class ReportsAdapter : ListAdapter<Report, ReportsAdapter.ReportViewHolder>(ReportDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_card, parent, false)
        return ReportViewHolder(view)
    }

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.reportTitle)
        private val dateView: TextView = itemView.findViewById(R.id.reportDate)
        private val previewDescriptionView: TextView = itemView.findViewById(R.id.reportPreviewDescription)
        private val fullDescriptionView: TextView = itemView.findViewById(R.id.reportFullDescription)
        private val imageView: ImageView = itemView.findViewById(R.id.reportImage)
        private val expandButton: TextView = itemView.findViewById(R.id.expandButton)
        private val collapseButton: TextView = itemView.findViewById(R.id.collapseButton)
        private val expandedLayout: LinearLayout = itemView.findViewById(R.id.expandedLayout)
        private var isExpanded = false

        fun bind(report: Report) {
            titleView.text = report.title
            dateView.text = formatDate(report.date.toDate())
            previewDescriptionView.text = report.description
            fullDescriptionView.text = report.description

            Glide.with(itemView.context)
                .load(report.imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(imageView)

            expandButton.setOnClickListener {
                isExpanded = true
                updateExpandedState()
            }

            collapseButton.setOnClickListener {
                isExpanded = false
                updateExpandedState()
            }

            updateExpandedState()
        }

        private fun updateExpandedState() {
            expandedLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
            expandButton.visibility = if (isExpanded) View.GONE else View.VISIBLE

            TransitionManager.beginDelayedTransition(
                itemView as ViewGroup,
                AutoTransition()
            )
        }

        private fun formatDate(date: Date): String {
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return formatter.format(date)
        }
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position))
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