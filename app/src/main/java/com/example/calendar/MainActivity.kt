package com.example.calendar

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
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

    // Obtener los días marcados de la base de datos
    var markedDays by remember(selectedMonth, selectedYear) {
        mutableStateOf<Map<Date, Boolean>>(emptyMap())
    }

    LaunchedEffect(selectedMonth, selectedYear) {
        val db = Firebase.firestore
        val year = selectedYear.toString()
        val month = (selectedMonth + 1).toString() // Ajustar el mes para que coincida con el formato de Firestore

        db.collection(year)
            .document(month)
            .collection("days")
            .get()
            .addOnSuccessListener { result ->
                val newMarkedDays = mutableMapOf<Date, Boolean>()
                for (document in result) {
                    val day = document.id.toIntOrNull()
                    val isChecked = document.getBoolean("isChecked") ?: false
                    if (day != null) {
                        val calendar = Calendar.getInstance().apply {
                            set(Calendar.YEAR, selectedYear)
                            set(Calendar.MONTH, selectedMonth)
                            set(Calendar.DAY_OF_MONTH, day)
                        }
                        newMarkedDays[calendar.time] = isChecked
                    }
                }
                markedDays = newMarkedDays
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }
    }

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

        // Calendar
        println(checkedDays)
        println(markedDays)
        CalendarGrid(
            selectedMonth = selectedMonth,
            selectedYear = selectedYear,
            selectedDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
            },
            checkedDays = markedDays, // Utiliza los días marcados obtenidos de la base de datos
            onCheckboxClick = { date, isChecked ->
                Log.d("Checkbox", "Date: $date, isChecked: $isChecked")
                markedDays = markedDays.toMutableMap().apply { this[date] = isChecked }
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
        firstDayOfWeek = Calendar.MONDAY
    }

    val currentDay = ((calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7) + 1
    val currentMonth = calendar.get(Calendar.MONTH)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Calcular los días del mes anterior
    val previousMonthDays = (currentDay - 1).coerceAtLeast(0)
    val previousMonth = (selectedMonth - 1 + 12) % 12
    val previousMonthYear = if (previousMonth == 11) selectedYear - 1 else selectedYear
    val daysInPreviousMonth = Calendar.getInstance().apply {
        set(Calendar.MONTH, previousMonth)
        set(Calendar.YEAR, previousMonthYear)
        set(Calendar.DAY_OF_MONTH, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Calcular los días del mes siguiente
    val nextMonthDays = (7 - (daysInMonth + previousMonthDays) % 7) % 7



    LazyColumn {
        item {
            // Weekday headers in the first row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Ajustar el orden de los días de la semana según la nueva configuración
                for (weekday in listOf("Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom")) {
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

        items((1..daysInMonth + previousMonthDays + nextMonthDays).toList().chunked(7)) { week ->
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
                        day = if (day <= previousMonthDays) {
                            daysInPreviousMonth - previousMonthDays + day
                        } else if (day > daysInMonth + previousMonthDays) {
                            day - (daysInMonth + previousMonthDays)
                        } else {
                            day - previousMonthDays
                        },
                        isCurrentMonth = isCurrentMonth,
                        isToday = isToday,
                        isSelected = selectedDate?.equals(date) == true,
                        markedDays = checkedDays, // Cambia markedDays a checkedDays
                        onDateClick = { onDateSelected(date) },
                        onCheckboxClick = { isChecked ->
                            onCheckboxClick(date, isChecked)
                            // Aquí puedes realizar acciones adicionales si es necesario
                        }
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
    markedDays: Map<Date, Boolean>,
    onDateClick: () -> Unit,
    onCheckboxClick: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = when {
                    isSelected -> colorResource(id = R.color.selectedDayBackground)
                    isToday -> colorResource(id = R.color.todayBackground)
                    isCurrentMonth -> Color.Transparent
                    markedDays[getCurrentDate(day)] == true -> colorResource(id = R.color.checkedDayBackground)
                    else -> colorResource(id = R.color.otherMonthDayBackground)
                }
            )
            .clickable { onDateClick() }
    ) {
        var localCheckedState by remember { mutableStateOf(markedDays[getCurrentDate(day)] == true) }

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
                    checked = localCheckedState,
                    onCheckedChange = { newCheckedState ->
                        localCheckedState = newCheckedState
                        onCheckboxClick(newCheckedState)
                        // El resto de tu lógica para guardar en Firestore permanece igual
                    },
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
private fun getCurrentDate(day: Int): Date {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, day)
    }
    return calendar.time
}
