package com.joaomartins.lilith

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String?,
    val intervalMinutes: Int = 0,
    val startTime: String = "08:00",
    val endTime: String = "22:00",
    val daysOfWeek: String = "1,2,3,4,5,6,7", // 1=Dom, 7=Sab
    val isDismissed: Boolean = false,
    val isEnabled: Boolean = true
)
