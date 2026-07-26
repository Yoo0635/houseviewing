package com.capstone.houseviewingapp.analysis

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.capstone.houseviewingapp.MainActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.capstone.houseviewingapp.R
import com.capstone.houseviewingapp.analysis.model.AnalysisResponse
import com.capstone.houseviewingapp.analysis.model.ApiRiskLevel
import com.capstone.houseviewingapp.data.local.AuthTokenLocalStore
import com.capstone.houseviewingapp.data.local.AnalysisLocalStore
import com.capstone.houseviewingapp.databinding.FragmentAnalysisBinding
import com.capstone.houseviewingapp.home.PdfViewerActivity
import kotlinx.coroutines.launch

class AnalysisFragment : Fragment(R.layout.fragment_analysis) {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!

    private lateinit var recordAdapter: AnalysisRecordAdapter
    private var allRecords: List<AnalysisRecordItem> = emptyList()

    private enum class RecordTab { MY, AUTO }
    private var selectedTab: RecordTab = RecordTab.MY

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAnalysisBinding.bind(view)

        parentFragmentManager.setFragmentResultListener(
            MainActivity.RESULT_BOTTOM_REFRESH,
            viewLifecycleOwner
        ) { _, result ->
            val targetId = result.getInt(MainActivity.RESULT_KEY_TARGET_ID, -1)
            if (targetId == R.id.nav_analysis) {
                refreshFromServerAndRender()
            }
        }

        setupRecycler()
        setupTabs()
        setupFilterEvents()

