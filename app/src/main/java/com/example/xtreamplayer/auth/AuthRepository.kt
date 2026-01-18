package com.example.xtreamplayer.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth")

class AuthRepository(private val context: Context) {
    val authConfig: Flow<AuthConfig?> = context.authDataStore.data.map { prefs ->
        val listName = prefs[Keys.LIST_NAME]
        val baseUrl = prefs[Keys.BASE_URL]
        val username = prefs[Keys.USERNAME]
        val password = prefs[Keys.PASSWORD]
        if (listName.isNullOrBlank() || baseUrl.isNullOrBlank() ||
            username.isNullOrBlank() || password.isNullOrBlank()
        ) {
            null
        } else {
            AuthConfig(
                listName = listName,
                baseUrl = baseUrl,
                username = username,
                password = password
            )
        }
    }

    suspend fun save(config: AuthConfig) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.LIST_NAME] = config.listName
            prefs[Keys.BASE_URL] = config.baseUrl
            prefs[Keys.USERNAME] = config.username
            prefs[Keys.PASSWORD] = config.password
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { prefs ->
            prefs.remove(Keys.LIST_NAME)
            prefs.remove(Keys.BASE_URL)
            prefs.remove(Keys.USERNAME)
            prefs.remove(Keys.PASSWORD)
        }
    }

    private object Keys {
        val LIST_NAME = stringPreferencesKey("list_name")
        val BASE_URL = stringPreferencesKey("base_url")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
    }
}
