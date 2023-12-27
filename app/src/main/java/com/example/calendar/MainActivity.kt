package com.example.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.East
import androidx.compose.material.icons.filled.West
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

var ActualYear = Calendar.getInstance().get(Calendar.YEAR)
var ActualMonth = Calendar.getInstance().get(Calendar.MONTH)
var calendarN = ""


class MainActivity : ComponentActivity() {
    private val mainViewModel by viewModels<MainViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        lifecycleScope.launchWhenStarted {
            try {
                mainViewModel.initializeCalendarsFromFirestore()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                setContent {
                    AppContent(mainViewModel)
                }
            }
        }
    }
}

@Composable
fun AppContent(mainViewModel: MainViewModel) {
    val navController = rememberNavController()

    val db = Firebase.firestore

    var isDialogVisible by remember { mutableStateOf(false) }
    var isSelectCalendarDialogVisible by remember { mutableStateOf(false) }
    var isDeleteCalendarDialogVisible by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = "mainMenu"
    ) {
        composable("mainMenu") {
            if (isDialogVisible) {
                AlertDialog(
                    onDismissRequest = {
                        isDialogVisible = false
                    },
                    title = {
                        Text(text = "Nuevo Calendario")
                    },
                    text = {
                        CreateCalendarDialog(
                            onConfirm = { name ->
                                // Guarda el nombre del nuevo calendario en calendarN
                                calendarN = name
                                // Puedes realizar otras acciones necesarias antes de cerrar el cuadro de diálogo
                                isDialogVisible = false

                                // Navegar a la pantalla del nuevo calendario
                                navController.navigate("calendar/$calendarN")
                            },
                            onDismiss = {
                                isDialogVisible = false
                            }
                        )
                    },
                    confirmButton = {
                        // No es necesario tener un botón de confirmación aquí
                    },
                    dismissButton = {
                        // No es necesario tener un botón de cancelación aquí
                    }
                )
            }

            LaunchedEffect(Unit) {
                mainViewModel.initializeCalendarsFromFirestore()
            }

            if (isSelectCalendarDialogVisible) {
                SelectCalendarDialog(
                    calendars = mainViewModel.calendars,
                    onCalendarSelected = { selectedCalendar ->
                        // Guarda el nombre del nuevo calendario en calendarN
                        calendarN = selectedCalendar.calendarName
                        isSelectCalendarDialogVisible = false

                        navController.navigate("calendar/$calendarN")
                    },
                    onDismiss = {
                        isSelectCalendarDialogVisible = false
                    }
                )
            }

            if (isDeleteCalendarDialogVisible) {
                DeleteCalendarDialog(
                    calendars = mainViewModel.calendars,
                    onCalendarSelected = { selectedCalendar ->
                        println("SE ELIMINÓ ${selectedCalendar.calendarName}")

                        db.collection("calendarios")
                            .document(selectedCalendar.calendarName)
                            .delete()

                        navController.navigate("mainMenu")
                        isDeleteCalendarDialogVisible = false
                    },
                    onDismiss = {
                        isDeleteCalendarDialogVisible = false
                    }
                )
            }

            MainMenu(
                mainViewModel = mainViewModel,
                onCalendarSelected = {
                    mainViewModel.selectCalendar(it)
                    navController.navigate("calendar/${it.calendarName}")
                    calendarN = it.calendarName
                },
                onCreateCalendar = {
                    isDialogVisible = true
                },
                onDeleteCalendar = {
                    isDeleteCalendarDialogVisible = true
                },
                onSelectCalendar = {
                    isSelectCalendarDialogVisible = true
                }
            )
        }
        composable("calendar/{calendarId}") { backStackEntry ->
            CalendarApp(selectedCalendar = calendarN) {
                println("Se presionó back")
                navController.navigate("mainMenu")
            }
        }
    }
}

