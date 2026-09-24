package com.nd300.controller.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM operation_logs ORDER BY timestampEpochMillis DESC LIMIT 500")
    fun observeRecent(): Flow<List<LogEntity>>

    @Insert
    suspend fun insert(log: LogEntity): Long

    @Query("DELETE FROM operation_logs")
    suspend fun clear()
}
