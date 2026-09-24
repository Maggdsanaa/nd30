package com.nd300.controller.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nd300.controller.data.db.ScheduleEntity
import com.nd300.controller.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ScheduleRepository(application)

    val schedules: StateFlow<List<ScheduleEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(schedule: ScheduleEntity) {
        viewModelScope.launch { repository.save(schedule) }
    }

    fun delete(schedule: ScheduleEntity) {
        viewModelScope.launch { repository.delete(schedule) }
    }

    fun toggleEnabled(schedule: ScheduleEntity) {
        viewModelScope.launch { repository.save(schedule.copy(isEnabled = !schedule.isEnabled)) }
    }
}
