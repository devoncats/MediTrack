@file:Suppress("DEPRECATION")

package com.devoncats.meditrack.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(@ApplicationContext context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSession(userId: Long, role: String) {
        preferences.edit {
            putLong(KEY_USER_ID, userId)
                .putString(KEY_ROLE, role)
        }
    }

    fun getUserId(): Long = preferences.getLong(KEY_USER_ID, NO_SESSION_ID)

    fun getRole(): String = preferences.getString(KEY_ROLE, "") ?: ""

    fun isLoggedIn(): Boolean = getUserId() != NO_SESSION_ID

    fun clearSession() {
        preferences.edit { clear() }
    }

    companion object {
        private const val PREFS_NAME = "meditrack_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ROLE = "role"
        private const val NO_SESSION_ID = -1L
    }
}
