package com.example

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "capsule_calibration_settings")

object CapsulePreferencesRepository {
    private val X_OFFSET = floatPreferencesKey("x_offset")
    private val Y_OFFSET = floatPreferencesKey("y_offset")
    private val SCALE_WIDTH = floatPreferencesKey("scale_width")
    private val SCALE_HEIGHT = floatPreferencesKey("scale_height")
    private val SPLIT_ISLAND_ENABLED = booleanPreferencesKey("split_island_enabled")
    
    fun getXOffset(context: Context): Flow<Float> = context.preferencesDataStore.data.map { it[X_OFFSET] ?: 0f }
    suspend fun setXOffset(context: Context, offset: Float) {
        context.preferencesDataStore.edit { it[X_OFFSET] = offset }
    }

    fun getYOffset(context: Context): Flow<Float> = context.preferencesDataStore.data.map { it[Y_OFFSET] ?: 50f }
    suspend fun setYOffset(context: Context, offset: Float) {
        context.preferencesDataStore.edit { it[Y_OFFSET] = offset }
    }

    fun getScaleWidth(context: Context): Flow<Float> = context.preferencesDataStore.data.map { it[SCALE_WIDTH] ?: 120f }
    suspend fun setScaleWidth(context: Context, width: Float) {
        context.preferencesDataStore.edit { it[SCALE_WIDTH] = width }
    }

    fun getScaleHeight(context: Context): Flow<Float> = context.preferencesDataStore.data.map { it[SCALE_HEIGHT] ?: 36f }
    suspend fun setScaleHeight(context: Context, height: Float) {
        context.preferencesDataStore.edit { it[SCALE_HEIGHT] = height }
    }

    fun isSplitIslandEnabled(context: Context): Flow<Boolean> = context.preferencesDataStore.data.map { it[SPLIT_ISLAND_ENABLED] ?: true }
    suspend fun setSplitIslandEnabled(context: Context, enabled: Boolean) {
        context.preferencesDataStore.edit { it[SPLIT_ISLAND_ENABLED] = enabled }
    }
}
