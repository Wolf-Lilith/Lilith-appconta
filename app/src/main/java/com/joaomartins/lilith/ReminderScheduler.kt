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
        
        // Verifica permissão no Android 12+
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
        
        if (isInitial) {
            // Se for inicial ou reativação, verifica se o horário atual está na faixa
            if (!isTimeInsideRange(calendar, reminder.startTime, reminder.endTime)) {
                setToStartTime(calendar, reminder.startTime)
                // Se o horário de início já passou hoje, agenda para amanhã
                if (calendar.before(Calendar.getInstance())) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            } else {
                // Se está na faixa, agenda para daqui a 10 segundos para feedback imediato
                calendar.add(Calendar.SECOND, 10)
            }
        } else {
            // Agendamento recorrente por intervalo
            if (reminder.intervalMinutes > 0) {
                calendar.add(Calendar.MINUTE, reminder.intervalMinutes)
            } else {
                return // Não recorrente
            }

            // Se o próximo pulo sair da faixa permitida (ex: passou das 22:00)
            if (!isTimeInsideRange(calendar, reminder.startTime, reminder.endTime)) {
                val nextAttemptTime = calendar.timeInMillis
                setToStartTime(calendar, reminder.startTime)
                // Se ao definir para o horário de início (ex: 08:00) ficamos no passado ou no mesmo horário, 
                // avançamos para o dia seguinte.
                if (calendar.timeInMillis <= nextAttemptTime) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                
                // Garantia extra: se ainda assim estivermos no passado, pula para amanhã
                if (calendar.before(Calendar.getInstance())) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }

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
            reminder.id + 5000, // ID diferente para não sobrescrever o principal
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
        try {
            val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val startParts = start.split(":")
            val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
            val endParts = end.split(":")
            val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
            return currentMinutes in startMinutes..endMinutes
        } catch (e: Exception) {
            return true
        }
    }
}
