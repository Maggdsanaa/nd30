package com.nd300.controller.data.repository

import android.content.Context
import com.nd300.controller.data.db.AppDatabase
import com.nd300.controller.data.db.ScheduleEntity
import com.nd300.controller.scheduler.AlarmScheduler
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val context: Context) {
    private val dao = AppDatabase.getInstance(context).scheduleDao()

    fun observeAll(): Flow<List<ScheduleEntity>> = dao.observeAll()

    suspend fun save(schedule: ScheduleEntity) {
        dao.upsert(schedule)
        refreshAlarms()
    }

    suspend fun delete(schedule: ScheduleEntity) {
        dao.delete(schedule)
        refreshAlarms()
    }

    suspend fun refreshAlarms() {
        val enabled = dao.getAllEnabled()
        AlarmScheduler.rescheduleAll(context, enabled)
    }
}
