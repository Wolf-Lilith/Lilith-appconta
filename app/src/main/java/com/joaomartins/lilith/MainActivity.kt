package com.joaomartins.lilith

import android.content.Intent
import android.os.Bundle
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
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.joaomartins.lilith.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { importDataFromUri(it) }
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
                prefs.edit().putString("bg_home_uri", selectedUri.toString()).apply()
                Toast.makeText(this, "Fundo atualizado!", Toast.LENGTH_SHORT).show()
                findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_home)
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

        // Adicionado nav_tasks como destino principal
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_add, R.id.nav_list, R.id.nav_tasks), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_share -> {
                    triggerExport()
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_backup -> {
                    triggerBackup()
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_import -> {
                    importBackupLauncher.launch("application/json")
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_change_bg -> {
                    pickImageLauncher.launch("image/*")
                    drawerLayout.closeDrawers()
                    true
                }
                else -> {
                    val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                    if (handled) drawerLayout.closeDrawers()
                    handled
                }
            }
        }
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

        if (currentFragment is ListAccountFragment) {
            currentFragment.shareRelatorioDeElite()
        } else {
            Toast.makeText(this, "Abra a lista para gerar o relatório.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun triggerBackup() {
        val repository = (application as LilithApplication).repository
        lifecycleScope.launch {
            try {
                val accounts = repository.allAccounts.first()
                if (accounts.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Não há dados para backup.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val gson = GsonBuilder().setPrettyPrinting().create()
                val jsonString = gson.toJson(accounts)
                val file = File(cacheDir, "lilith_backup.json")
                file.writeText(jsonString)
                val contentUri = FileProvider.getUriForFile(this@MainActivity, "${packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Salvar Backup Lilith"))
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun importDataFromUri(uri: android.net.Uri) {
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader()?.use { it.readText() }

                if (!jsonString.isNullOrEmpty()) {
                    val jsonArray = JsonParser.parseString(jsonString).asJsonArray
                    val importedAccounts = mutableListOf<Account>()

                    jsonArray.forEach { element ->
                        val obj = element.asJsonObject
                        
                        val dueDate = when {
                            obj.has("dueDate") -> obj.get("dueDate").asLong
                            obj.has("day") && obj.has("month") && obj.has("year") -> {
                                val cal = Calendar.getInstance()
                                cal.set(
                                    obj.get("year").asInt,
                                    obj.get("month").asInt,
                                    obj.get("day").asInt,
                                    12, 0, 0
                                )
                                cal.timeInMillis
                            }
                            else -> System.currentTimeMillis()
                        }

                        val account = Account(
                            description = obj.get("description").asString,
                            value = obj.get("value").asDouble,
                            isRevenue = obj.get("isRevenue").asBoolean,
                            isPaid = if (obj.has("isPaid")) obj.get("isPaid").asBoolean else false,
                            dueDate = dueDate,
                            currentParcel = if (obj.has("currentParcel")) obj.get("currentParcel").asInt else 1,
                            totalParcels = if (obj.has("totalParcels")) obj.get("totalParcels").asInt else 1
                        )
                        importedAccounts.add(account)
                    }

                    val repository = (application as LilithApplication).repository
                    importedAccounts.forEach { repository.insert(it.copy(id = 0)) }

                    Toast.makeText(this@MainActivity, "${importedAccounts.size} contas importadas!", Toast.LENGTH_LONG).show()
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_list)
                }
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