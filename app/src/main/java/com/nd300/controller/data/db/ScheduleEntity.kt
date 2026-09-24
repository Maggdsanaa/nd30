package com.nd300.controller.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * جدول زمني واحد: وقت إيقاف الإنترنت ووقت تشغيله، مع الأيام المفعّلة.
 * offHour/offMinute و onHour/onMinute بصيغة 24 ساعة.
 * days: مجموعة من 1..7 حيث 1=الأحد ... طابقناها مع Calendar.SUNDAY=1.
 */
@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val offHour: Int,
    val offMinute: Int,
    val onHour: Int,
    val onMinute: Int,
    val days: String, // مثال: "1,2,3,4,5,6,7" أيام مفعّلة مفصولة بفواصل (Calendar.DAY_OF_WEEK)
    val isEnabled: Boolean = true
) {
    fun daysSet(): Set<Int> =
        if (days.isBlank()) emptySet() else days.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

    companion object {
        fun daysToString(days: Set<Int>): String = days.sorted().joinToString(",")
    }
}
