package com.joaomartins.lilith

import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val reminderDao: ReminderDao) {
    val allReminders: Flow<List<Reminder>> = reminderDao.getAllReminders()

    suspend fun insert(reminder: Reminder): Long {
        return reminderDao.insert(reminder)
    }

    suspend fun insertReminders(reminders: List<Reminder>) {
        reminderDao.insertAll(reminders)
    }

    suspend fun delete(reminder: Reminder) {
        reminderDao.delete(reminder)
    }

    suspend fun update(reminder: Reminder) {
        reminderDao.update(reminder)
    }
}
