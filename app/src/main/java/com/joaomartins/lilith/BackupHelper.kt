package com.joaomartins.lilith

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class BackupHelper(private val context: Context) {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    // Chave estável de 128 bits para garantir portabilidade entre dispositivos e reinstalações
    private val aesKey = "LilithSuiteSecureKey123!".toByteArray(Charsets.UTF_8).copyOf(16)

    suspend fun createBackupFile(accounts: List<Account>, tasks: List<Task>, reminders: List<Reminder>): File? = withContext(Dispatchers.IO) {
        try {
            val backupData = JsonObject()
            backupData.add("accounts", gson.toJsonTree(accounts))
            backupData.add("tasks", gson.toJsonTree(tasks))
            backupData.add("reminders", gson.toJsonTree(reminders))

            val jsonString = gson.toJson(backupData)

            // Criptografa o texto JSON puro
            val encryptedBytes = encrypt(jsonString.toByteArray(Charsets.UTF_8))
            val base64Encrypted = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)

            val file = File(context.cacheDir, "lilith_suite_backup.json")
            file.writeText(base64Encrypted)
            file
        } catch (e: Exception) {
            e.printStackTrace() // Adicionado para não ocultar erros e facilitar o debug
            null
        }
    }

    suspend fun parseBackup(uri: Uri): JsonObject? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader()?.use { it.readText() }
            if (jsonString.isNullOrEmpty()) return@withContext null

            // Lógica de compatibilidade e decodificação avançada
            val finalJson = try {
                val encryptedBytes = Base64.decode(jsonString, Base64.DEFAULT)
                try {
                    // Tenta descriptografar usando o modo seguro CBC com IV
                    val decryptedBytes = decrypt(encryptedBytes)
                    String(decryptedBytes, Charsets.UTF_8)
                } catch (cbcException: Exception) {
                    // Fallback 1: Tenta o modo ECB caso o backup tenha sido criado na versão anterior
                    val decryptedBytes = decryptEcb(encryptedBytes)
                    String(decryptedBytes, Charsets.UTF_8)
                }
            } catch (e: Exception) {
                // Fallback 2: Se falhar totalmente, assume que é um backup legado em texto puro JSON
                jsonString
            }

            val element = JsonParser.parseString(finalJson)
            if (element.isJsonObject) element.asJsonObject else null
        } catch (e: Exception) {
            e.printStackTrace() // Adicionado para não ocultar erros e facilitar o debug
            null
        }
    }

    private fun encrypt(data: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(aesKey, "AES")
        // Alterado para AES/CBC/PKCS5Padding para máxima segurança contra análise de padrões
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

        // Gera um vetor de inicialização (IV) aleatório de 16 bytes obrigatório para o modo CBC
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encrypted = cipher.doFinal(data)

        // Junta os bytes do IV na frente do conteúdo criptografado para transporte no arquivo
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

        return combined
    }

    private fun decrypt(data: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(aesKey, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

        // Extrai os 16 bytes iniciais correspondentes ao IV
        val iv = ByteArray(16)
        System.arraycopy(data, 0, iv, 0, iv.size)
        val ivSpec = IvParameterSpec(iv)

        // Extrai o restante do conteúdo que representa a cifra real
        val encryptedSize = data.size - iv.size
        val ciphertext = ByteArray(encryptedSize)
        System.arraycopy(data, iv.size, ciphertext, 0, encryptedSize)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        return cipher.doFinal(ciphertext)
    }

    // Mantido exclusivamente como ferramenta de compatibilidade para ler backups criados na criptografia antiga
    private fun decryptEcb(data: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(aesKey, "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return cipher.doFinal(data)
    }
}