package com.example.packetapp.data

import android.content.Context
import android.content.SharedPreferences

class AuthManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveAuthData(accessToken: String, refreshToken: String, userId: Int, name: String? = null, email: String? = null) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putInt("user_id", userId)
            .putString("user_name", name)
            .putString("user_email", email)
            .commit() // Используем commit для синхронного сохранения
        println("Tokens saved: access=$accessToken, refresh=$refreshToken, userId=$userId, name = $name, email = $email")

    }

    fun getAccessToken(): String? {
        val token = prefs.getString("access_token", null)
        println("getAccessToken: $token")
        return token
    }

    fun getRefreshToken(): String? {
        val token = prefs.getString("refresh_token", null)
        println("getRefreshToken: $token")
        return token
    }

    fun getUserId(): Int? {
        val userId = if (prefs.contains("user_id")) prefs.getInt("user_id", -1) else null
        return if (userId == -1) null else userId
    }

    fun getUserName(): String? {
        return prefs.getString("user_name", null)
    }

    fun getUserEmail(): String? {
        return prefs.getString("user_email", null)
    }

    fun clearAuthData() {
        prefs.edit().clear().commit()
    }

    fun isUserLoggedIn(): Boolean {
        val accessToken = getAccessToken()
        val refreshToken = getRefreshToken()
        println("isUserLoggedIn: accessToken=$accessToken, refreshToken=$refreshToken")
        return accessToken != null && refreshToken != null
    }
}