        refreshFromServerAndRender()
    }
    private fun setupRecycler() {
        recordAdapter = AnalysisRecordAdapter(
            onLongClickDelete = { item ->
                AlertDialog.Builder(requireContext())
                    .setTitle("기록 삭제")
                    .setMessage("이 진단 기록을 삭제할까요?")
                    .setPositiveButton("삭제") { _, _ ->
                        AnalysisLocalStore.removeRecord(requireContext(), item)
                        allRecords = AnalysisLocalStore.getRecords(requireContext())
                        applyFilters()
                    }
                    .setNegativeButton("취소", null)
                    .show()
            },
            onDetailClick = { item ->
                val uriRaw = item.sourcePdfUri?.trim()?.takeIf { it.isNotBlank() }
                if (uriRaw == null || isLegacyMockPdfUrl(uriRaw)) {
                    Toast.makeText(
                        requireContext(),
                        "서버에서 받은 PDF 주소가 없습니다. 분석을 다시 진행해 주세요.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    startActivity(
                        Intent(requireContext(), PdfViewerActivity::class.java).apply {
                            putExtra(PdfViewerActivity.EXTRA_URI, uriRaw)
                            putExtra(PdfViewerActivity.EXTRA_TITLE, "${item.title} 상세 대응 리포트")
                            putExtra(PdfViewerActivity.EXTRA_SHOW_REPORT_BUTTON, true)
                        }
                    )
                }
            }
        )
        binding.recordRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recordRecyclerView.adapter = recordAdapter
    }
    private fun setupTabs() = with(binding) {
        tabMyRecord.setOnClickListener { selectTab(RecordTab.MY, animate = true) }
        tabAutoRecord.setOnClickListener { selectTab(RecordTab.AUTO, animate = true) }

        tabLayout.post { selectTab(selectedTab, animate = false) }
    }

    private fun selectTab(tab: RecordTab, animate: Boolean) = with(binding) {
        selectedTab = tab

        val blue = ContextCompat.getColor(requireContext(), R.color.blue)
        val gray = ContextCompat.getColor(requireContext(), R.color.textgray)
        val bold = ResourcesCompat.getFont(requireContext(), R.font.pretendard_bold)
        val medium = ResourcesCompat.getFont(requireContext(), R.font.pretendard_medium)

        val mySelected = tab == RecordTab.MY
        tabMyRecord.setTextColor(if (mySelected) blue else gray)
        tabAutoRecord.setTextColor(if (mySelected) gray else blue)
        tabMyRecord.typeface = if (mySelected) bold else medium
        tabAutoRecord.typeface = if (mySelected) medium else bold

        val target = if (mySelected) tabMyRecord else tabAutoRecord

        tabIndicator.layoutParams = tabIndicator.layoutParams.apply {
            width = target.width
        }
        tabIndicator.requestLayout()

        val targetX = target.x
        if (animate) {
            tabIndicator.animate()
                .x(targetX)
                .setDuration(180)
                .start()
        } else {
            tabIndicator.x = targetX
        }

        applyFilters()
    }

    private fun setupFilterEvents() {
        binding.filterChipGroup.setOnCheckedStateChangeListener { _, _ ->
            applyFilters()
        }
    }

    private fun applyFilters() {
        val bySource = when (selectedTab) {
            RecordTab.MY -> allRecords.filter { it.source == RecordSource.MANUAL }
            RecordTab.AUTO -> allRecords.filter { it.source == RecordSource.AUTO }
        }

        val filtered = bySource.filter { item ->
            when (binding.filterChipGroup.checkedChipId) {
                R.id.chipRed -> item.level == RiskLevel.RED
                R.id.chipAmber -> item.level == RiskLevel.AMBER
                R.id.chipBlue -> item.level == RiskLevel.BLUE
                else -> true // chipAll
            }
        }

        renderRecords(filtered)
    }

    private fun renderRecords(records: List<AnalysisRecordItem>) {
        val isEmpty = records.isEmpty()
        binding.emptyLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recordRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        recordAdapter.submit(records)
    }

    override fun onResume() {
        super.onResume()
        refreshFromServerAndRender()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** 예전 목 데이터에 남아 있을 수 있는 mock.local — 실제 PDF가 없으므로 열지 않음 */
    private fun isLegacyMockPdfUrl(url: String): Boolean {
        val host = Uri.parse(url).host?.lowercase() ?: return false
        return host == "mock.local" || host.endsWith(".mock.local")
    }

    private fun refreshFromServerAndRender() {
        val context = requireContext()
        val localRecords = AnalysisLocalStore.getRecords(context)
        val accessToken = AuthTokenLocalStore.getAccessToken(context).orEmpty()
        if (accessToken.isBlank() || localRecords.isEmpty()) {
            allRecords = localRecords
            applyFilters()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val manualAnalyses = AnalysisRepositoryProvider.repository
                .getAnalyses(accessToken)
                .getOrNull()
                .orEmpty()
            val autoAnalyses = AnalysisRepositoryProvider.repository
                .getDiffAnalyses(accessToken)
                .getOrNull()
                .orEmpty()

            val merged = localRecords.map { record ->
                mergeWithServerMeta(record, manualAnalyses, autoAnalyses)
            }
            if (merged != localRecords) {
                AnalysisLocalStore.setRecords(context, merged)
            }
            allRecords = merged
            applyFilters()
        }
    }

    private fun mergeWithServerMeta(
        record: AnalysisRecordItem,
        manualAnalyses: List<AnalysisResponse>,
        autoAnalyses: List<AnalysisResponse>
    ): AnalysisRecordItem {
        // AUTO는 카드 메타와 PDF URL을 같은 분석 결과로 유지해야 한다.
        // 그런데 /analyses/diff 응답에는 PDF URL이 없어서, 서버 메타를 임의 매칭하면
        // "카드는 SAFE, PDF는 WARNING" 같은 불일치가 생길 수 있다.
        // 이미 PDF가 있는 AUTO 레코드는 저장 당시 값을 우선 신뢰한다.
        if (record.source == RecordSource.AUTO && !record.sourcePdfUri.isNullOrBlank()) {
            return record
        }
        val candidates = if (record.source == RecordSource.AUTO) autoAnalyses else manualAnalyses
        val best = selectBestServerMeta(
            candidates = candidates,
            targetPdfReportId = record.pdfReportId,
            nickname = record.title,
            address = record.address,
            source = record.source
        ) ?: return record
        return record.copy(
            riskSummary = best.mainReason?.takeIf { it.isNotBlank() } ?: record.riskSummary,
            level = best.riskLevel?.toUiRiskLevel() ?: record.level,
            ltv = best.ltvScore?.toDouble() ?: record.ltv,
            pdfReportId = best.pdfReportId ?: record.pdfReportId
        )
    }

    private fun selectBestServerMeta(
        candidates: List<AnalysisResponse>,
        targetPdfReportId: Long?,
        nickname: String,
        address: String,
        source: RecordSource
    ): AnalysisResponse? {
        if (candidates.isEmpty()) return null
        val sourceFiltered = filterCandidatesBySource(candidates, source)
        if (sourceFiltered.isEmpty()) return null
        targetPdfReportId?.let { reportId ->
            sourceFiltered.firstOrNull { it.pdfReportId == reportId }?.let { return it }
        }
        if (source == RecordSource.AUTO) {
            // 자동 감지 기록은 가장 최신 DIFF 결과(첫 원소)를 화면 기준값으로 사용한다.
            return sourceFiltered.firstOrNull()
        }
        val normalizedNickname = normalizeKey(nickname)
        val ordered = if (source == RecordSource.MANUAL) sourceFiltered.asReversed() else sourceFiltered

        ordered.firstOrNull { item ->
            normalizeKey(item.nickname) == normalizedNickname &&
                normalizeAddress(item.address) == normalizeAddress(address)
        }?.let { return it }

        ordered.firstOrNull { item ->
            normalizeKey(item.nickname) == normalizedNickname &&
                matchAddressScore(item.address, address) >= 2
        }?.let { return it }

        return if (source == RecordSource.AUTO) {
            ordered.firstOrNull { matchAddressScore(it.address, address) >= 2 }
        } else {
            null
        }
    }

    private fun filterCandidatesBySource(
        candidates: List<AnalysisResponse>,
        source: RecordSource
    ): List<AnalysisResponse> {
        val expectedType = when (source) {
            RecordSource.MANUAL -> "PRE"
            RecordSource.AUTO -> "POST"
        }
        val typed = candidates.filter { it.analysisType.equals(expectedType, ignoreCase = true) }
        return if (typed.isNotEmpty()) typed else candidates
    }

    private fun normalizeKey(value: String): String {
        return value
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), "")
    }

    private fun matchAddressScore(serverAddress: String, localAddress: String): Int {
        val a = normalizeAddress(serverAddress)
        val b = normalizeAddress(localAddress)
        if (a.isBlank() || b.isBlank()) return 0
        return when {
            a == b -> 3
            a.contains(b) || b.contains(a) -> 2
            else -> 0
        }
    }

    private fun normalizeAddress(value: String): String {
        return value
            .lowercase()
            .replace(Regex("\\s+"), "")
            .replace("대한민국", "")
            .replace("경기도", "경기")
    }

    private fun ApiRiskLevel.toUiRiskLevel(): RiskLevel = when (this) {
        ApiRiskLevel.DANGER -> RiskLevel.RED
        ApiRiskLevel.WARNING -> RiskLevel.AMBER
        ApiRiskLevel.SAFE -> RiskLevel.BLUE
    }

}