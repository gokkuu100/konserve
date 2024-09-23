package com.example.konserve

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.CalendarDay
import com.applandeo.materialcalendarview.listeners.OnCalendarDayClickListener
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar
import kotlin.collections.ArrayList


class CalendarFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var memoRecyclerView: RecyclerView
    private lateinit var memoAdapter: MemoAdapter
    private lateinit var firebaseManager: FirebaseManager

    private lateinit var addMemoButton: FloatingActionButton
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private var selectedCalendar: Calendar? = null
    private val eventDays = ArrayList<CalendarDay>()
    private var memoListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.calendar_fragment, container, false)

        calendarView = view.findViewById(R.id.calendarView)
        memoRecyclerView = view.findViewById(R.id.memoRecyclerView)
        addMemoButton = view.findViewById(R.id.addMemoButton)

        firebaseManager = FirebaseManager(auth, firestore)

        memoRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        memoAdapter = MemoAdapter()
        memoRecyclerView.adapter = memoAdapter

        calendarView.setOnCalendarDayClickListener(object : OnCalendarDayClickListener {
            override fun onClick(calendarDay: CalendarDay) {
                selectedCalendar = calendarDay.calendar
                val dateString = "${selectedCalendar!!.get(Calendar.YEAR)}-${selectedCalendar!!.get(Calendar.MONTH) + 1}-${selectedCalendar!!.get(Calendar.DAY_OF_MONTH)}"
                loadMemosForDate(dateString)
            }
        })

        addMemoButton.setOnClickListener {
            selectedCalendar?.let {
                val dateString = "${it.get(Calendar.YEAR)}-${it.get(Calendar.MONTH) + 1}-${it.get(Calendar.DAY_OF_MONTH)}"
                addMemoForDate(dateString)
            } ?: Toast.makeText(requireContext(), "Please select a date first", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun loadMemosForDate(date: String) {
        val userId = auth.currentUser?.uid ?: return
        firebaseManager.getMemosForDate(userId, date) { memoList, error ->
            if (error != null) {
                Toast.makeText(requireContext(), "Error loading memos: $error", Toast.LENGTH_SHORT).show()
            } else {
                memoAdapter.setMemos(memoList)
            }
        }
    }

    private fun addMemoForDate(date: String) {
        val userId = auth.currentUser?.uid ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_memo, null)
        val memoTitleInput = dialogView.findViewById<EditText>(R.id.memoTitleInput)
        val memoContentInput = dialogView.findViewById<EditText>(R.id.memoContentInput)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.saveMemoButton).setOnClickListener {
            val memoTitle = memoTitleInput.text.toString()
            val memoContent = memoContentInput.text.toString()

            if (memoTitle.isNotEmpty() && memoContent.isNotEmpty()) {
                firebaseManager.addMemo(userId, date, memoTitle, memoContent) { success, error ->
                    if (success) {
                        Toast.makeText(requireContext(), "Memo added!", Toast.LENGTH_SHORT).show()
                        loadMemosForDate(date)
                        selectedCalendar?.let {
                            val calendarDay = CalendarDay(it)
                            eventDays.add(calendarDay)
                            calendarView.setCalendarDays(eventDays)
                        }
                    } else {
                        Log.e("CalendarFragment", "Error adding memo: $error")
                        Toast.makeText(requireContext(), "Error adding memo: $error", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Memo title and content cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<Button>(R.id.cancelMemoButton).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }


    override fun onStart() {
        super.onStart()
        val userId = auth.currentUser?.uid ?: return
        selectedCalendar?.let {
            val selectedDate = "${it.get(Calendar.YEAR)}-${it.get(Calendar.MONTH) + 1}-${it.get(Calendar.DAY_OF_MONTH)}"
            memoListener = firebaseManager.getMemosForDate(userId, selectedDate) { memoList, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Error loading memos: $error", Toast.LENGTH_SHORT).show()
                } else {
                    memoAdapter.setMemos(memoList)
                }
            }
        } ?: Toast.makeText(requireContext(), "Please select a date first", Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        memoListener?.remove()
    }
}
