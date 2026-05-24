package com.joaomartins.lilith

import android.content.Context
import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class BackupHelper(private val context: Context) {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun createBackupFile(accounts: List<Account>, tasks: List<Task>, reminders: List<Reminder>): File? = withContext(Dispatchers.IO) {
        try {
            val backupData = JsonObject()
            backupData.add("accounts", gson.toJsonTree(accounts))
            backupData.add("tasks", gson.toJsonTree(tasks))
            backupData.add("reminders", gson.toJsonTree(reminders))

            val file = File(context.cacheDir, "lilith_suite_backup.json")
            file.writeText(gson.toJson(backupData))
            file
        } catch (e: Exception) {
            null
        }
    }

    suspend fun parseBackup(uri: Uri): JsonObject? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader()?.use { it.readText() }
            if (jsonString.isNullOrEmpty()) return@withContext null
            
            val element = JsonParser.parseString(jsonString)
            if (element.isJsonObject) element.asJsonObject else null
        } catch (e: Exception) {
            null
        }
    }
}