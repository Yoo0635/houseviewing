package com.capstone.houseviewingapp.analysis

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.capstone.houseviewingapp.MainActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.capstone.houseviewingapp.R
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
    private val historyPager by lazy { AnalysisHistoryPager(AnalysisRepositoryProvider.repository) }
    private var isPaging = false

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
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recordRecyclerView.layoutManager = layoutManager
        binding.recordRecyclerView.adapter = recordAdapter
        binding.analysisScrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { scrollView, _, scrollY, _, oldScrollY ->
                if (scrollY > oldScrollY && isNearBottom(scrollView)) {
                    loadNextPage()
                }
            }
        )
    }
    private fun setupTabs() = with(binding) {
        tabMyRecord.setOnClickListener { changeTab(RecordTab.MY) }
        tabAutoRecord.setOnClickListener { changeTab(RecordTab.AUTO) }

        tabLayout.post { selectTab(selectedTab, animate = false) }
    }

    private fun changeTab(tab: RecordTab) {
        if (selectedTab == tab) return
        selectTab(tab, animate = true)
        refreshFromServerAndRender()
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
            refreshFromServerAndRender()
        }
    }

    private fun applyFilters() {
        val bySource = when (selectedTab) {
            RecordTab.MY -> allRecords.filter { it.source == RecordSource.MANUAL }
            RecordTab.AUTO -> allRecords.filter { it.source == RecordSource.AUTO }
        }

        val filtered = bySource.filter { item ->
            when (selectedRiskFilter()) {
                ApiRiskLevel.DANGER -> item.level == RiskLevel.RED
                ApiRiskLevel.WARNING -> item.level == RiskLevel.AMBER
                ApiRiskLevel.SAFE -> item.level == RiskLevel.BLUE
                null -> true
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
        if (accessToken.isBlank()) {
            allRecords = localRecords
            applyFilters()
            return
        }
        historyPager.reset(selectedSource(), selectedRiskFilter())
        loadNextPage()
    }

    private fun loadNextPage() {
        if (isPaging || _binding == null) return
        val context = requireContext()
        val accessToken = AuthTokenLocalStore.getAccessToken(context).orEmpty()
        if (accessToken.isBlank()) return
        isPaging = true
        binding.pagingProgressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val result = historyPager.loadNext(accessToken, selectedSource(), selectedRiskFilter())
            result.onSuccess { records ->
                val localByReportId = AnalysisLocalStore.getRecords(context)
                    .mapNotNull { local -> local.pdfReportId?.let { it to local } }
                    .toMap()
                allRecords = records.map { server ->
                    val local = server.pdfReportId?.let(localByReportId::get)
                    server.copy(sourcePdfUri = local?.sourcePdfUri)
                }
                applyFilters()
            }.onFailure {
                if (allRecords.isEmpty()) {
                    allRecords = AnalysisLocalStore.getRecords(context)
                    applyFilters()
                }
            }
            isPaging = false
            _binding?.pagingProgressBar?.visibility = View.GONE
        }
    }

    private fun selectedSource(): RecordSource {
        return when (selectedTab) {
            RecordTab.MY -> RecordSource.MANUAL
            RecordTab.AUTO -> RecordSource.AUTO
        }
    }

    private fun selectedRiskFilter(): ApiRiskLevel? {
        return when (binding.filterChipGroup.checkedChipId) {
            R.id.chipRed -> ApiRiskLevel.DANGER
            R.id.chipAmber -> ApiRiskLevel.WARNING
            R.id.chipBlue -> ApiRiskLevel.SAFE
            else -> null
        }
    }

    private fun isNearBottom(scrollView: NestedScrollView): Boolean {
        val child = scrollView.getChildAt(0) ?: return false
        return scrollView.scrollY >= child.measuredHeight - scrollView.measuredHeight - 64
    }

}
