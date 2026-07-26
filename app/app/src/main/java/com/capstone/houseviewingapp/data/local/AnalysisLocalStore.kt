package com.capstone.houseviewingapp.data.local

import android.content.Context
import com.capstone.houseviewingapp.analysis.AnalysisRecordItem
import com.capstone.houseviewingapp.analysis.RecordSource
import com.capstone.houseviewingapp.analysis.RiskLevel
import org.json.JSONArray
import org.json.JSONObject

object AnalysisLocalStore {
    private const val PREF_NAME_PREFIX = "analysis_local_pref"
    private const val KEY_JSON = "analysis_records_json"

    private fun prefs(context: Context) =
        context.getSharedPreferences(resolvePrefName(context), Context.MODE_PRIVATE)

    private fun resolvePrefName(context: Context): String {
        val loginId = AuthTokenLocalStore.getLoginId(context).orEmpty()
            .ifBlank { "guest" }
        val safeLoginId = loginId.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        return "${PREF_NAME_PREFIX}_$safeLoginId"
    }

    fun getRecords(context: Context): List<AnalysisRecordItem> {
        val raw = prefs(context).getString(KEY_JSON, null) ?: return emptyList()

        return runCatching {
            val arr = JSONArray(raw)
            val out = mutableListOf<AnalysisRecordItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    AnalysisRecordItem(
                        title = o.optString("title"),
                        address = o.optString("address"),
                        riskSummary = o.optString("riskSummary"),
                        level = runCatching { RiskLevel.valueOf(o.getString("level")) }.getOrDefault(RiskLevel.AMBER),
                        source = runCatching { RecordSource.valueOf(o.getString("source")) }.getOrDefault(RecordSource.MANUAL),
                        ltv = if (o.has("ltv") && !o.isNull("ltv")) o.optDouble("ltv") else null,
                        sourcePdfUri = o.optString("sourcePdfUri", "").ifBlank { null },
                        pdfReportId = if (o.has("pdfReportId") && !o.isNull("pdfReportId")) o.optLong("pdfReportId") else null
                    )
                )
            }
            out
        }.getOrElse {
            emptyList()
        }
    }
    fun addRecord(context: Context, item: AnalysisRecordItem) {
        val list = getRecords(context).toMutableList()
        list.add(0, item)
        saveRecords(context, list)
    }
    fun removeRecord(context: Context, target: AnalysisRecordItem) {
        val list = getRecords(context).toMutableList()
        val idx = list.indexOfFirst { it == target }
        if (idx == -1) return
        list.removeAt(idx)
        saveRecords(context, list)
    }

    fun setRecords(context: Context, items: List<AnalysisRecordItem>) {
        saveRecords(context, items)
    }

    private fun saveRecords(context: Context, items: List<AnalysisRecordItem>) {
        val arr = JSONArray()
        items.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("title", r.title)
                    put("address", r.address)
                    put("riskSummary", r.riskSummary)
                    put("level", r.level.name)
                    put("source", r.source.name)
                    if (r.ltv != null) put("ltv", r.ltv)
                    if (!r.sourcePdfUri.isNullOrBlank()) put("sourcePdfUri", r.sourcePdfUri)
                    if (r.pdfReportId != null) put("pdfReportId", r.pdfReportId)
                }
            )
        }
        prefs(context).edit().putString(KEY_JSON, arr.toString()).apply()
    }
}