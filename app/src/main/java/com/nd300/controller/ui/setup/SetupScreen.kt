package com.nd300.controller.ui.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nd300.controller.network.CommonTemplates
import com.nd300.controller.network.RequestTemplate
import com.nd300.controller.network.RouterTemplateSet
import com.nd300.controller.ui.viewmodel.SetupViewModel

@Composable
fun SetupScreen() {
    val viewModel: SetupViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("إعداد المودم", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.ip, onValueChange = viewModel::updateIp,
            label = { Text("عنوان IP") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.port, onValueChange = viewModel::updatePort,
            label = { Text("المنفذ (اختياري — اتركه فارغاً لـ 80)") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.username, onValueChange = viewModel::updateUsername,
            label = { Text("اسم المستخدم") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.password, onValueChange = viewModel::updatePassword,
            label = { Text("كلمة المرور") }, modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(Modifier.height(18.dp))
        Divider()
        Spacer(Modifier.height(12.dp))

        Text("قوالب الأوامر (Endpoints)", style = MaterialTheme.typography.titleMedium)
        Text(
            "لا يمكن لأي تطبيق معرفة أوامر مودمك بدقة دون التقاطها من متصفحك. " +
                "افتح لوحة إدارة المودم من متصفح على نفس الشبكة، فعّل أدوات المطوّر (Network tab)، " +
                "وسجّل الطلب الحقيقي الذي يُرسل عند تسجيل الدخول وعند كل زر تحكم، ثم انسخه هنا بالضبط.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        OutlinedButton(onClick = { viewModel.updateTemplates(CommonTemplates.cstecgiStyle) }) {
            Text("تحميل قالب مرجعي (غير مؤكد لـ ND300) كنقطة بداية")
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "تحذير: هذا القالب مبني على نمط شائع في طرازات TOTOLINK أخرى وليس مؤكداً لـ ND300. " +
                "اختبره ثم عدّله حسب الاستجابة الفعلية.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(Modifier.height(16.dp))

        TemplateEditor("تسجيل الدخول", state.templates.login) {
            viewModel.updateTemplates(state.templates.copy(login = it))
        }
        TemplateEditor("قراءة الحالة", state.templates.getStatus) {
            viewModel.updateTemplates(state.templates.copy(getStatus = it))
        }
        TemplateEditor("إيقاف الإنترنت", state.templates.disableInternet) {
            viewModel.updateTemplates(state.templates.copy(disableInternet = it))
        }
        TemplateEditor("تشغيل الإنترنت", state.templates.enableInternet) {
            viewModel.updateTemplates(state.templates.copy(enableInternet = it))
        }
        TemplateEditor("إعادة التشغيل", state.templates.reboot) {
            viewModel.updateTemplates(state.templates.copy(reboot = it))
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "\"إيقاف الإنترنت\" يعني فعلياً: ${if (state.templates.disableInternetMeans == "WAN") "تعطيل اتصال WAN" else "تعطيل Wi-Fi"} " +
                "(حسب ما يوفره الفريموير — لن يُعتبر إعادة التشغيل بديلاً).",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(20.dp))

        Button(onClick = { viewModel.testConnection() }, enabled = !state.isTesting, modifier = Modifier.fillMaxWidth()) {
            if (state.isTesting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text("اختبار الاتصال")
        }

        state.testResultMessage?.let { msg ->
            Spacer(Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (state.testSucceeded == true)
                        MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                )
            ) { Text(msg, modifier = Modifier.padding(12.dp)) }
        }

        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { viewModel.save() }, modifier = Modifier.fillMaxWidth()) {
            Text("حفظ")
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun TemplateEditor(title: String, template: RequestTemplate, onChange: (RequestTemplate) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = template.method, onValueChange = { onChange(template.copy(method = it)) },
                    label = { Text("Method (GET/POST)") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = template.path, onValueChange = { onChange(template.copy(path = it)) },
                    label = { Text("المسار (Path) — مثال: /cgi-bin/xxx.cgi") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = template.bodyTemplate, onValueChange = { onChange(template.copy(bodyTemplate = it)) },
                    label = { Text("محتوى الطلب (Body) — POST فقط") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = template.successContains, onValueChange = { onChange(template.copy(successContains = it)) },
                    label = { Text("نص يدل على النجاح داخل الاستجابة (اختياري)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = template.extractTokenRegex, onValueChange = { onChange(template.copy(extractTokenRegex = it)) },
                    label = { Text("Regex لاستخراج token من الاستجابة (اختياري، لتسجيل الدخول فقط)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }
        }
    }
}
