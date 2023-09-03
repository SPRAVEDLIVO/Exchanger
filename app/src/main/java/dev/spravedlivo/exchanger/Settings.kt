package dev.spravedlivo.exchanger

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first

object Settings {
    suspend fun readStringSetKey(context: Context, key: String, default: Set<String>?): Set<String>? {
        val dataStoreKey = stringSetPreferencesKey(key)
        val preferences = context.dataStore.data.first()
        return preferences[dataStoreKey] ?: default
    }
    suspend fun saveStringSetKey(context: Context, key: String, value: Set<String>) {
        val dataStoreKey = stringSetPreferencesKey(key)
        context.dataStore.edit { settings ->
            settings[dataStoreKey] = value
        }
    }
}