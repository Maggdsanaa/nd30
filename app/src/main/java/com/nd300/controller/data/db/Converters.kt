package com.nd300.controller.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromLogAction(value: LogAction): String = value.name

    @TypeConverter
    fun toLogAction(value: String): LogAction = LogAction.valueOf(value)

    @TypeConverter
    fun fromLogResult(value: LogResult): String = value.name

    @TypeConverter
    fun toLogResult(value: String): LogResult = LogResult.valueOf(value)
}
