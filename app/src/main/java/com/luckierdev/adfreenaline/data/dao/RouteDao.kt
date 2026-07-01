package com.luckierdev.adfreenaline.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.luckierdev.adfreenaline.data.entities.RouteEntity
import com.luckierdev.adfreenaline.data.entities.RouteWaypointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes ORDER BY id DESC")
    fun observeRoutes(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM route_waypoints WHERE routeId = :routeId ORDER BY sequence ASC")
    suspend fun waypointsForRoute(routeId: Long): List<RouteWaypointEntity>

    @Query("SELECT * FROM route_waypoints ORDER BY routeId ASC, sequence ASC")
    fun observeAllWaypoints(): Flow<List<RouteWaypointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoints(waypoints: List<RouteWaypointEntity>)

    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun deleteRoute(id: Long)

    @Query("DELETE FROM routes WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    @Query("DELETE FROM routes")
    suspend fun deleteAllRoutes()

    @Query("DELETE FROM route_waypoints")
    suspend fun deleteAllWaypoints()

    @Transaction
    suspend fun replaceRoute(route: RouteEntity, waypoints: List<RouteWaypointEntity>) {
        insertRoute(route)
        deleteWaypointsForRoute(route.id)
        insertWaypoints(waypoints)
    }

    @Query("DELETE FROM route_waypoints WHERE routeId = :routeId")
    suspend fun deleteWaypointsForRoute(routeId: Long)
}
