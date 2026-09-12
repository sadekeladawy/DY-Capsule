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

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "capsule_settings")

object CapsulePreferences {
    private val Y_OFFSET = floatPreferencesKey("y_offset")
    private val SPLIT_ISLAND_ENABLED = booleanPreferencesKey("split_island_enabled")
    
    fun getYOffset(context: Context): Flow<Float> {
        return context.dataStore.data.map { preferences ->
            preferences[Y_OFFSET] ?: 50f
        }
    }

    suspend fun setYOffset(context: Context, offset: Float) {
        context.dataStore.edit { preferences ->
            preferences[Y_OFFSET] = offset
        }
    }

    fun isSplitIslandEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[SPLIT_ISLAND_ENABLED] ?: true
        }
    }

    suspend fun setSplitIslandEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SPLIT_ISLAND_ENABLED] = enabled
        }
    }
}
