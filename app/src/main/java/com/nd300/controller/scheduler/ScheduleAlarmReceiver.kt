package com.nd300.controller.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nd300.controller.data.db.ScheduleEntity
import java.util.concurrent.TimeUnit

class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1)
        val actionType = intent.getStringExtra(AlarmScheduler.EXTRA_ACTION_TYPE) ?: return
        val dayOfWeek = intent.getIntExtra(AlarmScheduler.EXTRA_DAY_OF_WEEK, 1)
        val hour = intent.getIntExtra(AlarmScheduler.EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTE, 0)
        val scheduleName = intent.getStringExtra(AlarmScheduler.EXTRA_SCHEDULE_NAME)

        // مهلة تنفيذ قصيرة تكفي لإطلاق WorkManager قبل أن يعلّق النظام الـ Receiver
        val pendingResult = goAsync()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val data = Data.Builder()
            .putString(RouterActionWorker.KEY_ACTION_TYPE, actionType)
            .putString(RouterActionWorker.KEY_SCHEDULE_NAME, scheduleName)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<RouterActionWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        // أعد جدولة نفس التنبيه للأسبوع القادم (نفس اليوم/الوقت) حتى يستمر الجدول أسبوعياً
        val fakeSchedule = ScheduleEntity(
            id = scheduleId,
            name = scheduleName ?: "",
            offHour = if (actionType == AlarmScheduler.ACTION_OFF) hour else 0,
            offMinute = if (actionType == AlarmScheduler.ACTION_OFF) minute else 0,
            onHour = if (actionType == AlarmScheduler.ACTION_ON) hour else 0,
            onMinute = if (actionType == AlarmScheduler.ACTION_ON) minute else 0,
            days = dayOfWeek.toString()
        )
        AlarmScheduler.scheduleOne(context, fakeSchedule, dayOfWeek, actionType, hour, minute)

        pendingResult.finish()
    }
}
