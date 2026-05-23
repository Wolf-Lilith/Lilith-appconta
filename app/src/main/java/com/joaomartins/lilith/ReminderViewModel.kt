package com.joaomartins.lilith

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class ReminderViewModel(application: Application, private val repository: ReminderRepository) : AndroidViewModel(application) {

    val allReminders: LiveData<List<Reminder>> = repository.allReminders.asLiveData()

    fun insert(reminder: Reminder) = viewModelScope.launch {
        val id = repository.insert(reminder)
        val insertedReminder = reminder.copy(id = id.toInt())
        // Aciona o primeiro alarme quase imediatamente (10s) para teste e início do ciclo
        ReminderScheduler.scheduleNext(getApplication(), insertedReminder, isInitial = true)
    }

    fun delete(reminder: Reminder) = viewModelScope.launch {
        repository.delete(reminder)
    }

    fun update(reminder: Reminder) = viewModelScope.launch {
        repository.update(reminder)
        if (reminder.isEnabled) {
            // Se reativar, agenda o próximo ciclo
            ReminderScheduler.scheduleNext(getApplication(), reminder, isInitial = true)
        }
    }

    fun insertDefaultWaterReminder() = viewModelScope.launch {
        val current = allReminders.value
        if (current.isNullOrEmpty() || current.none { it.title.contains("Água", ignoreCase = true) }) {
            insert(
                Reminder(
                    title = "Beber Água",
                    description = "Hidrate-se! 60 minutos se passaram.",
                    intervalMinutes = 60,
                    startTime = "08:00",
                    endTime = "22:00"
                )
            )
        }
    }
}

class ReminderViewModelFactory(private val application: Application, private val repository: ReminderRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReminderViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
