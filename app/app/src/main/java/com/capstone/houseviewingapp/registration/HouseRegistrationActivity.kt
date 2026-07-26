package com.capstone.houseviewingapp.registration

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.capstone.houseviewingapp.MainActivity
import com.capstone.houseviewingapp.R
import com.capstone.houseviewingapp.analysis.AnalysisRepositoryProvider
import com.capstone.houseviewingapp.data.local.AuthTokenLocalStore
import com.capstone.houseviewingapp.data.local.HouseLocalStore
import com.capstone.houseviewingapp.data.local.model.HouseDetailItem
import com.capstone.houseviewingapp.data.remote.NetworkModule
import com.capstone.houseviewingapp.data.remote.RemoteApiException
import com.capstone.houseviewingapp.data.remote.executeApi
import com.capstone.houseviewingapp.databinding.ActivityHouseRegistrationBinding
import com.capstone.houseviewingapp.registration.remote.ContractRegisterRequest
import com.capstone.houseviewingapp.registration.remote.HouseRegisterRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HouseRegistrationActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HouseRegistration"
        const val EXTRA_QUICK_DIAGNOSIS_MODE = "extra_quick_diagnosis_mode"
        const val EXTRA_EDIT_HOUSE_ID = "extra_edit_house_id"
    }

    private lateinit var binding: ActivityHouseRegistrationBinding
    private val vm: HouseRegistrationViewModel by viewModels()
    private var currentStep = 1 // TODO: 현재 단계에 따라 이 값을 업데이트
    private var isQuickDiagnosisMode = false
    private var isSubmittingRegistration = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHouseRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isQuickDiagnosisMode = intent.getBooleanExtra(EXTRA_QUICK_DIAGNOSIS_MODE, false)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottom = maxOf(systemBars.bottom, ime.bottom)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottom)
            insets
        }

        if (savedInstanceState == null) {
            if (isQuickDiagnosisMode) {
                // 빠른 진단: 기존 Step1(주소·닉네임) → Step3(PDF만), Step2(계약) 생략
                currentStep = 1
                supportFragmentManager.beginTransaction()
                    .replace(R.id.registerationFragmentContainer, HouseInfoStep1Fragment())
                    .commit()
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.registerationFragmentContainer, HouseInfoStep1Fragment())
                    .commit()
            }
            updateStepUi()
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val top = supportFragmentManager.findFragmentById(R.id.registerationFragmentContainer)
            currentStep = when (top) {
                is HouseInfoStep1Fragment -> 1
                is HouseInfoStep2Fragment -> 2
                is HouseInfoStep3Fragment -> 3
                else -> currentStep
            }
            updateStepUi()
        }

        binding.nextButton.setOnClickListener {
            //TODO: 다음 버튼 클릭 시 다음 단계로 이동하는 로직 구현
            if (currentStep == 1) {
                val step1 = supportFragmentManager
                    .findFragmentById(R.id.registerationFragmentContainer) as? HouseInfoStep1Fragment
                    ?: return@setOnClickListener

                val input =
                    step1.collectStep1Data() ?: return@setOnClickListener // null이면 다음 단계로 넘어가지 않음

                vm.updateStep1(
                    nickname = input.nickname,
                    originAddress = input.originAddress,
                    detailAddress = input.detailAddress
                )

                if (isQuickDiagnosisMode) {
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.registerationFragmentContainer,
                            HouseInfoStep3Fragment.newInstance(
                                isQuickDiagnosisMode = true,
                                showQuickNickname = false
                            )
                        )
                        .addToBackStack(null)
                        .commit()
                    currentStep = 3
                } else {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.registerationFragmentContainer, HouseInfoStep2Fragment())
                        .addToBackStack(null)
                        .commit()
                    currentStep = 2
                }
            } else if (currentStep == 2) {
                val step2 = supportFragmentManager
                    .findFragmentById(R.id.registerationFragmentContainer) as? HouseInfoStep2Fragment
                    ?: return@setOnClickListener

                val input = step2.collectStep2Data() ?: return@setOnClickListener
                vm.updateStep2(
                    contractType = input.contractType,
                    deposit = input.deposit,
                    monthlyAmount = input.monthlyAmount,
                    maintenanceFee = input.maintenanceFee,
                    moveDate = input.moveDate,
                    confirmDate = input.confirmDate
                )

                supportFragmentManager.beginTransaction()
                    .replace(R.id.registerationFragmentContainer, HouseInfoStep3Fragment.newInstance(false))
                    .addToBackStack(null)
                    .commit()
                currentStep = 3
            } else if (currentStep == 3) {
                val step3 = supportFragmentManager
                    .findFragmentById(R.id.registerationFragmentContainer) as? HouseInfoStep3Fragment
                    ?: return@setOnClickListener
                if (!step3.hasSelectedFile()) {
                    return@setOnClickListener
                }
                if (isQuickDiagnosisMode) {
                    val nickname = step3.getNicknameOrNull()
                        ?: vm.draft.value.nickname.trim()
                    val selectedFileUri = step3.getSelectedFileUriString().orEmpty()
                    val originAddress = vm.draft.value.originAddress
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(MainActivity.EXTRA_SHOW_ANALYSIS_LOADING, true)
                        putExtra(MainActivity.EXTRA_ANALYSIS_SOURCE, com.capstone.houseviewingapp.analysis.AnalysisFlow.SOURCE_MANUAL)
                        putExtra(
                            com.capstone.houseviewingapp.analysis.AnalysisFlow.ARG_HOUSE_NICKNAME,
                            nickname
                        )
                        putExtra(
                            com.capstone.houseviewingapp.analysis.AnalysisFlow.ARG_SELECTED_FILE_URI,
                            selectedFileUri
                        )
                        putExtra(
                            com.capstone.houseviewingapp.analysis.AnalysisFlow.ARG_ORIGIN_ADDRESS,
                            originAddress
                        )
                    }
                    startActivity(intent)
                    finish()
                    return@setOnClickListener
                }

                // 기존 집 등록 플로우
                vm.updateStep3(step3.getSelectedFileUriString())
                val draft = vm.draft.value
                startHouseRegisterParsingFlow(draft)
            }
            updateStepUi()
        }

        binding.backButton.setOnClickListener {
            if (isQuickDiagnosisMode) {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    finish()
                }
                return@setOnClickListener
            }

            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                finish()
            }
        }

    }

    private fun updateStepUi() {
        if (isQuickDiagnosisMode) {
            binding.registrationProgressBar.visibility = View.VISIBLE
            binding.registrationProgressBar.max = 100
            binding.stepTextView.visibility = View.VISIBLE
            binding.toolBarTextView.text = "빠른 진단"
            when (currentStep) {
                1 -> {
                    binding.stepTextView.text = "1 / 2 단계"
                    binding.registrationProgressBar.progress = 50
                    binding.nextButton.text = "다음"
                    // Step1에서 입력 유효성에 따라 버튼 활성화
                }
                3 -> {
                    binding.stepTextView.text = "2 / 2 단계"
                    binding.registrationProgressBar.progress = 100
                    binding.nextButton.text = "분석 시작"
                    setNextButtonEnabled(false) // Step3에서 PDF 선택 시 활성화
                }
                else -> {
                    binding.stepTextView.text = ""
                    binding.nextButton.text = "다음"
                }
            }
            return
        }

        binding.registrationProgressBar.visibility = View.VISIBLE
        binding.registrationProgressBar.max = 100
        val progress = when (currentStep) {
            1 -> 34
            2 -> 67
            3 -> 100
            else -> 0
        }
        binding.registrationProgressBar.progress = progress
        binding.stepTextView.text = "$currentStep / 3 단계"

        if (currentStep == 1) {
            binding.toolBarTextView.text = "주택 기본 정보 입력"
            setNextButtonEnabled(true)
        }
        if (currentStep == 2) {
            binding.toolBarTextView.text = "계약 정보 입력"
            binding.nextButton.text = "다음"
            setNextButtonEnabled(false)
        }
        if (currentStep == 3) {
            binding.nextButton.text = "문서 검증"
            setNextButtonEnabled(false)
        }
    }

    fun setNextButtonEnabled(enabled: Boolean) {
        binding.nextButton.isEnabled = enabled // NOTE : 다음 버튼 활성화/비활성화 설정

        val colorRes = if (enabled) R.color.blue else R.color.icongray // 활성화 여부에 따른 색상 리소스 선택

        val colorInt =
            androidx.core.content.ContextCompat.getColor(this, colorRes) // 색상 리소스에서 실제 색상 값 가져오기

        binding.nextButton.backgroundTintList =
            android.content.res.ColorStateList.valueOf(colorInt) // 버튼 배경 색상 변경
    }

    private fun startHouseRegisterParsingFlow(draft: HouseRegistrationDraft) {
        if (isSubmittingRegistration) return
        isSubmittingRegistration = true
        setNextButtonEnabled(false)
        RegistrationParsingDialogFragment()
            .show(supportFragmentManager, RegistrationParsingDialogFragment.TAG)

        lifecycleScope.launch {
            val token = AuthTokenLocalStore.getAccessToken(this@HouseRegistrationActivity).orEmpty()
            if (token.isBlank()) {
                finishRegistrationWithError("로그인 정보가 없습니다. 다시 로그인해 주세요.")
                return@launch
            }

            val houseRequest = HouseRegisterRequest(
                nickname = draft.nickname,
                originAddress = draft.originAddress
            )
            val houseResult = NetworkModule.houseApi
                .register(bearer(token), houseRequest)
                .executeApi()
            val houseResponse = houseResult.getOrElse {
                finishRegistrationWithError(mapRegistrationErrorMessage(it))
                return@launch
            }

            val contractRequest = ContractRegisterRequest(
                houseId = houseResponse.houseId,
                contractType = toBackendContractType(draft.contractType),
                deposit = draft.deposit,
                monthlyAmount = draft.monthlyAmount,
                maintenanceFee = draft.maintenanceFee,
                moveDate = draft.moveDate,
                confirmDate = draft.confirmDate
            )
            val contractResult = NetworkModule.contractApi
                .register(bearer(token), contractRequest)
                .executeApi()
            contractResult.getOrElse {
                finishRegistrationWithError(mapRegistrationErrorMessage(it))
                return@launch
            }

            val resolvedLtv = runPostContractAnalysisIfPossible(
                accessToken = token,
                houseId = houseResponse.houseId,
                draft = draft
            )
            val detailItem = HouseDetailItem(
                houseId = houseResponse.houseId,
                homeName = draft.nickname,
                originAddress = draft.originAddress,
                detailAddress = draft.detailAddress,
                documentUri = draft.documentUri,
                contractType = draft.contractType.name,
                deposit = draft.deposit,
                monthlyAmount = draft.monthlyAmount,
                maintenanceFee = draft.maintenanceFee,
                moveDate = draft.moveDate,
                confirmDate = draft.confirmDate,
                ltv = resolvedLtv
            )
            HouseLocalStore.addHouseDetail(this@HouseRegistrationActivity, detailItem)
            if (resolvedLtv == null && !draft.documentUri.isNullOrBlank()) {
                Toast.makeText(
                    this@HouseRegistrationActivity,
                    "집 등록은 완료되었지만 LTV 계산은 아직 반영되지 않았습니다. 분석 메뉴에서 다시 시도해 주세요.",
                    Toast.LENGTH_LONG
                ).show()
            }
            dismissParsingDialog()
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun bearer(accessToken: String): String = "Bearer $accessToken"

    private fun toBackendContractType(contractType: ContractType): String {
        return when (contractType) {
            ContractType.JEONSE -> "JEONSE"
            ContractType.WOLSE -> "MONTHLY"
        }
    }

    private fun finishRegistrationWithError(message: String) {
        dismissParsingDialog()
        isSubmittingRegistration = false
        setNextButtonEnabled(true)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun mapRegistrationErrorMessage(throwable: Throwable): String {
        val remote = throwable as? RemoteApiException
        return when (remote?.code) {
            "NF002" -> "주소를 찾을 수 없습니다. 도로명 주소를 정확히 입력해 주세요."
            "AU001", "AU002", "AU003", "AU005", "AU006" -> "인증이 만료되었습니다. 다시 로그인해 주세요."
            else -> "등록 처리에 실패했습니다. 잠시 후 다시 시도해 주세요."
        }
    }

    private suspend fun runPostContractAnalysisIfPossible(
        accessToken: String,
        houseId: Long,
        draft: HouseRegistrationDraft
    ): Int? {
        val fileUri = draft.documentUri?.trim().orEmpty()
        if (fileUri.isBlank()) return null

        val analysisResult = AnalysisRepositoryProvider.repository.postContractDiagnoses(
            context = this,
            accessToken = accessToken,
            houseId = houseId,
            fileUri = fileUri
        )
        val analysisError = analysisResult.exceptionOrNull() as? RemoteApiException
        if (analysisResult.isFailure) {
            Log.w(
                TAG,
                "postContractDiagnoses failed houseId=$houseId code=${analysisError?.code} " +
                    "http=${analysisError?.statusCode} msg=${analysisError?.message}"
            )
        }

        // 등록 직후 /analyses 문자열 매칭이 아니라, houseId 단건 조회로 LTV를 확정한다.
        repeat(12) { attempt ->
            val houseResult = NetworkModule.houseApi
                .getHouse(bearer(accessToken), houseId)
                .executeApi()
            val house = houseResult.getOrNull()
            val houseError = houseResult.exceptionOrNull() as? RemoteApiException
            val ltvScore = house?.ltvScore
            if (ltvScore != null || attempt == 11) return ltvScore

            if (houseError?.code == "NF007") {
                // 단건 조회는 분석기록이 없으면 NF007로 실패하므로 목록 API에서 houseId를 다시 확인한다.
                val housesResult = NetworkModule.houseApi
                    .getHouses(bearer(accessToken))
                    .executeApi()
                val listLtv = housesResult.getOrNull()
                    ?.firstOrNull { it.houseId == houseId }
                    ?.ltvScore
                if (listLtv != null) return listLtv
            }
            Log.d(
                TAG,
                "ltv poll[$attempt] houseId=$houseId ltv=null getHouseCode=${houseError?.code} " +
                    "getHouseHttp=${houseError?.statusCode}"
            )
            delay(1000L)
        }
        Log.e(TAG, "ltv unresolved after polling houseId=$houseId")
        return null
    }

    private fun dismissParsingDialog() {
        (supportFragmentManager.findFragmentByTag(RegistrationParsingDialogFragment.TAG) as? RegistrationParsingDialogFragment)
            ?.dismissAllowingStateLoss()
    }
}