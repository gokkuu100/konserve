package com.example.konserve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.MonthDayBinder
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale


class CalendarFragment : Fragment() {

    private lateinit var calendarView: CalendarView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.calendar_fragment, container, false)
        calendarView = view.findViewById(R.id.calendarView)

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

        // Setup the calendar view
        calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        calendarView.scrollToMonth(currentMonth)

        class DayViewContainer(view: View) : ViewContainer(view) {
            val textView: TextView = view.findViewById(R.id.calendarDayText)
        }

        // Set up the MonthDayBinder for the CalendarView
        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            // This function is called only when a new view container is needed
            override fun create(view: View): DayViewContainer {
                return DayViewContainer(view)
            }

            // This function binds data (date) to each view container
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.textView.text = data.date.dayOfMonth.toString()
            }
        }

        return view
    }
}
