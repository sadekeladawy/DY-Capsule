package com.example

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "capsule_calibration_settings")

object CapsulePreferencesRepository {
    private val X_POS = intPreferencesKey("x_pos")
    private val Y_POS = intPreferencesKey("y_pos")
    private val CAPSULE_WIDTH = intPreferencesKey("capsule_width")
    private val CAPSULE_HEIGHT = intPreferencesKey("capsule_height")
    private val SPLIT_ISLAND_ENABLED = booleanPreferencesKey("split_island_enabled")
    
    fun getXOffset(context: Context): Flow<Int> = context.preferencesDataStore.data.map { it[X_POS] ?: 0 }
    suspend fun setXOffset(context: Context, offset: Int) {
        context.preferencesDataStore.edit { it[X_POS] = offset }
    }

    fun getYOffset(context: Context): Flow<Int> = context.preferencesDataStore.data.map { it[Y_POS] ?: 50 }
    suspend fun setYOffset(context: Context, offset: Int) {
        context.preferencesDataStore.edit { it[Y_POS] = offset }
    }

    fun getScaleWidth(context: Context): Flow<Int> = context.preferencesDataStore.data.map { it[CAPSULE_WIDTH] ?: 120 }
    suspend fun setScaleWidth(context: Context, width: Int) {
        context.preferencesDataStore.edit { it[CAPSULE_WIDTH] = width }
    }

    fun getScaleHeight(context: Context): Flow<Int> = context.preferencesDataStore.data.map { it[CAPSULE_HEIGHT] ?: 36 }
    suspend fun setScaleHeight(context: Context, height: Int) {
        context.preferencesDataStore.edit { it[CAPSULE_HEIGHT] = height }
    }

    fun isSplitIslandEnabled(context: Context): Flow<Boolean> = context.preferencesDataStore.data.map { it[SPLIT_ISLAND_ENABLED] ?: true }
    suspend fun setSplitIslandEnabled(context: Context, enabled: Boolean) {
        context.preferencesDataStore.edit { it[SPLIT_ISLAND_ENABLED] = enabled }
    }
}
