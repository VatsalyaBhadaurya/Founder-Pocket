package com.vatsalya.founderpocket.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class OnboardingStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

    val isCompleted: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[ONBOARDING_DONE] ?: false
    }

    suspend fun markCompleted() {
        context.appDataStore.edit { prefs -> prefs[ONBOARDING_DONE] = true }
    }
}
