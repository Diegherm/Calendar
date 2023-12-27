package com.example.calendar

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MainViewModel : ViewModel() {
    private val _calendars = mutableStateListOf<CalendarItem>()
    val calendars: List<CalendarItem> get() = _calendars

    private val _selectedCalendar = mutableStateOf<CalendarItem?>(null)
    val selectedCalendar: CalendarItem? get() = _selectedCalendar.value

    fun addCalendar(calendar: CalendarItem) {
        _calendars.add(calendar)
    }
    fun removeCalendar(calendar: CalendarItem) {
        _calendars.remove(calendar)
    }

    fun selectCalendar(calendar: CalendarItem) {
        _selectedCalendar.value = calendar
    }

    suspend fun initializeCalendarsFromFirestore() {
        try {
            // Realiza la consulta a Firestore para obtener los nombres de los calendarios
            val calendarNames = getCalendarNamesFromFirestore()

            // Limpia la lista actual y agrega los nuevos calendarios
            _calendars.clear()
            calendarNames.forEach { name ->
                _calendars.add(CalendarItem(calendarName = name))
            }
        } catch (e: Exception) {
            // Manejar errores, como problemas de red o acceso a Firestore
            e.printStackTrace()
        }
    }

    private suspend fun getCalendarNamesFromFirestore(): List<String> {
        val db = FirebaseFirestore.getInstance()

        return try {
            val result = db.collection("calendarios").get().await()

            result.documents.map { document ->
                document.id
            }

        } catch (e: Exception) {
            println("Error al obtener nombres de calendarios: $e")
            e.printStackTrace()
            emptyList()
        }
    }

    private fun generateCalendarId(): String {
        // Implementa la lógica para generar un ID único (puedes ajustar esto según tus necesidades)
        return System.currentTimeMillis().toString()
    }
}