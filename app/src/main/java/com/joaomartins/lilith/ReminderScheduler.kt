package com.joaomartins.lilith

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.*

object ReminderScheduler {

    private const val TAG = "ReminderScheduler"

    fun scheduleNext(context: Context, reminder: Reminder, isInitial: Boolean = false) {
        if (!reminder.isEnabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms: Permission missing")
                return
            }
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        if (isInitial) {
            // Se o horário atual já passou do fim ou ainda não chegou no início
            if (!isTimeInsideRange(calendar, reminder.startTime, reminder.endTime)) {
                setToStartTime(calendar, reminder.startTime)
                // Se o horário de início já passou hoje, pula para o próximo dia válido
                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            } else {
                // Se está dentro da faixa, agendamos um pequeno delay para feedback
                calendar.add(Calendar.SECOND, 10)
            }
        } else {
            // Recorrência normal
            if (reminder.intervalMinutes > 0) {
                calendar.add(Calendar.MINUTE, reminder.intervalMinutes)
            } else {
                return
            }

            // Se o pulo saiu da faixa (passou das 22h por exemplo)
            if (!isTimeInsideRange(calendar, reminder.startTime, reminder.endTime)) {
                setToStartTime(calendar, reminder.startTime)
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Garantir que o agendamento caia em um dia da semana selecionado
        ensureValidDay(calendar, reminder.daysOfWeek)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled reminder ${reminder.id} for ${calendar.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm", e)
        }
    }

    private fun ensureValidDay(cal: Calendar, daysOfWeekStr: String) {
        val selectedDays = daysOfWeekStr.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (selectedDays.isEmpty()) return

        var safetyNet = 0
        // Enquanto o dia da semana atual (Calendar.DAY_OF_WEEK) não estiver na lista, pula pro próximo dia
        while (!selectedDays.contains(cal.get(Calendar.DAY_OF_WEEK)) && safetyNet < 8) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            safetyNet++
        }
    }

    private fun setToStartTime(cal: Calendar, startTime: String) {
        try {
            val parts = startTime.split(":")
            cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            cal.set(Calendar.MINUTE, parts[1].toInt())
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid time format: $startTime", e)
        }
    }

    fun scheduleSnooze(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id + 5000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 5)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun isTimeInsideRange(cal: Calendar, start: String, end: String): Boolean {
        return try {
            val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val startParts = start.split(":")
            val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
            val endParts = end.split(":")
            val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()

            if (startMinutes <= endMinutes) {
                currentMinutes in startMinutes..endMinutes
            } else {
                // Caso atravesse a meia-noite (ex: 22h às 02h)
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }
        } catch (e: Exception) {
            true
        }
    }
}