@Composable
fun SelectCalendarDialog(
    calendars: List<CalendarItem>,
    onCalendarSelected: (CalendarItem) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCalendar by remember { mutableStateOf<CalendarItem?>(null) }

    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        title = {
            Text(text = "Seleccionar Calendario")
        },
        text = {
            // Lista de calendarios para seleccionar
            LazyColumn {
                items(calendars) { calendar ->
                    CalendarItemRow(
                        calendar = calendar,
                        isSelected = selectedCalendar == calendar,
                        onCalendarSelected = {
                            selectedCalendar = calendar
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                selectedCalendar?.let {
                    onCalendarSelected(it)
                }
            }) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            Button(onClick = {
                // Restablece la selección cuando se cierra el diálogo
                selectedCalendar = null
                onDismiss()
            }) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun CalendarItemRow(
    calendar: CalendarItem,
    isSelected: Boolean,
    onCalendarSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = calendar.calendarName,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = isSelected,
            onCheckedChange = {
                onCalendarSelected()
            }
        )
    }
}

@Composable
fun DeleteCalendarDialog(
    calendars: List<CalendarItem>,
    onCalendarSelected: (CalendarItem) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCalendar by remember { mutableStateOf<CalendarItem?>(null) }

    AlertDialog(
        onDismissRequest = {
            // Puedes manejar lógica adicional al cerrar el diálogo si es necesario
            onDismiss()
        },
        title = {
            Text(text = "Eliminar Calendario")
        },
        text = {
            // Lista de calendarios para seleccionar
            LazyColumn {
                items(calendars) { calendar ->
                    CalendarItemRow(
                        calendar = calendar,
                        isSelected = selectedCalendar == calendar,
                        onCalendarSelected = {
                            selectedCalendar = calendar
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                selectedCalendar?.let {
                    onCalendarSelected(it)
                }
            }) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            Button(onClick = {
                // Restablece la selección cuando se cierra el diálogo
                selectedCalendar = null
                onDismiss()
            }) {
                Text("Cancelar")
            }
        }
    )
}
@Composable
fun CreateCalendarDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCalendarName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextField(
            value = newCalendarName,
            onValueChange = {
                newCalendarName = it
            },
            label = { Text("Nombre del nuevo calendario") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = {
                onConfirm(newCalendarName)
            }) {
                Text("Aceptar")
            }
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    }
}

@Composable
fun MainMenu(
    mainViewModel: MainViewModel,
    onCalendarSelected: (CalendarItem) -> Unit,
    onCreateCalendar: () -> Unit,
    onDeleteCalendar: () -> Unit,
    onSelectCalendar: () -> Unit
) {
    var newCalendarName by remember { mutableStateOf("") }
    var isDialogVisible by remember { mutableStateOf(false) }
    var isSelectCalendarDialogVisible by remember { mutableStateOf(false) }
    var isDeleteCalendarDialogVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Calendarios",
            fontWeight = FontWeight.Bold,
            fontSize = 50.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            textAlign = TextAlign.Center
        )

        // Botón para Crear Calendario
        Button(
            onClick = {
                isDialogVisible = true
                println("isDialogVisible: $isDialogVisible")
                println("calendarN: $calendarN")
                onCreateCalendar()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(text = "Crear Calendario")
        }

        Button(
            onClick = { isSelectCalendarDialogVisible = true
                //println("isDialogVisible: $isSelectCalendarDialogVisible")
                onSelectCalendar()},
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(text = "Seleccionar Calendario")
        }

        // Botón para Eliminar Calendario
        Button(
            onClick = { isDeleteCalendarDialogVisible = true
                        println("isDeleteCalendarDialogVisible: $isDeleteCalendarDialogVisible")
                        onDeleteCalendar()},
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(text = "Eliminar Calendario")
        }

        /*// Lista de Calendarios (puedes usar LazyColumn si hay muchos calendarios)
        mainViewModel.calendars.forEach { calendar ->

        }*/
    }
}

@Composable
fun CalendarApp(selectedCalendar: String, onNavigateBack: () -> Unit) {

    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }

    // Obtener los días marcados de la base de datos
    var markedDays by remember { mutableStateOf(emptyMap<String, Boolean>()) }
    var markedDaysPM by remember { mutableStateOf(emptyMap<String, Boolean>()) }
    var markedDaysNM by remember { mutableStateOf(emptyMap<String, Boolean>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Calendario de $calendarN",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

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
            markedDaysNM = getMarkedDaysOfNextMonth(selectedYear, selectedMonth)
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
            markedDaysNM = markedDaysNM,
            onCheckboxClick = { day, isChecked ->
                // Utiliza Pair(day, selectedMonth) como clave para actualizar el mapa
                markedDays = markedDays.toMutableMap().apply { this[day.toString()] = isChecked }
            }
        )

        Button(
            onClick = { onNavigateBack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .padding(top = 40.dp)
        ) {
            Text("Volver al Menú Principal")
        }
    }
}

@Composable
fun CalendarGrid(
    selectedMonth: Int,
    selectedYear: Int,
    selectedDate: Date?,
    onDateSelected: (Date) -> Unit,
    markedDays: Map<String, Boolean>,
    markedDaysPM: Map<String, Boolean>,
    markedDaysNM: Map<String, Boolean>,
    onCheckboxClick: (String, Boolean) -> Unit
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
                    val isChecked = markedDays[day.toString()] ?: false
                    val isCheckedPM = markedDaysPM[day.toString()] ?: false
                    val isCheckedNM = markedDaysNM[day.toString()] ?: false

                    //println("$isCurrentMonth && ${calendar.get(Calendar.DAY_OF_MONTH)} == $day")

                    DayItem(
                        day = if (day <= previousMonthDays) {
                            daysInPreviousMonth - previousMonthDays + day
                        } else if (day > daysInMonth + previousMonthDays) {
                            day - (daysInMonth + previousMonthDays)
                        } else {
                            day - previousMonthDays
                        },
                        month = if (day <= previousMonthDays) {
                            ActualMonth
                        } else if (day > daysInMonth + previousMonthDays) {
                            ActualMonth + 2
                        } else {
                            ActualMonth + 1
                        },
                        isCurrentMonth = if (day <= previousMonthDays) {
                            false
                        } else if (day > daysInMonth + previousMonthDays) {
                            false
                        } else {
                            true
                        },
                        isSelected = selectedDate?.equals(date) == true,
                        isChecked = markedDays,
                        isCheckedPM = markedDaysPM,
                        isCheckedNM = markedDaysNM,
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
    month: Int,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    isChecked: Map<String, Boolean>,
    isCheckedPM: Map<String, Boolean>,
    isCheckedNM: Map<String, Boolean>,
    onDateClick: () -> Unit,
    onCheckboxClick: (String, Boolean) -> Unit
) {
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd", Locale.getDefault())
    val currentDay = dateFormat.format(calendar.time).toInt()
    val calendar2 = Calendar.getInstance()
    val dateFormat2 = SimpleDateFormat("MM", Locale.getDefault())
    val currentMonth = dateFormat2.format(calendar2.time).toInt()
    val isToday = if(day == currentDay && (ActualMonth+1) == month && ActualMonth+1 == currentMonth) true else false
    val diacb = day.toString() + "-" + month.toString()

    //println("DIA $diacb | $isChecked | $isCheckedPM | $isCheckedNM")

    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = when {
                    isToday -> colorResource(id = R.color.todayBackground)
                    !isCurrentMonth -> colorResource(id = R.color.checkedDayBackground)
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
                    checked = isChecked[diacb] ?: false || isCheckedPM[diacb] ?: false || isCheckedNM[diacb] ?: false,
                    onCheckedChange = { newCheckedState ->
                        onCheckboxClick(diacb, newCheckedState)
                        val db = Firebase.firestore
                        val dia = day.toString() + "-" + (ActualMonth+1).toString()

                        // Guardar datos
                        val year = ActualYear.toString()
                        val month = (ActualMonth + 1).toString()

                        val nullMap = mapOf(
                            "documentId" to calendarN)

                        val isCheckedMap = mapOf(
                            "date" to dia,
                            "isChecked" to newCheckedState)

                        db.collection("calendarios")
                            .document(calendarN)
                            .set(nullMap)

                        db.collection("calendarios")
                            .document(calendarN)
                            .collection(year)
                            .document(month)
                            .collection("days")
                            .document(dia)
                            .set(isCheckedMap)
                            .addOnSuccessListener {
                                //Log.d(TAG, "Año $year Mes $month Dia $dayFormatted ACTUALIZADO")
                            }
                            .addOnFailureListener { e ->
                                //Log.w(TAG, "Error writing document", e)
                            }
                    },
                    enabled = if(isCurrentMonth) true else false,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

suspend fun getMarkedDays(year: Int, month: Int): Map<String, Boolean> {
    val db = Firebase.firestore

    val result = db.collection("calendarios")
        .document(calendarN)
        .collection(year.toString())
        .document((month + 1).toString())
        .collection("days")
        .get()
        .await()

    val markedDays = mutableMapOf<String, Boolean>()
    for (document in result) {
        val day = document.get("date")
        val isChecked = document.getBoolean("isChecked") ?: false
        if (day != null && isChecked) {
            //println("DIA = ${day}")
            markedDays[day.toString()] = isChecked
        }
    }
    //println(markedDays)

    return markedDays
}

suspend fun getMarkedDaysOfPreviousMonth(year: Int, month: Int): MutableMap<String, Boolean> {
    val db = Firebase.firestore

    val calendar = Calendar.getInstance().apply {
        set(Calendar.MONTH, ActualMonth)
        set(Calendar.YEAR, ActualYear)
        set(Calendar.DAY_OF_MONTH, 1)
        firstDayOfWeek = Calendar.MONDAY
    }

    val currentDay = ((calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7) + 1
    val currentMonth = ActualMonth
    val currentYear = ActualYear

    val previousMonthDays = (currentDay - 1).coerceAtLeast(0)
    val previousMonth = (currentMonth - 1 + 12) % 12
    val previousMonthYear = if (previousMonth == 11) currentYear - 1 else currentYear
    val daysInPreviousMonth = Calendar.getInstance().apply {
        set(Calendar.MONTH, previousMonth)
        set(Calendar.YEAR, previousMonthYear)
        set(Calendar.DAY_OF_MONTH, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)

    val minDay = (daysInPreviousMonth - previousMonthDays + 1).toString() + "-" + (currentMonth).toString()

    val result = db.collection("calendarios")
        .document(calendarN)
        .collection(year.toString())
        .document(month.toString())
        .collection("days")
        .whereGreaterThanOrEqualTo("date", minDay)
        .get()
        .await()

    val markedDays = mutableMapOf<String, Boolean>()
    for (document in result) {
        val day = document.get("date")
        val isChecked = document.getBoolean("isChecked") ?: false
        if (day != null && isChecked) {
            markedDays[day.toString()] = isChecked
        }
    }
    //println(markedDays)

    return markedDays
}

suspend fun getMarkedDaysOfNextMonth(year: Int, month: Int): MutableMap<String, Boolean> {
    val db = Firebase.firestore

    val calendar = Calendar.getInstance().apply {
        set(Calendar.MONTH, ActualMonth)
        set(Calendar.YEAR, ActualYear)
        set(Calendar.DAY_OF_MONTH, 1)
        firstDayOfWeek = Calendar.MONDAY
    }

    val currentDay = ((calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7) + 1
    val previousMonthDays = (currentDay - 1).coerceAtLeast(0)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val nextMonthDays = (7 - (daysInMonth + previousMonthDays) % 7) % 7
    val maxDay = nextMonthDays.toString() + "-" + (ActualMonth+2).toString()

    val result = db.collection("calendarios")
        .document(calendarN)
        .collection(year.toString())
        .document((month+2).toString())
        .collection("days")
        .whereLessThanOrEqualTo("date", maxDay)
        .get()
        .await()

    val markedDays = mutableMapOf<String, Boolean>()
    for (document in result) {
        val day = document.get("date")
        val isChecked = document.getBoolean("isChecked") ?: false
        if (day != null && isChecked) {
            markedDays[day.toString()] = isChecked
        }
    }
    //println(markedDays)

    return markedDays
}