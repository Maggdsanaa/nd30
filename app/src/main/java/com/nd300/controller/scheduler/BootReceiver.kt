package com.nd300.controller.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nd300.controller.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * يعيد بناء جميع التنبيهات المجدولة بعد:
 *  - إعادة تشغيل الهاتف (BOOT_COMPLETED)
 *  - تحديث التطبيق (MY_PACKAGE_REPLACED)
 *  - تغيير الوقت أو المنطقة الزمنية يدوياً (TIME_SET / TIMEZONE_CHANGED)
 * لأن AlarmManager يفقد كل التنبيهات غير المستمرة (non-persisted) في هذه الحالات.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(appContext)
                val schedules = db.scheduleDao().getAllEnabled()
                AlarmScheduler.rescheduleAll(appContext, schedules)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
