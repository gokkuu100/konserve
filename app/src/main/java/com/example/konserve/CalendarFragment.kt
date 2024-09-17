package com.example.konserve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.MonthDayBinder
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale


class CalendarFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var memoRecyclerView: RecyclerView
    private lateinit var memoAdapter: MemoAdapter
    private lateinit var firebaseManager: FirebaseManager


    private lateinit var addMemoButton: FloatingActionButton
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private var selectedDate: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.calendar_fragment, container, false)

        calendarView = view.findViewById(R.id.calendarView)
        memoRecyclerView = view.findViewById(R.id.memoRecyclerView)
        addMemoButton = view.findViewById(R.id.addMemoButton)

        // Initialize FirebaseManager
        firebaseManager = FirebaseManager(auth, firestore)

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

        memoRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        memoAdapter = MemoAdapter()
        memoRecyclerView.adapter = memoAdapter

        // Setup the calendar view
        calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        calendarView.scrollToMonth(currentMonth)

        class DayViewContainer(view: View) : ViewContainer(view) {
            val textView: TextView = view.findViewById(R.id.calendarDayText)
            var day: CalendarDay? = null

            init {
                view.setOnClickListener {
                    day?.let {
                        selectedDate = it.date.toString()
                        loadMemosForDate(selectedDate!!)
                    }
                }
            }
        }

        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View): DayViewContainer {
                return DayViewContainer(view)
            }

            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.textView.text = data.date.dayOfMonth.toString()
                container.day = data
            }
        }

        // Add memo on button click
        addMemoButton.setOnClickListener {
            selectedDate?.let {
                addMemoForDate(it)
            } ?: Toast.makeText(requireContext(), "Please select a date first", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    // Load memos for a specific date from Firebase
    private fun loadMemosForDate(date: String) {
        val userId = auth.currentUser?.uid ?: return
        firebaseManager.getMemosForDate(userId, date) { memoStrings, error ->
            if (error != null) {
                Toast.makeText(requireContext(), "Error loading memos: $error", Toast.LENGTH_SHORT).show()
            } else {
                // Convert List<String> to List<Memo>
                val memos = memoStrings.map { Memo(it) }
                memoAdapter.setMemos(memos)
            }
        }
    }

    // Add a memo for a specific date to Firebase
    private fun addMemoForDate(date: String) {
        val userId = auth.currentUser?.uid ?: return
        val memoInput = EditText(requireContext()) // Add a dialog or input to get memo

        // Example: Show an input dialog for memo
        val memoText = memoInput.text.toString()
        val formatter = DateTimeFormatter.ISO_DATE
        val dateObj = LocalDate.parse(date, formatter)

        if (memoText.isNotEmpty()) {
            firebaseManager.addMemo(userId, date, memoText) { success, error ->
                if (success) {
                    Toast.makeText(requireContext(), "Memo added!", Toast.LENGTH_SHORT).show()
                    loadMemosForDate(date) // Reload memos
                } else {
                    Toast.makeText(requireContext(), "Error adding memo: $error", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Memo cannot be empty", Toast.LENGTH_SHORT).show()
        }
    }
}
