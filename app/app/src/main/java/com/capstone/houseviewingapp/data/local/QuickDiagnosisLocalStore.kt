package com.capstone.houseviewingapp.data.local

import android.content.Context

object QuickDiagnosisLocalStore {
    private const val PREF_NAME = "quick_diagnosis_local_pref"
    private const val KEY_PREFIX_FREE_USED = "free_used_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isFreeUsed(context: Context, loginId: String): Boolean {
        if (loginId.isBlank()) return false
        return prefs(context).getBoolean(KEY_PREFIX_FREE_USED + loginId.lowercase(), false)
    }

    fun markFreeUsed(context: Context, loginId: String) {
        if (loginId.isBlank()) return
        prefs(context).edit()
            .putBoolean(KEY_PREFIX_FREE_USED + loginId.lowercase(), true)
            .apply()
    }
}

