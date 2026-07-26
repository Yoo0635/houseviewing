package com.capstone.houseviewingapp.data.local

import android.content.Context

object UserProfileLocalStore {
    private const val PREF_NAME = "user_profile_local_pref"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"
    private const val KEY_LOGIN_ID = "login_id"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, name: String, email: String, loginId: String) {
        prefs(context).edit()
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_LOGIN_ID, loginId)
            .apply()
    }

    fun getName(context: Context): String =
        prefs(context).getString(KEY_NAME, "").orEmpty()

    fun getEmail(context: Context): String =
        prefs(context).getString(KEY_EMAIL, "").orEmpty()

    fun getLoginId(context: Context): String =
        prefs(context).getString(KEY_LOGIN_ID, "").orEmpty()

    fun clear(context: Context) {
        prefs(context).edit()
            .remove(KEY_NAME)
            .remove(KEY_EMAIL)
            .remove(KEY_LOGIN_ID)
            .apply()
    }
}

