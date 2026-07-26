package com.capstone.houseviewingapp.registration

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.capstone.houseviewingapp.R
import com.capstone.houseviewingapp.databinding.FragmentHouseInfoStep2Binding
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.DecimalFormat

class HouseInfoStep2Fragment : Fragment(R.layout.fragment_house_info_step2) {
    private var _binding: FragmentHouseInfoStep2Binding? = null
    private val binding get() = _binding!!
    // [B] setText() 재호출 무한루프 방지 플래그
    private var isFormattingMoveInDate = false
    private var isFormattingConfirmDate = false
    private val currencyFormattingStates = mutableMapOf<Int, Boolean>()
    private val currencyFormatter = DecimalFormat("#,###")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHouseInfoStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    data class Step2Data(
        val contractType: ContractType, // 계약 유형 (전세/월세)
        val deposit: Long, // 보증금
        val monthlyAmount: Long, // 월세 금액 (월세인 경우) -> 전세면 0
        val maintenanceFee: Long, // 관리비 (미포함이면 0)
        val moveDate: String, // 전입일 (yyyy-MM-dd)
        val confirmDate: String // 확정일자 (yyyy-MM-dd)
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.noManageCheckBox.setOnCheckedChangeListener {
            _, isChecked ->
            binding.monthManageEditText.isEnabled = !isChecked
            if(isChecked) {
                binding.monthManageEditText.setText("0")
                binding.monthManageEditText.isEnabled = false
                binding.monthManageEditText.isFocusable = false
                binding.monthManageEditText.isFocusableInTouchMode = false
                binding.monthManageInputBox.alpha = 0.45f
                binding.monthManageWonText.alpha = 0.45f
            } else {
                binding.monthManageEditText.isEnabled = true
                binding.monthManageEditText.isFocusable = true
                binding.monthManageEditText.isFocusableInTouchMode = true
                binding.monthManageInputBox.alpha = 1f
                binding.monthManageWonText.alpha = 1f
            }
            validateStep2Input()
        }

        binding.fixedDateIcon.setOnClickListener {
            showMoveInDatePicker()
        }
        setupMoveInDateInputFormat()

        binding.confirmDateIcon.setOnClickListener {
            showConfirmDatePicker()
        }
        setupConfirmDateInputFormat()
        setupCurrencyInputFormats()

        setupRentTypeToggle()

        binding.depositEditText.addTextChangedListener { validateStep2Input() }
        binding.monthEditText.addTextChangedListener { validateStep2Input() }
        binding.monthManageEditText.addTextChangedListener { validateStep2Input() }
        binding.yearDepositEditText.addTextChangedListener { validateStep2Input() }
        binding.yearManageEditText.addTextChangedListener { validateStep2Input() }
        binding.moveInDateEditText.addTextChangedListener { validateStep2Input() }
        binding.confirmDateEditText.addTextChangedListener { validateStep2Input() }
        binding.yearNoManageCheckBox.setOnCheckedChangeListener { _, isChecked ->
            binding.yearManageEditText.isEnabled = !isChecked
            if (isChecked) {
                binding.yearManageEditText.setText("0")
                binding.yearManageEditText.isEnabled = false
                binding.yearManageEditText.isFocusable = false
                binding.yearManageEditText.isFocusableInTouchMode = false
                binding.yearManageInputBox.alpha = 0.45f
                binding.yearManageWonText.alpha = 0.45f
            } else {
                binding.yearManageEditText.isEnabled = true
                binding.yearManageEditText.isFocusable = true
                binding.yearManageEditText.isFocusableInTouchMode = true
                binding.yearManageInputBox.alpha = 1f
                binding.yearManageWonText.alpha = 1f
            }
            validateStep2Input()
        }

        validateStep2Input()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // NOTE : 계약 정보 필수 입력이 모두 채워졌는지 검사하고, 다음 버튼 활성/비활성 설정 (Step1·Step3와 동일 패턴)
    private fun validateStep2Input() {
        val canGoNext = collectStep2Data() != null
        (activity as? HouseRegistrationActivity)?.setNextButtonEnabled(canGoNext)
    }

    fun collectStep2Data(): Step2Data? {

        val isMonthly = binding.toggleGroup.checkedButtonId == R.id.monthButton // 월세 여부
        val contractType = if (isMonthly) ContractType.WOLSE else ContractType.JEONSE // 계약 유형 결정

        val depositText = if (isMonthly) { //
            binding.depositEditText.text?.toString() // 월세일 때는 월세 카드의 보증금 입력창
        } else {
            binding.yearDepositEditText.text?.toString() // 전세일 때는 전세 카드의 보증금 입력창
        }

        val deposit = parseCurrencyToLong(depositText) ?: return null // 보증금은 필수 입력

        val monthlyAmount = if (isMonthly) {
            parseCurrencyToLong(binding.monthEditText.text?.toString()) ?: return null // 월세 금액은 월세일 때 필수 입력
        } else {
            0L // 전세일 때는 월세 금액 0으로 설정
        }

        val maintenanceFee = if (isMonthly) {
            if(binding.noManageCheckBox.isChecked) 0L // "관리비 없음" 체크 여부 -> 0원
            else parseCurrencyToLong(binding.monthManageEditText.text?.toString()) ?: return null // 체크가 안되어있으면 필수 입력
        } else {
            if(binding.yearNoManageCheckBox.isChecked) 0L
            else parseCurrencyToLong(binding.yearManageEditText.text?.toString()) ?: return null // 체크가 안되어있으면 필수 입력 -> Long 변환 실패하면 null 반환
        }

        val moveDate = binding.moveInDateEditText.text?.toString()?.trim().orEmpty() // null 이면 빈 문자열로 처리
        val confirmDate = binding.confirmDateEditText.text?.toString()?.trim().orEmpty()

        if (moveDate.isBlank()) return null // 이것을 통해 빈문자열을 null로 반환
        if (confirmDate.isBlank()) return null


        return Step2Data(
            contractType = contractType,
            deposit = deposit,
            monthlyAmount = monthlyAmount,
            maintenanceFee = maintenanceFee,
            moveDate = moveDate,
            confirmDate = confirmDate
        )
    }

    private fun setupRentTypeToggle() {
        showMonthlySection()
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
           if(!isChecked) return@addOnButtonCheckedListener //
            if(checkedId == R.id.monthButton) {
                showMonthlySection()
            } else if(checkedId == R.id.yearButton) {
                showYearlySection()
            }
            validateStep2Input()
        }
    }

