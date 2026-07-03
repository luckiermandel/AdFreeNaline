package com.luckierdev.adfreenaline.data

import androidx.room.TypeConverter
import com.luckierdev.adfreenaline.BiologicalSex
import com.luckierdev.adfreenaline.DistanceUnit
import com.luckierdev.adfreenaline.RouteMode
import com.luckierdev.adfreenaline.ThemeMode

class Converters {
    @TypeConverter
    fun fromThemeMode(value: ThemeMode): String = value.name

    @TypeConverter
    fun toThemeMode(value: String): ThemeMode = ThemeMode.valueOf(value)


    @TypeConverter
    fun fromDistanceUnit(value: DistanceUnit): String = value.name

    @TypeConverter
    fun toDistanceUnit(value: String): DistanceUnit = DistanceUnit.valueOf(value)

    @TypeConverter
    fun fromBiologicalSex(value: BiologicalSex): String = value.name

    @TypeConverter
    fun toBiologicalSex(value: String): BiologicalSex = BiologicalSex.valueOf(value)

    @TypeConverter
    fun fromRouteMode(value: RouteMode): String = value.name

    @TypeConverter
    fun toRouteMode(value: String): RouteMode = RouteMode.valueOf(value)
}
