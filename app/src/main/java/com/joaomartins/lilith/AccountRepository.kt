package com.joaomartins.lilith

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow

class AccountRepository(private val accountDao: AccountDao) {

    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()

    @WorkerThread
    suspend fun insert(account: Account) {
        accountDao.insert(account)
    }

    @WorkerThread
    suspend fun delete(account: Account) {
        accountDao.delete(account)
    }

    @WorkerThread
    suspend fun deleteAccounts(accounts: List<Account>) {
        accountDao.deleteAccounts(accounts)
    }

    @WorkerThread
    suspend fun update(account: Account) {
        accountDao.update(account)
    }

    suspend fun findRelatedParcels(account: Account): List<Account> {
        // Remove o sufixo " (1/10)" da descrição para achar as outras
        val baseDescription = if (account.totalParcels > 1) {
            account.description.substringBeforeLast(" (")
        } else {
            account.description
        }
        return accountDao.findRelatedParcels(baseDescription, account.totalParcels)
    }
}