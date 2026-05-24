package com.joaomartins.lilith

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra("reminder_id", -1)
        val action = intent.action

        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)

                when (action) {
                    Intent.ACTION_BOOT_COMPLETED, "android.intent.action.QUICKBOOT_POWERON" -> {
                        rescheduleAllRemindersSync(context, database)
                    }
                    "ACTION_SNOOZE" -> {
                        if (reminderId != -1) snoozeReminderSync(context, database, reminderId)
                    }
                    "ACTION_DISMISS" -> {
                        if (reminderId != -1) dismissReminderSync(context, database, reminderId)
                    }
                    else -> {
                        if (reminderId != -1) {
                            val reminder = database.reminderDao().getReminderById(reminderId)
                            
                            if (reminder != null && reminder.isEnabled) {
                                if (reminder.isDismissed) {
                                    database.reminderDao().update(reminder.copy(isDismissed = false))
                                }

                                // Verificação de horário aprimorada (suporta virada de meia-noite)
                                if (isWithinTimeRange(reminder.startTime, reminder.endTime)) {
                                    withContext(Dispatchers.Main) {
                                        showNotification(context, reminder)
                                    }
                                }
                                
                                if (reminder.intervalMinutes > 0) {
                                    ReminderScheduler.scheduleNext(context, reminder)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleAllRemindersSync(context: Context, database: AppDatabase) {
        val allReminders = database.reminderDao().getAllRemindersSync()
        allReminders.forEach { reminder ->
            if (reminder.isEnabled) {
                ReminderScheduler.scheduleNext(context, reminder, isInitial = true)
            }
        }
    }

    private suspend fun snoozeReminderSync(context: Context, database: AppDatabase, reminderId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId)
        val reminder = database.reminderDao().getReminderById(reminderId)
        reminder?.let { 
            ReminderScheduler.scheduleSnooze(context, it)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Soneca de 5 minutos ativada.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun dismissReminderSync(context: Context, database: AppDatabase, reminderId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId)
        val reminder = database.reminderDao().getReminderById(reminderId)
        reminder?.let { 
            database.reminderDao().update(it.copy(isDismissed = true))
        }
    }

    private fun isWithinTimeRange(start: String, end: String): Boolean {
        return try {
            val now = Calendar.getInstance()
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            
            val startParts = start.split(":")
            val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
            
            val endParts = end.split(":")
            val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
            
            if (startMinutes <= endMinutes) {
                currentMinutes in startMinutes..endMinutes
            } else {
                // Suporte para virada de meia-noite (ex: 22h às 02h)
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }
        } catch (e: Exception) {
            true
        }
    }

    private fun showNotification(context: Context, reminder: Reminder) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "lilith_reminders_v2" 
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) 
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(channelId, "Lembretes Lilith", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notificações de lembretes e alarmes"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(soundUri, attributes)
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPI = PendingIntent.getActivity(context, reminder.id + 3000, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("reminder_id", reminder.id)
        }
        val snoozePI = PendingIntent.getBroadcast(context, reminder.id + 1000, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val dismissIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "ACTION_DISMISS"
            putExtra("reminder_id", reminder.id)
        }
        val dismissPI = PendingIntent.getBroadcast(context, reminder.id + 2000, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(reminder.title)
            .setContentText(reminder.description ?: "Hora do seu lembrete!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setContentIntent(mainPI)
            .addAction(android.R.drawable.ic_menu_recent_history, "Soneca", snoozePI)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "OK", dismissPI)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(mainPI, true)

        notificationManager.notify(reminder.id, builder.build())
    }
}
