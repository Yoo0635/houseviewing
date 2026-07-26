package com.capstone.houseviewingapp.registration


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.capstone.houseviewingapp.R
import com.capstone.houseviewingapp.databinding.FragmentHouseInfoStep3Binding
import androidx.core.widget.addTextChangedListener

class HouseInfoStep3Fragment : Fragment(R.layout.fragment_house_info_step3) {
    companion object {
        private const val ARG_QUICK_DIAGNOSIS = "arg_quick_diagnosis"
        /** 빠른 진단에서 Step1에서 닉네임을 이미 받은 경우 Step3에서 닉네임 입력을 숨김 */
        private const val ARG_SHOW_QUICK_NICKNAME = "arg_show_quick_nickname"

        fun newInstance(
            isQuickDiagnosisMode: Boolean,
            showQuickNickname: Boolean = true
        ): HouseInfoStep3Fragment {
            return HouseInfoStep3Fragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_QUICK_DIAGNOSIS, isQuickDiagnosisMode)
                    putBoolean(ARG_SHOW_QUICK_NICKNAME, showQuickNickname)
                }
            }
        }
    }

    private var _binding: FragmentHouseInfoStep3Binding? = null
    private val binding get() = _binding!!

    private var selectedFileUri: Uri? = null // 선택된 파일의 URI를 저장하는 변수

    // NOTE : PDF 파일 선택을 위한 ActivityResultLauncher
    private val pickPDfLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) {
                return@registerForActivityResult
            }

            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            selectedFileUri = uri
            renderSelectedFileState()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHouseInfoStep3Binding.inflate(inflater, container, false)
        val isQuickDiagnosis = arguments?.getBoolean(ARG_QUICK_DIAGNOSIS, false) ?: false
        val showQuickNickname = arguments?.getBoolean(ARG_SHOW_QUICK_NICKNAME, true) ?: true
        val showNicknameArea = isQuickDiagnosis && showQuickNickname
        // 빠른 진단 + Step3 단독(구버전)일 때만 닉네임. Step1→Step3 재사용 시에는 Step1에서 닉네임 처리
        binding.step3NicknameAreaWrapper.visibility =
            if (showNicknameArea) View.VISIBLE else View.GONE
        binding.quickDiagnosisNicknameContainer.visibility =
            if (showNicknameArea) View.VISIBLE else View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.pdfCardView.setOnClickListener {
            pickPDfLauncher.launch(arrayOf("application/pdf")) // NOTE : PDF 파일만 선택하도록 MIME 타입 지정
        }
        // NOTE : 삭제 버튼 클릭시
        binding.attachedFileItem.deleteIcon.setOnClickListener {
            selectedFileUri = null
            renderSelectedFileState()
        }
        binding.quickNicknameEditText.addTextChangedListener { validateAndUpdateNextButton() }
        renderSelectedFileState()
    }

    // NOTE : PDF 파일이 선택되었는지 여부를 반환하는 함수
    fun hasSelectedFile(): Boolean {
        return selectedFileUri != null
    }

    // NOTE : 선택된 PDF 파일의 URI 문자열을 반환하는 함수 (없으면 null)
    fun getSelectedFileUriString(): String? = selectedFileUri?.toString()

    fun getNicknameOrNull(): String? {
        if (arguments?.getBoolean(ARG_QUICK_DIAGNOSIS, false) != true) return null
        if (arguments?.getBoolean(ARG_SHOW_QUICK_NICKNAME, true) != true) return null
        return binding.quickNicknameEditText.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun validateAndUpdateNextButton() {
        val hasFile = selectedFileUri != null
        val isQuick = arguments?.getBoolean(ARG_QUICK_DIAGNOSIS, false) ?: false
        val showQuickNickname = arguments?.getBoolean(ARG_SHOW_QUICK_NICKNAME, true) ?: true
        val nicknameOk =
            !isQuick || !showQuickNickname || binding.quickNicknameEditText.text?.toString()?.trim()
                .orEmpty().isNotBlank()
        (activity as? HouseRegistrationActivity)?.setNextButtonEnabled(hasFile && nicknameOk)
    }

    //TODO : 나중에 백엔드와 연동할 때, getSelectedFileUriString() 대신, selectedFileUri를 서버에 업로드하고, 서버에서 반환된 URL을 저장하도록 수정 필요

    // NOTE : 파일 상태에 따라 UI 업데이트
    private fun renderSelectedFileState() {
        val hasFile = selectedFileUri != null

        binding.emptyFileTextView.visibility = if (hasFile) View.GONE else View.VISIBLE
        binding.attachedFileItem.root.visibility = if (hasFile) View.VISIBLE else View.GONE

        if (hasFile) {
            val uri = selectedFileUri!!
            val fileName = getDisplayName(uri) ?: "문서.pdf"
            val fileSize = getFileSizeText(uri)

            binding.attachedFileItem.fileNameTextView.text = fileName
            binding.attachedFileItem.fileDescTextView.text = "$fileSize  ·  분석 준비 완료"

        }
        validateAndUpdateNextButton()

    }

    // NOTE : URI에서 파일 이름을 읽어오는 함수
    private fun getDisplayName(uri: Uri): String? {
        requireContext().contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        return null
    }

    // NOTE : 파일 크기를 읽어서 사람이 읽기 쉬운 형식으로 변환하는 함수
    private fun getFileSizeText(uri: Uri): String {
        var bytes = 0L
        requireContext().contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    bytes = cursor.getLong(sizeIndex)
                }
            }
        }
        if (bytes < 1024) return "크기 미확인"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1) String.format("%.1f MB", mb) else String.format("%.0f KB", kb)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}