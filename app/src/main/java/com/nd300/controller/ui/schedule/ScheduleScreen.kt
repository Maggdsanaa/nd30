package com.nd300.controller.ui.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nd300.controller.data.db.ScheduleEntity
import com.nd300.controller.ui.viewmodel.ScheduleViewModel
import java.util.Calendar

private val dayLabels = listOf(
    Calendar.SATURDAY to "السبت",
    Calendar.SUNDAY to "الأحد",
    Calendar.MONDAY to "الاثنين",
    Calendar.TUESDAY to "الثلاثاء",
    Calendar.WEDNESDAY to "الأربعاء",
    Calendar.THURSDAY to "الخميس",
    Calendar.FRIDAY to "الجمعة"
)

@Composable
fun ScheduleScreen() {
    val viewModel: ScheduleViewModel = viewModel()
    val schedules by viewModel.schedules.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ScheduleEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "إضافة جدول")
            }
        }
    ) { padding ->
        if (schedules.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("لا توجد جداول بعد — اضغط + لإضافة جدول")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(schedules) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onToggle = { viewModel.toggleEnabled(schedule) },
                        onEdit = { editing = schedule; showDialog = true },
                        onDelete = { viewModel.delete(schedule) }
                    )
                }
            }
        }
    }

    if (showDialog) {
        ScheduleEditDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { viewModel.save(it); showDialog = false }
        )
    }
}

@Composable
private fun ScheduleCard(
    schedule: ScheduleEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(schedule.name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = schedule.isEnabled, onCheckedChange = { onToggle() })
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف")
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("إيقاف: %02d:%02d".format(schedule.offHour, schedule.offMinute))
            Text("تشغيل: %02d:%02d".format(schedule.onHour, schedule.onMinute))
            Spacer(Modifier.height(4.dp))
            val days = schedule.daysSet()
            Text(
                dayLabels.filter { days.contains(it.first) }.joinToString("، ") { it.second },
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onEdit) { Text("تعديل") }
        }
    }
}

@Composable
private fun ScheduleEditDialog(
    initial: ScheduleEntity?,
    onDismiss: () -> Unit,
    onSave: (ScheduleEntity) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var offHour by remember { mutableStateOf(initial?.offHour ?: 23) }
    var offMinute by remember { mutableStateOf(initial?.offMinute ?: 0) }
    var onHour by remember { mutableStateOf(initial?.onHour ?: 7) }
    var onMinute by remember { mutableStateOf(initial?.onMinute ?: 0) }
    var selectedDays by remember {
        mutableStateOf(initial?.daysSet() ?: dayLabels.map { it.first }.toSet())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة جدول" else "تعديل الجدول") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("اسم الجدول") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                TimePickerRow("وقت إيقاف الإنترنت", offHour, offMinute) { h, m -> offHour = h; offMinute = m }
                Spacer(Modifier.height(10.dp))
                TimePickerRow("وقت تشغيل الإنترنت", onHour, onMinute) { h, m -> onHour = h; onMinute = m }
                Spacer(Modifier.height(10.dp))
                Text("الأيام", style = MaterialTheme.typography.labelLarge)
                dayLabels.forEach { (day, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectedDays.contains(day),
                            onCheckedChange = { checked ->
                                selectedDays = if (checked) selectedDays + day else selectedDays - day
                            }
                        )
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && selectedDays.isNotEmpty()) {
                    onSave(
                        ScheduleEntity(
                            id = initial?.id ?: 0,
                            name = name,
                            offHour = offHour, offMinute = offMinute,
                            onHour = onHour, onMinute = onMinute,
                            days = ScheduleEntity.daysToString(selectedDays),
                            isEnabled = initial?.isEnabled ?: true
                        )
                    )
                }
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun TimePickerRow(label: String, hour: Int, minute: Int, onChange: (Int, Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberStepper(value = hour, range = 0..23) { onChange(it, minute) }
            Text("  :  ")
            NumberStepper(value = minute, range = 0..59, step = 5) { onChange(hour, it) }
        }
    }
}

@Composable
private fun NumberStepper(value: Int, range: IntRange, step: Int = 1, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            val next = value - step
            onChange(if (next < range.first) range.last else next)
        }) { Text("-") }
        Text("%02d".format(value), modifier = Modifier.width(32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = {
            val next = value + step
            onChange(if (next > range.last) range.first else next)
        }) { Text("+") }
    }
}
