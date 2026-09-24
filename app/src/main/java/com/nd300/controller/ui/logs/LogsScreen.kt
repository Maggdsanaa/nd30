package com.nd300.controller.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nd300.controller.data.db.LogAction
import com.nd300.controller.data.db.LogEntity
import com.nd300.controller.data.db.LogResult
import com.nd300.controller.ui.viewmodel.LogsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen() {
    val viewModel: LogsViewModel = viewModel()
    val logs by viewModel.logs.collectAsState()

    if (logs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد عمليات مسجلة بعد")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(logs) { log -> LogRow(log) }
    }
}

private val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar"))

@Composable
private fun LogRow(log: LogEntity) {
    val actionLabel = when (log.action) {
        LogAction.DISABLE_INTERNET -> "إيقاف الإنترنت"
        LogAction.ENABLE_INTERNET -> "تشغيل الإنترنت"
        LogAction.REBOOT -> "إعادة تشغيل المودم"
        LogAction.TEST_CONNECTION -> "اختبار الاتصال"
    }
    val isSuccess = log.result == LogResult.SUCCESS

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatter.format(Date(log.timestampEpochMillis)), style = MaterialTheme.typography.bodySmall)
                Text(
                    if (isSuccess) "نجح" else "فشل",
                    color = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(actionLabel, style = MaterialTheme.typography.titleSmall)
            log.triggeredBySchedule?.let {
                Text("جدول: $it", style = MaterialTheme.typography.bodySmall)
            }
            if (!isSuccess && log.failureReason != null) {
                Spacer(Modifier.height(4.dp))
                Text("السبب: ${log.failureReason}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828))
            }
        }
    }
}
