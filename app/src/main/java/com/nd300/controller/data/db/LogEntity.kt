package com.nd300.controller.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogAction {
    DISABLE_INTERNET, ENABLE_INTERNET, REBOOT, TEST_CONNECTION
}

enum class LogResult { SUCCESS, FAILURE }

@Entity(tableName = "operation_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMillis: Long,
    val action: LogAction,
    val result: LogResult,
    val failureReason: String? = null,
    val triggeredBySchedule: String? = null // اسم الجدول إن كان التنفيذ تلقائياً، أو null للتنفيذ اليدوي
)
