package com.joaomartins.lilith

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String?,
    val intervalMinutes: Int = 0, // 0 significa que não é recorrente por intervalo
    val startTime: String = "08:00",
    val endTime: String = "22:00",
    val lastTriggered: Long = 0,
    val isDismissed: Boolean = false, // Fica vermelho se foi desativado/concluído no ciclo atual
    val isEnabled: Boolean = true
)
