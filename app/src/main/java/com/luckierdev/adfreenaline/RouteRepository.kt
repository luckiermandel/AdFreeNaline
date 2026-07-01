package com.luckierdev.adfreenaline

import android.content.Context
import com.luckierdev.adfreenaline.data.AppDatabase
import com.luckierdev.adfreenaline.data.mappers.toEntities
import com.luckierdev.adfreenaline.data.mappers.toSavedRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class RouteRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _routes = MutableStateFlow<List<SavedRoute>>(emptyList())
    val routes: StateFlow<List<SavedRoute>> = _routes.asStateFlow()

    init {
        scope.launch {
            val db = AppDatabase.getInstance(appContext)
            val routeDao = db.routeDao()
            combine(routeDao.observeRoutes(), routeDao.observeAllWaypoints()) { routes, waypoints ->
                val grouped = waypoints.groupBy { it.routeId }
                routes.map { route ->
                    route.toSavedRoute(grouped[route.id].orEmpty())
                }
            }.collect { _routes.value = it }
        }
    }

    fun save(route: SavedRoute) {
        scope.launch {
            val (entity, waypoints) = route.toEntities()
            AppDatabase.getInstance(appContext).routeDao().replaceRoute(entity, waypoints)
        }
    }

    fun delete(id: Long) {
        scope.launch {
            AppDatabase.getInstance(appContext).routeDao().deleteRoute(id)
        }
    }

    fun deleteCategory(category: String) {
        scope.launch {
            AppDatabase.getInstance(appContext).routeDao().deleteByCategory(category)
        }
    }

    fun clearAll() {
        scope.launch {
            val dao = AppDatabase.getInstance(appContext).routeDao()
            dao.deleteAllWaypoints()
            dao.deleteAllRoutes()
        }
    }
}
