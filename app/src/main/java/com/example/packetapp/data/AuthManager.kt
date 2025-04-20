package com.example.packetapp.data

import android.content.Context
import android.content.SharedPreferences

class AuthManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveAuthData(accessToken: String, refreshToken: String, userId: Int) {
        val success = prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putInt("user_id", userId)
            .commit() // Заменяем apply() на commit()
        println("Tokens saved: access=$accessToken, refresh=$refreshToken, userId=$userId, success=$success")
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
        val userId = prefs.getInt("user_id", -1)
        println("getUserId: $userId")
        return if (userId != -1) userId else null
    }

    fun isUserLoggedIn(): Boolean {
        val accessToken = getAccessToken()
        val refreshToken = getRefreshToken()
        println("isUserLoggedIn: accessToken=$accessToken, refreshToken=$refreshToken")
        return accessToken != null && refreshToken != null
    }

    fun clearAuthData() {
        val success = prefs.edit().clear().commit() // Заменяем apply() на commit()
        println("Clearing auth data, success=$success")
    }
}
