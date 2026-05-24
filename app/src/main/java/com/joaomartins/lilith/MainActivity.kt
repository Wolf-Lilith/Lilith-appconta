package com.joaomartins.lilith

import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Menu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.gson.GsonBuilder
import com.joaomartins.lilith.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val backupHelper by lazy { BackupHelper(this) }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Avisos de lembretes desativados.", Toast.LENGTH_SHORT).show()
        }
    }

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { importFullBackup(it) }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            try {
                contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val prefs = getSharedPreferences("lilith_prefs", MODE_PRIVATE)
                prefs.edit().putString("bg_global_uri", selectedUri.toString()).apply()
                updateGlobalBackground(selectedUri)
                Toast.makeText(this, "Fundo atualizado!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao processar imagem.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_list, R.id.nav_tasks, R.id.nav_reminders), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        loadGlobalBackground()
        checkAndRequestPermissions()
        handleIntent(intent)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val menu = navView.menu
            when (destination.id) {
                R.id.nav_home -> setMenuGroupsVisibility(menu, finance = false, tasks = false, reminders = false)
                R.id.nav_list, R.id.nav_add -> setMenuGroupsVisibility(menu, finance = true, tasks = false, reminders = false)
                R.id.nav_tasks -> setMenuGroupsVisibility(menu, finance = false, tasks = true, reminders = false)
                R.id.nav_reminders -> setMenuGroupsVisibility(menu, finance = false, tasks = false, reminders = true)
            }
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> { navController.navigate(R.id.nav_home); drawerLayout.closeDrawers(); true }
                R.id.nav_share -> { triggerExport(); drawerLayout.closeDrawers(); true }
                R.id.nav_backup -> { triggerFullBackup() ; drawerLayout.closeDrawers(); true }
                R.id.nav_import -> { importBackupLauncher.launch("*/*"); drawerLayout.closeDrawers(); true }
                R.id.nav_change_bg -> { pickImageLauncher.launch("image/*"); drawerLayout.closeDrawers(); true }
                R.id.nav_reset_bg -> {
                    getSharedPreferences("lilith_prefs", MODE_PRIVATE).edit().remove("bg_global_uri").apply()
                    binding.imgGlobalBackground.setImageResource(R.drawable.logo_lilith)
                    binding.imgGlobalBackground.alpha = 1.0f
                    Toast.makeText(this, "Fundo padrão restaurado.", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawers(); true
                }
                else -> {
                    val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                    if (handled) drawerLayout.closeDrawers()
                    handled
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            if (it.getStringExtra("navigate_to") == "call_blocker") {
                findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_call_blocker)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                showBatteryDialog()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                showExactAlarmDialog()
            }
        }
    }

    private fun showBatteryDialog() {
        AlertDialog.Builder(this)
            .setTitle("Lembretes Precisos")
            .setMessage("Para que seus alarmes toquem sempre, desative a economia de energia para o Lilith.")
            .setPositiveButton("Configurar") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    })
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
            .setNegativeButton("Depois", null).show()
    }

    private fun showExactAlarmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissão de Alarme")
            .setMessage("O Lilith precisa de permissão para definir alarmes exatos e recorrentes.")
            .setPositiveButton("Autorizar") { _, _ ->
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
            .setNegativeButton("Depois", null).show()
    }

    private fun setMenuGroupsVisibility(menu: Menu, finance: Boolean, tasks: Boolean, reminders: Boolean) {
        menu.setGroupVisible(R.id.group_finance, finance)
        menu.setGroupVisible(R.id.group_tasks, tasks)
        menu.setGroupVisible(R.id.group_reminders, reminders)
    }

    private fun loadGlobalBackground() {
        val prefs = getSharedPreferences("lilith_prefs", MODE_PRIVATE)
        val bgUriString = prefs.getString("bg_global_uri", null)
        if (bgUriString != null) {
            try {
                val uri = Uri.parse(bgUriString)
                contentResolver.openInputStream(uri)?.close()
                updateGlobalBackground(uri)
            } catch (e: Exception) {
                binding.imgGlobalBackground.setImageResource(R.drawable.logo_lilith)
                binding.imgGlobalBackground.alpha = 1.0f
                prefs.edit().remove("bg_global_uri").apply()
                Toast.makeText(this, "A imagem de fundo não está mais disponível.", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.imgGlobalBackground.setImageResource(R.drawable.logo_lilith)
            binding.imgGlobalBackground.alpha = 1.0f
        }
    }

    private fun updateGlobalBackground(uri: Uri) {
        binding.imgGlobalBackground.setImageURI(uri)
        binding.imgGlobalBackground.alpha = 1.0f
    }

    private fun triggerExport() {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        if (navController.currentDestination?.id != R.id.nav_list) {
            navController.navigate(R.id.nav_list)
            binding.root.postDelayed({ executeShare() }, 500)
        } else {
            executeShare()
        }
    }

    private fun executeShare() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.find { it is ListAccountFragment }
        if (currentFragment is ListAccountFragment) currentFragment.shareRelatorioDeElite()
        else Toast.makeText(this, "Abra a lista para gerar o relatório.", Toast.LENGTH_SHORT).show()
    }

    private fun triggerFullBackup() {
        val lilithApp = application as LilithApplication
        lifecycleScope.launch {
            try {
                val accounts = lilithApp.repository.allAccounts.first()
                val tasks = lilithApp.taskRepository.allTasks.first()
                val reminders = lilithApp.reminderRepository.allReminders.first()
                
                val file = backupHelper.createBackupFile(accounts, tasks, reminders)
                
                if (file != null) {
                    val contentUri = FileProvider.getUriForFile(this@MainActivity, "${packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, "Salvar Backup Lilith"))
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Erro no backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun importFullBackup(uri: Uri) {
        lifecycleScope.launch {
            try {
                val root = backupHelper.parseBackup(uri) ?: return@launch
                val gson = GsonBuilder().create()
                val lilithApp = (application as LilithApplication)
                
                // Importar Contas com Lógica de Compatibilidade
                if (root.has("accounts")) {
                    val accounts = root.getAsJsonArray("accounts").map { jsonElem ->
                        val obj = jsonElem.asJsonObject
                        var dueDate = if (obj.has("dueDate")) obj.get("dueDate").asLong else 0L
                        
                        // Compatibilidade: Se não tem dueDate, mas tem day/month/year (versão antiga)
                        if (dueDate == 0L && obj.has("day") && obj.has("month") && obj.has("year")) {
                            val cal = Calendar.getInstance()
                            cal.set(obj.get("year").asInt, obj.get("month").asInt, obj.get("day").asInt)
                            dueDate = cal.timeInMillis
                        }

                        Account(
                            description = obj.get("description").asString,
                            value = obj.get("value").asDouble,
                            isRevenue = obj.get("isRevenue").asBoolean,
                            isPaid = if (obj.has("isPaid")) obj.get("isPaid").asBoolean else false,
                            dueDate = dueDate,
                            currentParcel = if (obj.has("currentParcel")) obj.get("currentParcel").asInt else 1,
                            totalParcels = if (obj.has("totalParcels")) obj.get("totalParcels").asInt else 1
                        )
                    }
                    lilithApp.repository.insertAccounts(accounts)
                }
                
                // Importar Tarefas com Compatibilidade
                if (root.has("tasks")) {
                    val tasks = root.getAsJsonArray("tasks").map { jsonElem ->
                        val obj = jsonElem.asJsonObject
                        Task(
                            id = 0,
                            title = obj.get("title").asString,
                            isCompleted = if (obj.has("isCompleted")) obj.get("isCompleted").asBoolean else false,
                            createdAt = if (obj.has("createdAt")) obj.get("createdAt").asLong else System.currentTimeMillis()
                        )
                    }
                    lilithApp.taskRepository.insertTasks(tasks)
                }

                // Importar Lembretes com Compatibilidade
                if (root.has("reminders")) {
                    root.getAsJsonArray("reminders").forEach { jsonElem ->
                        var reminder = gson.fromJson(jsonElem, Reminder::class.java).copy(id = 0)
                        if (reminder.daysOfWeek.isNullOrEmpty()) {
                            reminder = reminder.copy(daysOfWeek = "1,2,3,4,5,6,7")
                        }
                        val newId = lilithApp.reminderRepository.insert(reminder)
                        if (reminder.isEnabled) {
                            ReminderScheduler.scheduleNext(this@MainActivity, reminder.copy(id = newId.toInt()), isInitial = true)
                        }
                    }
                }
                
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Sucesso")
                    .setMessage("Dados importados com sucesso!")
                    .setPositiveButton("OK", null)
                    .show()

            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Erro ao importar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}