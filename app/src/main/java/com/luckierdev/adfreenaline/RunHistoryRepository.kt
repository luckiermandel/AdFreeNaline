package com.luckierdev.adfreenaline

import android.content.Context
import com.luckierdev.adfreenaline.data.AppDatabase
import com.luckierdev.adfreenaline.data.mappers.toRecord
import com.luckierdev.adfreenaline.data.mappers.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RunHistoryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _history = MutableStateFlow<List<RunRecord>>(emptyList())
    val history: StateFlow<List<RunRecord>> = _history.asStateFlow()

    init {
        scope.launch {
            val dao = AppDatabase.getInstance(appContext).runDao()
            dao.observeAll()
                .map { records -> records.map { it.toRecord() } }
                .collect { _history.value = it }
        }
    }

    suspend fun awaitReady() {
        AppDatabase.getInstance(appContext)
    }

    fun addRecord(record: RunRecord) {
        scope.launch {
            AppDatabase.getInstance(appContext).runDao().insert(record.toEntity())
        }
    }

    fun clearAll() {
        scope.launch {
            AppDatabase.getInstance(appContext).runDao().deleteAll()
        }
    }
}
