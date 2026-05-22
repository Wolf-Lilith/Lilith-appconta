package com.joaomartins.lilith

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Delete
    suspend fun deleteAccounts(accounts: List<Account>)

    @Update
    suspend fun update(account: Account)

    @Query("SELECT * FROM accounts ORDER BY dueDate ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE dueDate >= :startMillis AND dueDate <= :endMillis")
    fun getAccountsByPeriod(startMillis: Long, endMillis: Long): Flow<List<Account>>

    // Busca todas as parcelas que parecem pertencer à mesma série
    @Query("SELECT * FROM accounts WHERE totalParcels = :total AND description LIKE :baseDesc || '%'")
    suspend fun findRelatedParcels(baseDesc: String, total: Int): List<Account>
}