package com.luckierdev.adfreenaline

import android.content.Context
import com.luckierdev.adfreenaline.data.AppDatabase
import com.luckierdev.adfreenaline.data.mappers.ActiveRunSnapshot
import com.luckierdev.adfreenaline.data.mappers.toEntity
import com.luckierdev.adfreenaline.data.mappers.toSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActiveSessionRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun load(): ActiveRunSnapshot? = withContext(Dispatchers.IO) {
        AppDatabase.getInstance(appContext).activeSessionDao().get()?.toSnapshot()
    }

    fun save(snapshot: ActiveRunSnapshot) {
        scope.launch {
            AppDatabase.getInstance(appContext).activeSessionDao().upsert(snapshot.toEntity())
        }
    }

    fun clear() {
        scope.launch {
            AppDatabase.getInstance(appContext).activeSessionDao().deleteAll()
        }
    }

    suspend fun clearSuspend() = withContext(Dispatchers.IO) {
        AppDatabase.getInstance(appContext).activeSessionDao().deleteAll()
    }
}
