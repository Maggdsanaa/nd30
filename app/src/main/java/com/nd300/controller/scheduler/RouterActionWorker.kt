package com.nd300.controller.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nd300.controller.data.db.AppDatabase
import com.nd300.controller.data.db.LogAction
import com.nd300.controller.data.db.LogEntity
import com.nd300.controller.data.db.LogResult
import com.nd300.controller.data.security.SecureStorage
import com.nd300.controller.network.RouterActionResult
import com.nd300.controller.network.RouterConfig
import com.nd300.controller.network.RouterController
import com.nd300.controller.network.TemplateJson
import com.nd300.controller.notifications.NotificationHelper

/**
 * ينفّذ أمراً واحداً فعلياً على المودم (إيقاف/تشغيل الإنترنت)، مع إعادة محاولة تلقائية
 * (سياسة Backoff من WorkManager) عند فشل الشبكة أو تسجيل الدخول، ثم يسجّل النتيجة
 * في قاعدة البيانات ويُرسل إشعاراً للمستخدم.
 */
class RouterActionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val actionType = inputData.getString(KEY_ACTION_TYPE) ?: return Result.failure()
        val scheduleName = inputData.getString(KEY_SCHEDULE_NAME)

        val secureStorage = SecureStorage(applicationContext)
        val templates = TemplateJson.decode(secureStorage.requestTemplatesJson)
        val config = RouterConfig(
            ip = secureStorage.routerIp,
            port = secureStorage.routerPort,
            username = secureStorage.username,
            password = secureStorage.password,
            templates = templates
        )
        val controller = RouterController(config)

        val result = when (actionType) {
            AlarmScheduler.ACTION_OFF -> controller.disableInternet()
            AlarmScheduler.ACTION_ON -> controller.enableInternet()
            else -> RouterActionResult.NotConfigured("غير معروف")
        }
        controller.disconnect()

        val db = AppDatabase.getInstance(applicationContext)
        val logAction = if (actionType == AlarmScheduler.ACTION_OFF) LogAction.DISABLE_INTERNET else LogAction.ENABLE_INTERNET
        val actionLabel = if (actionType == AlarmScheduler.ACTION_OFF) "إيقاف الإنترنت" else "تشغيل الإنترنت"

        db.logDao().insert(
            LogEntity(
                timestampEpochMillis = System.currentTimeMillis(),
                action = logAction,
                result = if (result.isSuccess) LogResult.SUCCESS else LogResult.FAILURE,
                failureReason = result.failureReasonOrNull(),
                triggeredBySchedule = scheduleName
            )
        )

        return if (result.isSuccess) {
            NotificationHelper.notifyResult(
                applicationContext, true,
                "تم تنفيذ \"$actionLabel\" بنجاح" + (scheduleName?.let { " — جدول: $it" } ?: "")
            )
            Result.success()
        } else {
            val attempt = runAttemptCount
            if (attempt < MAX_RETRIES) {
                Result.retry()
            } else {
                NotificationHelper.notifyResult(
                    applicationContext, false,
                    "فشل تنفيذ \"$actionLabel\" بعد $MAX_RETRIES محاولات — ${result.failureReasonOrNull()}"
                )
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_ACTION_TYPE = "action_type"
        const val KEY_SCHEDULE_NAME = "schedule_name"
        const val MAX_RETRIES = 3
    }
}
