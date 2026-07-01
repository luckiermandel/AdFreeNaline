package com.luckierdev.adfreenaline.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "route_waypoints",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routeId")]
)
data class RouteWaypointEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val routeId: Long,
    val sequence: Int,
    val latitude: Double,
    val longitude: Double
)
