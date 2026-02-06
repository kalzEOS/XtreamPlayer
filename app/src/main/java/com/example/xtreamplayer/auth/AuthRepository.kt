package com.example.xtreamplayer.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
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
                password = decryptPassword(password)
            )
        }
    }

    suspend fun save(config: AuthConfig) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.LIST_NAME] = config.listName
            prefs[Keys.BASE_URL] = config.baseUrl
            prefs[Keys.USERNAME] = config.username
            prefs[Keys.PASSWORD] = encryptPassword(config.password)
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

    private fun encryptPassword(password: String): String {
        if (password.isEmpty()) return password
        return runCatching {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val encrypted = cipher.doFinal(password.toByteArray(StandardCharsets.UTF_8))
            val payload = ByteArray(cipher.iv.size + encrypted.size)
            System.arraycopy(cipher.iv, 0, payload, 0, cipher.iv.size)
            System.arraycopy(encrypted, 0, payload, cipher.iv.size, encrypted.size)
            "$ENCRYPTED_PREFIX${Base64.encodeToString(payload, Base64.NO_WRAP)}"
        }.getOrElse { password }
    }

    private fun decryptPassword(stored: String): String {
        if (!stored.startsWith(ENCRYPTED_PREFIX)) {
            return stored
        }
        return runCatching {
            val payload = Base64.decode(stored.removePrefix(ENCRYPTED_PREFIX), Base64.NO_WRAP)
            if (payload.size <= GCM_IV_SIZE_BYTES) {
                return@runCatching stored
            }
            val iv = payload.copyOfRange(0, GCM_IV_SIZE_BYTES)
            val encrypted = payload.copyOfRange(GCM_IV_SIZE_BYTES, payload.size)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            )
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        }.getOrElse { stored }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)
        if (existing != null) {
            return existing
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val parameterSpec =
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "xtreamplayer_auth_password_key"
        const val ENCRYPTED_PREFIX = "enc:"
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val GCM_IV_SIZE_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
