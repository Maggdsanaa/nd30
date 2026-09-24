package com.nd300.controller.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nd300.controller.data.db.ScheduleEntity
import java.util.Calendar

/**
 * يجدول تنبيهات دقيقة (Exact Alarms) لكل جدول × كل يوم مفعّل × (إيقاف/تشغيل).
 * كل تنبيه يُعيد جدولة نفسه للأسبوع التالي فور تنفيذه (راجع ScheduleAlarmReceiver)
 * حتى يستمر العمل رغم إغلاق واجهة التطبيق أو إعادة تشغيل الهاتف (عبر BootReceiver).
 */
object AlarmScheduler {

    const val ACTION_OFF = "ACTION_DISABLE_INTERNET"
    const val ACTION_ON = "ACTION_ENABLE_INTERNET"

    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    const val EXTRA_ACTION_TYPE = "extra_action_type"
    const val EXTRA_DAY_OF_WEEK = "extra_day_of_week"
    const val EXTRA_HOUR = "extra_hour"
    const val EXTRA_MINUTE = "extra_minute"
    const val EXTRA_SCHEDULE_NAME = "extra_schedule_name"

    fun rescheduleAll(context: Context, schedules: List<ScheduleEntity>) {
        cancelAll(context, schedules)
        schedules.filter { it.isEnabled }.forEach { schedule ->
            schedule.daysSet().forEach { dayOfWeek ->
                scheduleOne(context, schedule, dayOfWeek, ACTION_OFF, schedule.offHour, schedule.offMinute)
                scheduleOne(context, schedule, dayOfWeek, ACTION_ON, schedule.onHour, schedule.onMinute)
            }
        }
    }

    fun scheduleOne(
        context: Context,
        schedule: ScheduleEntity,
        dayOfWeek: Int,
        actionType: String,
        hour: Int,
        minute: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextTriggerTimeMillis(dayOfWeek, hour, minute)

        val intent = buildIntent(context, schedule.id, actionType, dayOfWeek, hour, minute, schedule.name)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(schedule.id, dayOfWeek, actionType),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelAll(context: Context, schedules: List<ScheduleEntity>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        schedules.forEach { schedule ->
            for (day in 1..7) {
                for (action in listOf(ACTION_OFF, ACTION_ON)) {
                    val intent = buildIntent(context, schedule.id, action, day, 0, 0, schedule.name)
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode(schedule.id, day, action),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.cancel(pendingIntent)
                }
            }
        }
    }

    private fun buildIntent(
        context: Context,
        scheduleId: Long,
        actionType: String,
        dayOfWeek: Int,
        hour: Int,
        minute: Int,
        name: String
    ): Intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
        putExtra(EXTRA_SCHEDULE_ID, scheduleId)
        putExtra(EXTRA_ACTION_TYPE, actionType)
        putExtra(EXTRA_DAY_OF_WEEK, dayOfWeek)
        putExtra(EXTRA_HOUR, hour)
        putExtra(EXTRA_MINUTE, minute)
        putExtra(EXTRA_SCHEDULE_NAME, name)
    }

    /** رقم طلب فريد لكل (جدول × يوم × نوع أمر) لتفادي تعارض الـ PendingIntent. */
    private fun requestCode(scheduleId: Long, dayOfWeek: Int, actionType: String): Int {
        val actionBit = if (actionType == ACTION_OFF) 0 else 1
        return ((scheduleId.toInt() and 0xFFFF) * 100) + (dayOfWeek * 2) + actionBit
    }

    /** يحسب أقرب توقيت مستقبلي (RTC) يطابق اليوم والساعة والدقيقة المطلوبة. */
    fun nextTriggerTimeMillis(dayOfWeek: Int, hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 7)
        }
        return target.timeInMillis
    }
}
