package com.capstone.houseviewingapp.data.local

import android.content.Context

object BillingLocalStore {
    private const val PREF_NAME_PREFIX = "billing_local_pref"
    private const val KEY_IS_PREMIUM = "is_premium"

    private fun prefs(context: Context) =
        context.getSharedPreferences(resolvePrefName(context), Context.MODE_PRIVATE)

    private fun resolvePrefName(context: Context): String {
        val loginId = AuthTokenLocalStore.getLoginId(context).orEmpty()
            .ifBlank { "guest" }
        val safeLoginId = loginId.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        return "${PREF_NAME_PREFIX}_$safeLoginId"
    }

    fun isPremium(context: Context): Boolean =
        prefs(context).getBoolean(KEY_IS_PREMIUM, true)

    fun setPremium(context: Context, isPremium: Boolean) {
        prefs(context).edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply()
    }
}

