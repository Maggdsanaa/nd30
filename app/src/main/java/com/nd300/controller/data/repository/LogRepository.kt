package com.nd300.controller.data.repository

import android.content.Context
import com.nd300.controller.data.db.AppDatabase
import com.nd300.controller.data.db.LogEntity
import kotlinx.coroutines.flow.Flow

class LogRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).logDao()

    fun observeRecent(): Flow<List<LogEntity>> = dao.observeRecent()

    suspend fun add(entry: LogEntity) {
        dao.insert(entry)
    }
}
