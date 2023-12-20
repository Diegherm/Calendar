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
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

var ActualYear = Calendar.getInstance().get(Calendar.YEAR)
var ActualMonth = Calendar.getInstance().get(Calendar.MONTH)

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
    var checkedDays by remember { mutableStateOf(mutableMapOf<Int, Boolean>()) }

    // Obtener los días marcados de la base de datos
    var markedDays by remember { mutableStateOf(emptyMap<Pair<Int, Int>, Boolean>()) }
    var markedDaysPM by remember { mutableStateOf(emptyMap<Pair<Int, Int>, Boolean>()) }

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
                ActualMonth = selectedMonth
                if (selectedMonth == 11) {
                    selectedYear--
                    ActualYear = selectedYear
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
                ActualMonth = selectedMonth
                if (selectedMonth == 0) {
                    selectedYear++
                    ActualYear = selectedYear
                }
            }) {
                Icon(Icons.Default.East, contentDescription = "Next Month")
            }
        }

        LaunchedEffect(selectedMonth, selectedYear) {
            markedDays = getMarkedDays(selectedYear, selectedMonth)
            markedDaysPM = getMarkedDaysOfPreviousMonth(selectedYear, selectedMonth)
        }

        // Calendar
        CalendarGrid(
            selectedMonth = selectedMonth,
            selectedYear = selectedYear,
            selectedDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
            },
            markedDays = markedDays,
            markedDaysPM = markedDaysPM,
            onCheckboxClick = { day, isChecked ->
                // Utiliza Pair(day, selectedMonth) como clave para actualizar el mapa
                markedDays = markedDays.toMutableMap().apply { this[Pair(day, selectedMonth)] = isChecked }
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
    markedDays: Map<Pair<Int, Int>, Boolean>,
    markedDaysPM: Map<Pair<Int, Int>, Boolean>,
    onCheckboxClick: (Int, Boolean) -> Unit
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
                    val isChecked = markedDays[Pair(day, selectedMonth)] ?: false
                    val isCheckedPM = markedDaysPM[Pair(day, selectedMonth)] ?: false

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
                        isChecked = markedDays,
                        isCheckedPM = markedDaysPM,
                        onDateClick = { onDateSelected(date) },
                        onCheckboxClick = { day, isChecked -> onCheckboxClick(day, isChecked) }
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
    isChecked: Map<Pair<Int, Int>, Boolean>,
    isCheckedPM: Map<Pair<Int, Int>, Boolean>,
    onDateClick: () -> Unit,
    onCheckboxClick: (Int, Boolean) -> Unit
) {
    //val isCheckedPM = isCheckedPM[Pair(day, ActualMonth-1)] ?: false
    //val isChecked = isChecked[Pair(day, ActualMonth)] ?: false
    val isPreviousMonth = day <= 0
    val actualMonth = if (isPreviousMonth) ActualMonth - 1 else ActualMonth
    val actualYear = if (isPreviousMonth && ActualMonth == 0) ActualYear - 1 else ActualYear

    println("DIA $day ICM $isCurrentMonth")

    /*val isDayChecked = if (actualMonth-1 <= actualMonth) {
        isCheckedPM[Pair(day.absoluteValue, actualMonth-1)] ?: false
    } else {
        isChecked[Pair(day, actualMonth)] ?: false
    }*/

    /*LaunchedEffect(isChecked, isCheckedPM) {
        println("Marked Days(3): $isChecked")
        println("Marked Days(3): $isCheckedPM")
    }*/

    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = when {
                    isSelected -> colorResource(id = R.color.selectedDayBackground)
                    isToday -> colorResource(id = R.color.todayBackground)
                    //isCurrentMonth && isChecked -> colorResource(id = R.color.checkedDayBackground)
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
                    checked = if (isPreviousMonth) isCheckedPM[Pair(day, actualMonth)] ?: false else isChecked[Pair(day, actualMonth)] ?: false,
                    onCheckedChange = { newCheckedState ->
                        onCheckboxClick(day, newCheckedState)
                        val db = Firebase.firestore

                        // Guardar datos
                        val year = ActualYear.toString()
                        val month = (ActualMonth + 1).toString()
                        val dayFormatted = day.toString() // Convierte el día a String
                        val isCheckedMap = mapOf(
                            "date" to day,
                            "isChecked" to newCheckedState)

                        db.collection(year)
                            .document(month)
                            .collection("days")
                            .document(dayFormatted)
                            .set(isCheckedMap)
                            .addOnSuccessListener {
                                //Log.d(TAG, "Año $year Mes $month Dia $dayFormatted ACTUALIZADO")
                            }
                            .addOnFailureListener { e ->
                                //Log.w(TAG, "Error writing document", e)
                            }
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

suspend fun getMarkedDays(year: Int, month: Int): Map<Pair<Int, Int>, Boolean> {
    val db = Firebase.firestore

    val result = db.collection(year.toString())
        .document((month + 1).toString())
        .collection("days")
        .get()
        .await()

    val markedDays = mutableMapOf<Pair<Int, Int>, Boolean>()
    for (document in result) {
        val day = document.id.toIntOrNull()
        val isChecked = document.getBoolean("isChecked") ?: false
        if (day != null && isChecked) {
            markedDays[Pair(day, month)] = isChecked
        }
    }
    println(markedDays)

    return markedDays
}

suspend fun getMarkedDaysOfPreviousMonth(year: Int, month: Int): Map<Pair<Int, Int>, Boolean> {
    val db = Firebase.firestore

    val result = db.collection(year.toString())
        .document(month.toString())
        .collection("days")
        .whereGreaterThanOrEqualTo("date", 27)
        .get()
        .await()

    val markedDays = mutableMapOf<Pair<Int, Int>, Boolean>()
    for (document in result) {
        val day = document.id.toIntOrNull()
        val isChecked = document.getBoolean("isChecked") ?: false
        if (day != null && isChecked) {
            markedDays[Pair(day, month-1)] = isChecked
        }
    }
    println(markedDays)

    return markedDays
}