package com.nd300.controller.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nd300.controller.data.db.AppDatabase
import com.nd300.controller.data.db.LogAction
import com.nd300.controller.data.db.LogEntity
import com.nd300.controller.data.db.LogResult
import com.nd300.controller.data.security.SecureStorage
import com.nd300.controller.network.RouterActionResult
import com.nd300.controller.network.RouterConfig
import com.nd300.controller.network.RouterController
import com.nd300.controller.network.TemplateJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ConnectionState { UNKNOWN, CONNECTED, DISCONNECTED }

data class HomeUiState(
    val connectionState: ConnectionState = ConnectionState.UNKNOWN,
    val isBusy: Boolean = false,
    val lastMessage: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = SecureStorage(application)
    private val logDao = AppDatabase.getInstance(application).logDao()

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    private fun buildController(): RouterController {
        val templates = TemplateJson.decode(storage.requestTemplatesJson)
        val config = RouterConfig(storage.routerIp, storage.routerPort, storage.username, storage.password, templates)
        return RouterController(config)
    }

    fun refreshStatus() = runAction(LogAction.TEST_CONNECTION, "تحديث الحالة") { it.connect() }

    fun enableInternet() = runAction(LogAction.ENABLE_INTERNET, "تشغيل الإنترنت") { it.enableInternet() }

    fun disableInternet() = runAction(LogAction.DISABLE_INTERNET, "إيقاف الإنترنت") { it.disableInternet() }

    fun reboot() = runAction(LogAction.REBOOT, "إعادة تشغيل المودم") { it.reboot() }

    private fun runAction(
        logAction: LogAction,
        label: String,
        block: suspend (RouterController) -> RouterActionResult
    ) {
        _state.value = _state.value.copy(isBusy = true, lastMessage = null)
        viewModelScope.launch {
            val controller = buildController()
            val result = withContext(Dispatchers.IO) { block(controller) }
            controller.disconnect()

            logDao.insert(
                LogEntity(
                    timestampEpochMillis = System.currentTimeMillis(),
                    action = logAction,
                    result = if (result.isSuccess) LogResult.SUCCESS else LogResult.FAILURE,
                    failureReason = result.failureReasonOrNull()
                )
            )

            val newConnState = if (result.isSuccess) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED

            _state.value = _state.value.copy(
                isBusy = false,
                connectionState = newConnState,
                lastMessage = if (result.isSuccess) "$label: تم بنجاح" else "$label: ${result.failureReasonOrNull()}"
            )
        }
    }
}
