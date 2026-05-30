package com.joaomartins.lilith

import android.app.NotificationManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.joaomartins.lilith.databinding.ActivityReminderAlertBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReminderAlertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReminderAlertBinding
    private var reminderId: Int = -1
    private var currentReminder: Reminder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuração moderna para fazer a Activity acordar a tela e aparecer por cima do bloqueio
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityReminderAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reminderId = intent.getIntExtra("reminder_id", -1)

        loadReminderData()

        binding.btnAlertOk.setOnClickListener {
            dismissReminder()
        }

        binding.btnAlertSnooze.setOnClickListener {
            snoozeReminder()
        }
    }

    private fun loadReminderData() {
        if (reminderId == -1) return

        lifecycleScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(this@ReminderAlertActivity)
            val reminder = database.reminderDao().getReminderById(reminderId)
            if (reminder != null) {
                currentReminder = reminder
                withContext(Dispatchers.Main) {
                    binding.tvAlertTitle.text = reminder.title
                    binding.tvAlertDescription.text = reminder.description ?: "Hora do seu lembrete!"
                }
            }
        }
    }

    private fun dismissReminder() {
        if (reminderId == -1) return

        lifecycleScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(this@ReminderAlertActivity)
            currentReminder?.let {
                database.reminderDao().update(it.copy(isDismissed = true))
            }

            // Cancela a notificação pendente da barra de status (qualificador Context removido)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(reminderId)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@ReminderAlertActivity, "Lembrete concluído.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun snoozeReminder() {
        if (reminderId == -1) return

        lifecycleScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(this@ReminderAlertActivity)
            val reminder = database.reminderDao().getReminderById(reminderId)
            reminder?.let {
                ReminderScheduler.scheduleSnooze(this@ReminderAlertActivity, it)

                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(reminderId)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ReminderAlertActivity, "Soneca de 5 minutos ativada.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    // Intercepta os botões físicos de Volume+ e Volume- e os faz agir como o botão OK
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            dismissReminder()
            return true // Informa ao sistema que o clique do botão já foi processado por nós
        }
        return super.onKeyDown(keyCode, event)
    }
}