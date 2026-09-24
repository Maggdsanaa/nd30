package com.nd300.controller.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nd300.controller.data.db.AppDatabase
import com.nd300.controller.data.db.LogAction
import com.nd300.controller.data.db.LogEntity
import com.nd300.controller.data.db.LogResult
import com.nd300.controller.data.security.SecureStorage
import com.nd300.controller.network.RouterConfig
import com.nd300.controller.network.RouterController
import com.nd300.controller.network.RouterTemplateSet
import com.nd300.controller.network.TemplateJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SetupUiState(
    val ip: String = "192.168.1.1",
    val port: String = "",
    val username: String = "admin",
    val password: String = "",
    val templates: RouterTemplateSet = RouterTemplateSet(),
    val isTesting: Boolean = false,
    val testResultMessage: String? = null,
    val testSucceeded: Boolean? = null
)

class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = SecureStorage(application)
    private val logDao = AppDatabase.getInstance(application).logDao()

    private val _state = MutableStateFlow(loadFromStorage())
    val state: StateFlow<SetupUiState> = _state

    private fun loadFromStorage() = SetupUiState(
        ip = storage.routerIp,
        port = storage.routerPort,
        username = storage.username,
        password = storage.password,
        templates = TemplateJson.decode(storage.requestTemplatesJson)
    )

    fun updateIp(v: String) { _state.value = _state.value.copy(ip = v) }
    fun updatePort(v: String) { _state.value = _state.value.copy(port = v) }
    fun updateUsername(v: String) { _state.value = _state.value.copy(username = v) }
    fun updatePassword(v: String) { _state.value = _state.value.copy(password = v) }
    fun updateTemplates(v: RouterTemplateSet) { _state.value = _state.value.copy(templates = v) }

    fun save() {
        val s = _state.value
        storage.routerIp = s.ip
        storage.routerPort = s.port
        storage.username = s.username
        storage.password = s.password
        storage.requestTemplatesJson = TemplateJson.encode(s.templates)
    }

    fun testConnection() {
        save()
        val s = _state.value
        _state.value = s.copy(isTesting = true, testResultMessage = null, testSucceeded = null)

        viewModelScope.launch {
            val config = RouterConfig(s.ip, s.port, s.username, s.password, s.templates)
            val (success, message) = withContext(Dispatchers.IO) {
                val controller = RouterController(config)
                val connectResult = controller.connect()
                if (!connectResult.isSuccess) {
                    controller.disconnect()
                    return@withContext false to (connectResult.failureReasonOrNull() ?: "فشل الاتصال")
                }
                if (s.templates.login.path.isBlank()) {
                    controller.disconnect()
                    return@withContext true to "تم الوصول إلى عنوان المودم. لم يتم إعداد قالب تسجيل الدخول بعد — أضفه لاختبار المصادقة الكاملة."
                }
                val loginResult = controller.login()
                controller.disconnect()
                loginResult.isSuccess to (loginResult.failureReasonOrNull() ?: "تم تسجيل الدخول بنجاح")
            }

            logDao.insert(
                LogEntity(
                    timestampEpochMillis = System.currentTimeMillis(),
                    action = LogAction.TEST_CONNECTION,
                    result = if (success) LogResult.SUCCESS else LogResult.FAILURE,
                    failureReason = if (success) null else message
                )
            )

            _state.value = _state.value.copy(
                isTesting = false,
                testSucceeded = success,
                testResultMessage = message
            )
        }
    }
}
