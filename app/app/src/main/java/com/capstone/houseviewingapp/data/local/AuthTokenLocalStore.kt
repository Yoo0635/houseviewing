package com.capstone.houseviewingapp.data.local

import android.content.Context

object AuthTokenLocalStore {
    private const val PREF_NAME = "auth_token_local_pref"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_LOGIN_ID = "login_id"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getAccessToken(context: Context): String? =
        prefs(context).getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(context: Context): String? =
        prefs(context).getString(KEY_REFRESH_TOKEN, null)

    fun saveTokens(context: Context, accessToken: String, refreshToken: String) {
        prefs(context).edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun saveLoginId(context: Context, loginId: String) {
        prefs(context).edit().putString(KEY_LOGIN_ID, loginId).apply()
    }

    fun getLoginId(context: Context): String? =
        prefs(context).getString(KEY_LOGIN_ID, null)

    fun clear(context: Context) {
        prefs(context).edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_LOGIN_ID)
            .apply()
    }
}

