package com.capstone.houseviewingapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.capstone.houseviewingapp.databinding.ActivityMainBinding
import com.capstone.houseviewingapp.notification.NotificationAccessGuideBottomSheetFragment
import com.capstone.houseviewingapp.notification.RegistryChangeDetectedDialogFragment

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ANALYSIS_SOURCE = "extra_analysis_source"
        const val EXTRA_SHOW_NOTIFICATION_ACCESS_GUIDE = "show_notification_access_guide"
        const val EXTRA_SHOW_REGISTRY_CHANGE_DIALOG = "show_registry_change_dialog"
        const val EXTRA_SHOW_ANALYSIS_LOADING = "show_analysis_loading"
        const val RESULT_BOTTOM_REFRESH = "result_bottom_refresh"
        const val RESULT_KEY_TARGET_ID = "target_id"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NOTE: 루트는 좌/우/상만 시스템 바 패딩 적용, 하단은 네비 바 영역이 별도 담당
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // NOTE: BottomNavigationView가 자체 패딩을 먹지 않도록 고정
        ViewCompat.setOnApplyWindowInsetsListener(binding.navigationBar) { v, insets ->
            v.setPadding(0, 0, 0, 0)
            insets
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // NOTE: 바텀 탭 수동 네비게이션 (상태 꼬임 방지)
        fun navigateBottom(targetId: Int) {
            // 같은 화면이면 무시
            if (navController.currentDestination?.id == targetId) return

            when (targetId) {
                R.id.nav_home -> {
                    // NOTE: 홈은 백스택에 있으면 pop, 없으면 navigate
                    val popped = navController.popBackStack(R.id.nav_home, false)
                    if (!popped) navController.navigate(R.id.nav_home)
                }
                R.id.nav_analysis -> navController.navigate(
                    R.id.nav_analysis,
                    null,
                    androidx.navigation.navOptions { launchSingleTop = true }
                )
                R.id.nav_my -> navController.navigate(
                    R.id.nav_my,
                    null,
                    androidx.navigation.navOptions { launchSingleTop = true }
                )
            }
        }

        fun notifyBottomRefresh(targetId: Int) {
            supportFragmentManager.setFragmentResult(
                RESULT_BOTTOM_REFRESH,
                bundleOf(RESULT_KEY_TARGET_ID to targetId)
            )
        }

        binding.navigationBar.setOnItemSelectedListener { item ->
            navigateBottom(item.itemId)
            notifyBottomRefresh(item.itemId)
            true
        }


// NOTE: destination 바뀌면 하단바 체크 상태 동기화
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.navigationBar.menu.findItem(destination.id)?.isChecked = true
        }

        // NOTE: 선택된 탭을 다시 눌렀을 때도 홈은 확실히 홈으로 보내기
        binding.navigationBar.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.nav_home) {
                val popped = navController.popBackStack(R.id.nav_home, false)
                if (!popped) navController.navigate(R.id.nav_home)
            }
            notifyBottomRefresh(item.itemId)
        }

        // NOTE: 최초 실행(onCreate) 시 전달된 extra 처리
        handleIntentExtras(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // NOTE: SINGLE_TOP / CLEAR_TOP으로 기존 MainActivity 재사용될 때는 onCreate가 아니라 onNewIntent가 호출됨
        setIntent(intent)
        // NOTE: 재진입 시에도 동일하게 extra 처리해야 로딩 화면 진입이 누락되지 않음
        handleIntentExtras(intent)
    }

    private fun handleIntentExtras(sourceIntent: Intent?) {
        if (sourceIntent == null) return

        // NOTE: 무료 진단 플로우에서 분석 로딩 화면 진입 트리거
        if (sourceIntent.getBooleanExtra(EXTRA_SHOW_ANALYSIS_LOADING, false)) {
            val source = sourceIntent.getStringExtra(EXTRA_ANALYSIS_SOURCE)
                ?: com.capstone.houseviewingapp.analysis.AnalysisFlow.SOURCE_MANUAL
            val houseNickname = sourceIntent.getStringExtra(com.capstone.houseviewingapp.analysis.AnalysisFlow.ARG_HOUSE_NICKNAME)
            val selectedFileUri = sourceIntent.getStringExtra(com.capstone.houseviewingapp.analysis.AnalysisFlow.ARG_SELECTED_FILE_URI)
            val originAddress = sourceIntent.getStringExtra(com.capstone.houseviewingapp.analysis.AnalysisFlow.ARG_ORIGIN_ADDRESS)
            val houseId = sourceIntent.getLongExtra(com.capstone.houseviewingapp.analysis.AnalysisFlow.ARG_HOUSE_ID, -1L)

            navController.navigate(
                R.id.nav_analysis_loading,
                androidx.core.os.bundleOf(
                    com.capstone.houseviewingapp.analysis.AnalysisFlow.ARG_ANALYSIS_SOURCE to source,
                    com.capstone.houseviewingapp.analysis.AnalysisFlow.ARG_HOUSE_NICKNAME to (houseNickname ?: ""),
                    com.capstone.houseviewingapp.analysis.AnalysisFlow.ARG_SELECTED_FILE_URI to (selectedFileUri ?: ""),
                    com.capstone.houseviewingapp.analysis.AnalysisFlow.ARG_ORIGIN_ADDRESS to (originAddress ?: ""),
                    com.capstone.houseviewingapp.analysis.AnalysisFlow.ARG_HOUSE_ID to houseId
                )
            )
            binding.navigationBar.menu.findItem(R.id.nav_analysis)?.isChecked = true
            sourceIntent.removeExtra(EXTRA_SHOW_ANALYSIS_LOADING)
            sourceIntent.removeExtra(EXTRA_ANALYSIS_SOURCE)
            sourceIntent.removeExtra(com.capstone.houseviewingapp.analysis.AnalysisFlow.ARG_SELECTED_FILE_URI)
            sourceIntent.removeExtra(com.capstone.houseviewingapp.analysis.AnalysisFlow.ARG_ORIGIN_ADDRESS)
            sourceIntent.removeExtra(com.capstone.houseviewingapp.analysis.AnalysisFlow.ARG_HOUSE_ID)
        }

        // NOTE: 알림 접근 권한 안내 바텀시트 표시
        if (sourceIntent.getBooleanExtra(EXTRA_SHOW_NOTIFICATION_ACCESS_GUIDE, false)) {
            supportFragmentManager.executePendingTransactions()
            NotificationAccessGuideBottomSheetFragment().show(
                supportFragmentManager,
                "notification_access_guide"
            )
            sourceIntent.removeExtra(EXTRA_SHOW_NOTIFICATION_ACCESS_GUIDE)
        }

        // NOTE: 등기 변경 감지 다이얼로그 표시
        if (sourceIntent.getBooleanExtra(EXTRA_SHOW_REGISTRY_CHANGE_DIALOG, false)) {
            supportFragmentManager.executePendingTransactions()
            RegistryChangeDetectedDialogFragment().show(
                supportFragmentManager,
                "registry_change_dialog"
            )
            sourceIntent.removeExtra(EXTRA_SHOW_REGISTRY_CHANGE_DIALOG)
        }
    }
}