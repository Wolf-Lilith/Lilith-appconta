package com.joaomartins.lilith

import android.app.Application

class LilithApplication : Application() {

    // Cria a instância do banco de dados de forma preguiçosa (apenas quando for usada)
    val database by lazy { AppDatabase.getDatabase(this) }

    // Cria o repositório de contas usando o DAO do banco de dados
    val repository by lazy { AccountRepository(database.accountDao()) }

    // Cria o repositório de tarefas
    val taskRepository by lazy { TaskRepository(database.taskDao()) }
}