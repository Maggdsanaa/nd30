package com.nd300.controller.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nd300.controller.data.db.LogEntity
import com.nd300.controller.data.repository.LogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LogsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LogRepository(application)

    val logs: StateFlow<List<LogEntity>> = repository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
