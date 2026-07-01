package com.luckierdev.adfreenaline

import android.content.Context
import com.luckierdev.adfreenaline.data.AppDatabase
import com.luckierdev.adfreenaline.data.mappers.toChallenge
import com.luckierdev.adfreenaline.data.mappers.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ChallengeRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _items = MutableStateFlow<List<CustomChallenge>>(emptyList())
    val items: StateFlow<List<CustomChallenge>> = _items.asStateFlow()

    init {
        scope.launch {
            val dao = AppDatabase.getInstance(appContext).challengeDao()
            dao.observeAll()
                .map { list -> list.map { it.toChallenge() } }
                .collect { _items.value = it }
        }
    }

    fun clearAll() {
        scope.launch {
            AppDatabase.getInstance(appContext).challengeDao().deleteAll()
        }
    }

    fun save(challenge: CustomChallenge) {
        scope.launch {
            AppDatabase.getInstance(appContext).challengeDao().insert(challenge.toEntity())
        }
    }
}
