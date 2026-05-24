package com.joaomartins.lilith

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import com.google.gson.GsonBuilder

class AccountViewModel(private val repository: AccountRepository) : ViewModel() {

    // Lista de todas as contas observadas pelo Fragment
    val allAccounts: LiveData<List<Account>> = repository.allAccounts.asLiveData()

    /**
     * Insere contas com lógica de parcelamento e cálculo de datas.
     */
    fun insertWithParcels(
        description: String,
        value: Double,
        isRevenue: Boolean,
        startDateMillis: Long,
        totalParcels: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = startDateMillis
            val accountsToInsert = mutableListOf<Account>()

            for (i in 1..totalParcels) {
                val finalDescription = if (totalParcels > 1) {
                    "$description ($i/$totalParcels)"
                } else {
                    description
                }

                val account = Account(
                    description = finalDescription,
                    value = value,
                    isRevenue = isRevenue,
                    dueDate = calendar.timeInMillis,
                    currentParcel = i,
                    totalParcels = totalParcels,
                    isPaid = false
                )

                accountsToInsert.add(account)

                // Avança um mês para a próxima parcela
                calendar.add(Calendar.MONTH, 1)
            }
            
            // Inserção em lote é mais eficiente
            repository.insertAccounts(accountsToInsert)
        }
    }

    // Função para excluir uma conta específica
    fun delete(account: Account) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(account)
        }
    }

    // Função para excluir todas as parcelas de uma série
    fun deleteAllParcels(account: Account) {
        viewModelScope.launch(Dispatchers.IO) {
            val related = repository.findRelatedParcels(account)
            repository.deleteAccounts(related)
        }
    }

    // Função para excluir da parcela atual em diante
    fun deleteFromCurrentParcel(account: Account) {
        viewModelScope.launch(Dispatchers.IO) {
            val related = repository.findRelatedParcels(account)
            val filtered = related.filter { it.currentParcel >= account.currentParcel }
            repository.deleteAccounts(filtered)
        }
    }

    // Função para atualizar o status (pago/não pago)
    fun update(account: Account) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(account)
        }
    }

    /**
     * Função para exportar os dados para uma String JSON.
     */
    fun exportDataToJson(accounts: List<Account>): String {
        return GsonBuilder().setPrettyPrinting().create().toJson(accounts)
    }
}

/**
 * Factory para passar o repositório para o ViewModel
 */
class AccountViewModelFactory(private val repository: AccountRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}