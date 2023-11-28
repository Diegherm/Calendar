package com.example.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.East
import androidx.compose.material.icons.filled.West
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalendarApp()
        }
    }
}

@Composable
fun CalendarApp() {
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var checkedDays by remember { mutableStateOf(mutableMapOf<Date, Boolean>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                selectedMonth = (selectedMonth - 1 + 12) % 12 // Previous month
                if (selectedMonth == 11) {
                    selectedYear--
                }
            }) {
                Icon(Icons.Default.West, contentDescription = "Previous Month")
            }
            Text(
                text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(
                    Calendar.getInstance().apply {
                        set(Calendar.MONTH, selectedMonth)
                        set(Calendar.YEAR, selectedYear)
                    }.time
                ),
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {
                selectedMonth = (selectedMonth + 1) % 12 // Next month
                if (selectedMonth == 0) {
                    selectedYear++
                }
            }) {
                Icon(Icons.Default.East, contentDescription = "Next Month")
            }
        }

        // Weekday headers
        /*LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            for (weekday in listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")) {
                item {
                    Text(
                        text = weekday,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }*/

        // Calendar
        CalendarGrid(
            selectedMonth = selectedMonth,
            selectedYear = selectedYear,
            selectedDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
            },
            checkedDays = checkedDays,
            onCheckboxClick = { date, isChecked ->
                checkedDays[date] = isChecked
            }
        )
    }
}

@Composable
fun CalendarGrid(
    selectedMonth: Int,
    selectedYear: Int,
    selectedDate: Date?,
    onDateSelected: (Date) -> Unit,
    checkedDays: Map<Date, Boolean>,
    onCheckboxClick: (Date, Boolean) -> Unit
) {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.MONTH, selectedMonth)
        set(Calendar.YEAR, selectedYear)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val currentMonth = calendar.get(Calendar.MONTH)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    LazyColumn {
        item {
            // Weekday headers in the first row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (weekday in listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")) {
                    Text(
                        text = weekday,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        items((1..daysInMonth).toList().chunked(7)) { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (day in week) {
                    val date = calendar.time
                    val isCurrentMonth = calendar.get(Calendar.MONTH) == currentMonth
                    val isToday = isCurrentMonth && calendar.get(Calendar.DAY_OF_MONTH) == day
                    val isChecked = checkedDays[date] ?: false

                    DayItem(
                        day = day,
                        isCurrentMonth = isCurrentMonth,
                        isToday = isToday,
                        isSelected = selectedDate?.equals(date) == true,
                        isChecked = isChecked,
                        onDateClick = { onDateSelected(date) },
                        onCheckboxClick = { onCheckboxClick(date, !isChecked) }
                    )

                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
        }
    }
}

@Composable
fun DayItem(
    day: Int,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    isChecked: Boolean,
    onDateClick: () -> Unit,
    onCheckboxClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = when {
                    isSelected -> colorResource(id = R.color.selectedDayBackground)
                    isToday -> colorResource(id = R.color.todayBackground)
                    isCurrentMonth -> Color.Transparent
                    else -> colorResource(id = R.color.otherMonthDayBackground)
                }
            )
            .clickable { onDateClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (day != 0) {
                Text(text = day.toString(), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onCheckboxClick() },
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}


@Preview
@Composable
fun PreviewCalendarApp() {
    CalendarApp()
}
