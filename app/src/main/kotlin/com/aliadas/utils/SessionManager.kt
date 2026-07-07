package com.aliadas.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val Context.dataStore by preferencesDataStore(name = "aliadas_prefs")

object SessionManager {
    private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    private val USER_ID_KEY = intPreferencesKey("user_id")
    private val USER_NAME_KEY = stringPreferencesKey("user_name")
    private val AVATAR_KEY = stringPreferencesKey("avatar_icon")
    private val LOCATION_SHARING_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("location_sharing")
    private val TRUSTED_CONTACTS_KEY = stringPreferencesKey("trusted_contacts")

    suspend fun saveSession(context: Context, token: String, userId: Int, name: String, avatar: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_ID_KEY] = userId
            prefs[USER_NAME_KEY] = name
            prefs[AVATAR_KEY] = avatar
        }
    }

    fun setLocationSharing(context: Context, enabled: Boolean, scope: CoroutineScope? = null) {
        val work = {
            runBlocking {
                context.dataStore.edit { prefs ->
                    prefs[LOCATION_SHARING_KEY] = enabled
                }
            }
        }
        if (scope != null) scope.launch { work() } else work()
    }

    fun isLocationSharingEnabled(context: Context): Boolean = runBlocking {
        context.dataStore.data.map { it[LOCATION_SHARING_KEY] ?: false }.first()
    }

    suspend fun saveTrustedContacts(context: Context, contactsJson: String) {
        context.dataStore.edit { prefs ->
            prefs[TRUSTED_CONTACTS_KEY] = contactsJson
        }
    }

    fun getTrustedContacts(context: Context): String? = runBlocking {
        context.dataStore.data.map { it[TRUSTED_CONTACTS_KEY] }.first()
    }

    suspend fun clearSession(context: Context) {
        context.dataStore.edit { it.clear() }
    }

    fun getToken(context: Context): String? = runBlocking {
        context.dataStore.data.map { it[TOKEN_KEY] }.first()
    }

    fun getBearerToken(context: Context): String {
        val token = getToken(context)?.trim()
        return if (token.isNullOrBlank()) "" else "Bearer $token"
    }

    fun getUserName(context: Context): String? = runBlocking {
        context.dataStore.data.map { it[USER_NAME_KEY] }.first()
    }

    fun getAvatar(context: Context): String? = runBlocking {
        context.dataStore.data.map { it[AVATAR_KEY] }.first()
    }

    fun isLoggedIn(context: Context): Boolean = !getToken(context).isNullOrBlank()
}
