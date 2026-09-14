package com.capstone.houseviewingapp.analysis

import com.capstone.houseviewingapp.analysis.model.AnalysisResponse
import com.capstone.houseviewingapp.analysis.model.ApiRiskLevel

enum class RiskLevel { RED, AMBER, BLUE }

enum class RecordSource {MANUAL, AUTO}

data class AnalysisRecordItem(
    val title: String, // TODO : 집 닉네임, 1회 진단 : 시간 등으로 변경 (2026.03.14 진단기록)
    val address: String,
    val riskSummary: String, // 주요 원인
    val level: RiskLevel,
    val source: RecordSource, // 수동 입력인지 자동 분석인지
    val ltv: Double? = null,
    val sourcePdfUri: String? = null,
    val pdfReportId: Long? = null
)

fun AnalysisResponse.toRecordItem(source: RecordSource): AnalysisRecordItem {
    return AnalysisRecordItem(
        title = nickname,
        address = address,
        riskSummary = mainReason?.takeIf { it.isNotBlank() } ?: "-",
        level = riskLevel.toUiRiskLevel(),
        source = source,
        ltv = ltvScore?.toDouble(),
        pdfReportId = pdfReportId
    )
}

private fun ApiRiskLevel?.toUiRiskLevel(): RiskLevel {
    return when (this) {
        ApiRiskLevel.DANGER -> RiskLevel.RED
        ApiRiskLevel.WARNING -> RiskLevel.AMBER
        ApiRiskLevel.SAFE -> RiskLevel.BLUE
        null -> RiskLevel.AMBER
    }
}
