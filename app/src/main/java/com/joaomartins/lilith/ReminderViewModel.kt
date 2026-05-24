package com.joaomartins.lilith

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class ReminderViewModel(application: Application, private val repository: ReminderRepository) : AndroidViewModel(application) {

    val allReminders: LiveData<List<Reminder>> = repository.allReminders.asLiveData()

    fun insert(reminder: Reminder) = viewModelScope.launch {
        val id = repository.insert(reminder)
        val insertedReminder = reminder.copy(id = id.toInt())
        ReminderScheduler.scheduleNext(getApplication(), insertedReminder, isInitial = true)
    }

    fun delete(reminder: Reminder) = viewModelScope.launch {
        repository.delete(reminder)
    }

    fun update(reminder: Reminder) = viewModelScope.launch {
        repository.update(reminder)
        if (reminder.isEnabled) {
            ReminderScheduler.scheduleNext(getApplication(), reminder, isInitial = true)
        }
    }

    /**
     * Insere o lembrete de água apenas uma vez na vida do app.
     * Se o usuário deletar, não volta mais automaticamente.
     */
    fun checkAndInsertDefaultWaterReminder() = viewModelScope.launch {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("lilith_prefs", Context.MODE_PRIVATE)
        val alreadyCreated = sharedPrefs.getBoolean("default_water_created", false)
        
        if (!alreadyCreated) {
            insert(
                Reminder(
                    title = "Beber Água",
                    description = "Hidrate-se! 60 minutos se passaram.",
                    intervalMinutes = 60,
                    startTime = "08:00",
                    endTime = "22:00",
                    daysOfWeek = "1,2,3,4,5,6,7"
                )
            )
            sharedPrefs.edit().putBoolean("default_water_created", true).apply()
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