    private fun showMonthlySection() {
        binding.monthSection.visibility = View.VISIBLE
        binding.yearSection.visibility = View.GONE
    }

    private fun showYearlySection() {
        binding.monthSection.visibility = View.GONE
        binding.yearSection.visibility = View.VISIBLE
    }

    // NOTE : 전입일 선택 다이얼로그 띄우는 함수
    private fun showMoveInDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("전입일 선택")
            .build()

        picker.addOnPositiveButtonClickListener { selectionMillis ->
            val formatted = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREA)
                .format(java.util.Date(selectionMillis))
            binding.moveInDateEditText.setText(formatted)
        }
        picker.show(parentFragmentManager, "MOVE_IN_DATE_PICKER")
    }

    /**
     * yyyy-MM-dd 자동 하이픈 후 커서가 하이픈 **앞**에 남아 다음 글자가 이상하게 들어가는 문제 방지:
     * 방금 입력한 자릿수 기준으로, 필요하면 하이픈까지 건너뛴 위치에 커서를 둔다.
     */
    private fun cursorAfterDigits(formatted: String, digitCountBeforeCursor: Int): Int {
        if (digitCountBeforeCursor <= 0) return 0
        var seen = 0
        for (i in formatted.indices) {
            if (formatted[i].isDigit()) {
                seen++
                if (seen >= digitCountBeforeCursor) {
                    var pos = i + 1
                    while (pos < formatted.length && formatted[pos] == '-') pos++
                    return pos
                }
            }
        }
        return formatted.length
    }

    private fun applyYyyyMmDdMask(editText: EditText, s: Editable?, formatting: () -> Boolean, setFormatting: (Boolean) -> Unit) {
        if (formatting()) return
        val str = s?.toString().orEmpty()
        val sel = editText.selectionStart.coerceIn(0, str.length)
        val digitsBeforeCursor = str.take(sel).count { it.isDigit() }
        val digitsOnly = str.filter { it.isDigit() }.take(8)
        val formatted = buildString {
            digitsOnly.forEachIndexed { index, c ->
                append(c)
                // 다음 자릿수가 실제로 있을 때만 하이픈 삽입 (예: 2025 입력 시 '-' 미삽입)
                if ((index == 3 && digitsOnly.length > 4) || (index == 5 && digitsOnly.length > 6)) {
                    append('-')
                }
            }
        }
        if (str == formatted) return

        setFormatting(true)
        try {
            editText.setText(formatted)
            val newPos = cursorAfterDigits(formatted, digitsBeforeCursor.coerceIn(0, digitsOnly.length))
            editText.setSelection(newPos.coerceIn(0, formatted.length))
        } finally {
            setFormatting(false)
        }
    }

    // NOTE : 전입일 EditText에 yyyy-MM-dd 형식 자동 포맷팅 적용하는 함수
    private fun setupMoveInDateInputFormat() {
        binding.moveInDateEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                applyYyyyMmDdMask(
                    binding.moveInDateEditText,
                    s,
                    { isFormattingMoveInDate },
                    { isFormattingMoveInDate = it }
                )
            }
        })
    }

    // NOTE : 확정일 선택 다이얼로그 띄우는 함수
    private fun showConfirmDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("확정일자 선택")
            .build()

        picker.addOnPositiveButtonClickListener { selectionMillis ->
            val formatted = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREA)
                .format(java.util.Date(selectionMillis))
            binding.confirmDateEditText.setText(formatted)
        }
        picker.show(parentFragmentManager, "CONFIRM_DATE_PICKER")
    }

    // NOTE : 확정일자 EditText에 yyyy-MM-dd 형식 자동 포맷팅 적용하는 함수
    private fun setupConfirmDateInputFormat() {
        binding.confirmDateEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                applyYyyyMmDdMask(
                    binding.confirmDateEditText,
                    s,
                    { isFormattingConfirmDate },
                    { isFormattingConfirmDate = it }
                )
            }
        })
    }

    private fun setupCurrencyInputFormats() {
        val targets = listOf(
            binding.depositEditText,
            binding.monthEditText,
            binding.monthManageEditText,
            binding.yearDepositEditText,
            binding.yearManageEditText
        )
        targets.forEach { editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(s: Editable?) {
                    applyCurrencyMask(editText, s)
                }
            })
        }
    }

    private fun applyCurrencyMask(editText: EditText, s: Editable?) {
        val key = editText.id
        if (currencyFormattingStates[key] == true) return

        val text = s?.toString().orEmpty()
        val selection = editText.selectionStart.coerceIn(0, text.length)
        val digitsBeforeCursor = text.take(selection).count { it.isDigit() }
        val digits = text.filter { it.isDigit() }
        if (digits.isEmpty()) {
            if (text.isNotEmpty()) {
                currencyFormattingStates[key] = true
                try {
                    editText.setText("")
                } finally {
                    currencyFormattingStates[key] = false
                }
            }
            return
        }

        val numeric = digits.toLongOrNull() ?: return
        val formatted = currencyFormatter.format(numeric)
        if (formatted == text) return

        currencyFormattingStates[key] = true
        try {
            editText.setText(formatted)
            editText.setSelection(findCursorAfterDigits(formatted, digitsBeforeCursor))
        } finally {
            currencyFormattingStates[key] = false
        }
    }

    private fun findCursorAfterDigits(formatted: String, digitCountBeforeCursor: Int): Int {
        if (digitCountBeforeCursor <= 0) return 0
        var seen = 0
        for (i in formatted.indices) {
            if (formatted[i].isDigit()) {
                seen++
                if (seen >= digitCountBeforeCursor) {
                    return i + 1
                }
            }
        }
        return formatted.length
    }

    private fun parseCurrencyToLong(raw: String?): Long? {
        val digits = raw.orEmpty().filter { it.isDigit() }
        if (digits.isBlank()) return null
        return digits.toLongOrNull()
    }
}