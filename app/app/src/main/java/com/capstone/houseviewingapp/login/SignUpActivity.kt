package com.capstone.houseviewingapp.login

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.core.widget.addTextChangedListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.capstone.houseviewingapp.auth.AuthRepositoryProvider
import com.capstone.houseviewingapp.auth.model.RegisterRequest
import com.capstone.houseviewingapp.data.local.UserProfileLocalStore
import com.capstone.houseviewingapp.data.remote.RemoteApiException
import com.capstone.houseviewingapp.R
import com.capstone.houseviewingapp.databinding.ActivitySignUpBinding
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private var isLoginIdAvailableChecked = false
    private var lastCheckedLoginId = ""

    private fun isEmailFormatValid(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun passwordValidationReasons(password: String): List<String> {
        val reasons = mutableListOf<String>()
        if (password.length < 8) reasons.add("8자 이상이어야 합니다.")
        if (!password.any { it.isLetter() }) reasons.add("영문이 포함되어야 합니다.")
        if (!password.any { it.isDigit() }) reasons.add("숫자가 포함되어야 합니다.")
        return reasons
    }

    private fun updatePasswordGuide(password: String) {
        val neutral = ContextCompat.getColor(this, R.color.textgray)
        val invalid = ContextCompat.getColor(this, R.color.red)
        val valid = ContextCompat.getColor(this, R.color.blue)

        if (password.isBlank()) {
            binding.passwordInfoTextView.text = "영문, 숫자 포함 8자 이상"
            binding.passwordInfoTextView.setTextColor(neutral)
            binding.passwordInfoIcon.imageTintList = ColorStateList.valueOf(neutral)
            return
        }

        val reasons = passwordValidationReasons(password)
        if (reasons.isEmpty()) {
            binding.passwordInfoTextView.text = "사용 가능한 비밀번호입니다."
            binding.passwordInfoTextView.setTextColor(valid)
            binding.passwordInfoIcon.imageTintList = ColorStateList.valueOf(valid)
        } else {
            binding.passwordInfoTextView.text = reasons.joinToString(" ")
            binding.passwordInfoTextView.setTextColor(invalid)
            binding.passwordInfoIcon.imageTintList = ColorStateList.valueOf(invalid)
        }
    }

    private fun setIdStatus(text: String, isPositive: Boolean) {
        binding.idStatusTextView.visibility = android.view.View.VISIBLE
        binding.idStatusTextView.text = text
        val color = if (isPositive) R.color.blue else R.color.red
        binding.idStatusTextView.setTextColor(ContextCompat.getColor(this, color))
    }

    private fun setEmailStatus(text: String, isPositive: Boolean) {
        binding.emailStatusTextView.visibility = android.view.View.VISIBLE
        binding.emailStatusTextView.text = text
        val color = if (isPositive) R.color.blue else R.color.red
        binding.emailStatusTextView.setTextColor(ContextCompat.getColor(this, color))
    }

    private fun hideEmailStatus() {
        binding.emailStatusTextView.visibility = android.view.View.GONE
    }

    private fun updatePasswordMatchStatus(password: String, passwordCheck: String) {
        if (passwordCheck.isBlank()) {
            binding.passwordCheckStatusTextView.visibility = android.view.View.GONE
            return
        }
        val matched = password == passwordCheck
        binding.passwordCheckStatusTextView.visibility = android.view.View.VISIBLE
        binding.passwordCheckStatusTextView.text = if (matched) {
            "비밀번호가 일치합니다."
        } else {
            "비밀번호가 일치하지 않습니다."
        }
        val color = if (matched) R.color.blue else R.color.red
        binding.passwordCheckStatusTextView.setTextColor(ContextCompat.getColor(this, color))
    }

    private fun resetDuplicateCheckStateForId() {
        isLoginIdAvailableChecked = false
        lastCheckedLoginId = ""
        binding.idStatusTextView.visibility = android.view.View.GONE
    }

    private fun firstSignUpValidationError(): String? {
        val name = binding.nameEditText.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) return "이름을 입력해 주세요."

        val id = binding.idEditText.text?.toString()?.trim().orEmpty()
        if (id.isBlank()) return "아이디를 입력해 주세요."
        if (!isLoginIdAvailableChecked || id != lastCheckedLoginId) {
            return "아이디 중복 확인을 해 주세요."
        }

        val email = binding.emailEditText.text?.toString()?.trim().orEmpty()
        if (email.isBlank()) return "이메일을 입력해 주세요."
        if (!isEmailFormatValid(email)) return "이메일 형식이 올바르지 않습니다."

        val password = binding.passwordEditText.text?.toString().orEmpty()
        if (password.isBlank()) return "비밀번호를 입력해 주세요."
        val pwdReasons = passwordValidationReasons(password)
        if (pwdReasons.isNotEmpty()) return pwdReasons.joinToString(" ")

        val passwordCheck = binding.passwordCheckEditText.text?.toString().orEmpty()
        if (passwordCheck.isBlank()) return "비밀번호 확인을 입력해 주세요."
        if (password != passwordCheck) return "비밀번호가 일치하지 않습니다."

        if (!binding.termOfServieceCheckBox.isChecked || !binding.infoCheckBox.isChecked) {
            return "필수 약관에 동의해 주세요."
        }
        return null
    }

    private fun setCheckButtonStyle(enabled: Boolean) {
        binding.checkButton.isEnabled = true
        val color = ContextCompat.getColor(
            this,
            R.color.blue
        )
        binding.checkButton.backgroundTintList = ColorStateList.valueOf(color)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottom = maxOf(systemBars.bottom, ime.bottom)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottom)
            insets
        }
        binding.backButton.setOnClickListener {
            finish()
        }

        fun setConfirmEnabled(enabled: Boolean) {
            binding.confirmButton.isEnabled = true
            val color = androidx.core.content.ContextCompat.getColor(
                this,
                R.color.blue
            )
            binding.confirmButton.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        }

        fun validateInputs() {
            val nameOk = binding.nameEditText.text?.toString()?.trim().orEmpty().isNotBlank()
            val id = binding.idEditText.text?.toString()?.trim().orEmpty()
            val email = binding.emailEditText.text?.toString()?.trim().orEmpty()
            val idOk = id.isNotBlank()
            val emailOk = email.isNotBlank() && isEmailFormatValid(email)
            val password = binding.passwordEditText.text?.toString().orEmpty()
            val passwordCheck = binding.passwordCheckEditText.text?.toString().orEmpty()
            val passwordOk = passwordValidationReasons(password).isEmpty()
            val passwordMatch = password == passwordCheck && passwordCheck.isNotBlank()
            val idDuplicateOk = isLoginIdAvailableChecked && id == lastCheckedLoginId

            updatePasswordGuide(password)
            updatePasswordMatchStatus(password, passwordCheck)

            val requiredTermsOk =
                binding.termOfServieceCheckBox.isChecked && binding.infoCheckBox.isChecked

            setConfirmEnabled(
                nameOk && idOk && emailOk &&
                    passwordOk && passwordMatch &&
                    idDuplicateOk &&
                    requiredTermsOk
            )

            // 중복 확인: 아이디·이메일 입력 및 이메일 형식 통과 시에만 파란색
            setCheckButtonStyle(idOk && email.isNotBlank() && isEmailFormatValid(email))
        }

        // 약관 체크박스 동기화: 전체 동의 ↔ 개별 동의
        var isSyncingAgreementState = false
        fun syncAllCheckboxFromIndividuals() {
            if (isSyncingAgreementState) return
            isSyncingAgreementState = true
            binding.allCheckBox.isChecked =
                binding.termOfServieceCheckBox.isChecked && binding.infoCheckBox.isChecked
            isSyncingAgreementState = false
            validateInputs()
        }

        binding.allCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isSyncingAgreementState) return@setOnCheckedChangeListener
            isSyncingAgreementState = true
            binding.termOfServieceCheckBox.isChecked = isChecked
            binding.infoCheckBox.isChecked = isChecked
            isSyncingAgreementState = false
            validateInputs()
        }

        binding.termOfServieceCheckBox.setOnCheckedChangeListener { _, _ ->
            syncAllCheckboxFromIndividuals()
        }
        binding.infoCheckBox.setOnCheckedChangeListener { _, _ ->
            syncAllCheckboxFromIndividuals()
        }
        binding.nameEditText.addTextChangedListener { validateInputs() }
        binding.idEditText.addTextChangedListener {
            resetDuplicateCheckStateForId()
            validateInputs()
        }
        binding.emailEditText.addTextChangedListener {
            hideEmailStatus()
            validateInputs()
        }
        binding.passwordEditText.addTextChangedListener { validateInputs() }
        binding.passwordCheckEditText.addTextChangedListener { validateInputs() }

        binding.checkButton.setOnClickListener {
            val loginId = binding.idEditText.text?.toString()?.trim().orEmpty()
            val email = binding.emailEditText.text?.toString()?.trim().orEmpty()

            if (loginId.isBlank()) {
                Toast.makeText(this, "아이디를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email.isBlank()) {
                Toast.makeText(this, "이메일을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isEmailFormatValid(email)) {
                setEmailStatus("이메일 형식이 올바르지 않습니다.", false)
                validateInputs()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val idAvailable = AuthRepositoryProvider.repository
                    .isLoginIdAvailable(loginId)
                    .getOrElse {
                        setIdStatus("아이디 확인에 실패했습니다. 다시 시도해 주세요.", false)
                        validateInputs()
                        return@launch
                    }
                if (idAvailable) {
                    setIdStatus("사용 가능한 아이디입니다.", true)
                    isLoginIdAvailableChecked = true
                    lastCheckedLoginId = loginId
                } else {
                    setIdStatus("이미 사용 중인 아이디입니다.", false)
                    isLoginIdAvailableChecked = false
                    lastCheckedLoginId = ""
                }

                val emailAvailable = AuthRepositoryProvider.repository
                    .isEmailAvailable(email)
                    .getOrElse {
                        setEmailStatus("이메일 확인에 실패했습니다. 다시 시도해 주세요.", false)
                        validateInputs()
                        return@launch
                    }
                if (emailAvailable) {
                    setEmailStatus("사용 가능한 이메일입니다.", true)
                } else {
                    setEmailStatus("이미 사용 중인 이메일입니다.", false)
                }

                validateInputs()
            }
        }

        binding.termTextView.setOnClickListener {
            AgreementDialogFragment
                .newInstance(R.string.terms_title, R.string.terms_content)
                .show(supportFragmentManager, "TermsDialog")
        }
        binding.infoTextView.setOnClickListener {
            AgreementDialogFragment
                .newInstance(R.string.privacy_title, R.string.privacy_content)
                .show(supportFragmentManager, "PrivacyDialog")
        }

        binding.confirmButton.setOnClickListener {
            firstSignUpValidationError()?.let { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = RegisterRequest(
                name = binding.nameEditText.text?.toString()?.trim().orEmpty(),
                email = binding.emailEditText.text?.toString()?.trim().orEmpty(),
                loginId = binding.idEditText.text?.toString()?.trim().orEmpty(),
                password = binding.passwordEditText.text?.toString().orEmpty()
            )

            lifecycleScope.launch {
                val result = AuthRepositoryProvider.repository.register(request)
                result.onSuccess {
                    UserProfileLocalStore.save(
                        context = this@SignUpActivity,
                        name = request.name,
                        email = request.email,
                        loginId = request.loginId
                    )
                    Toast.makeText(this@SignUpActivity, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                    finish()
                }.onFailure { e ->
                    val generic =
                        "회원가입에 실패했습니다. 백엔드(8080) 실행·에뮬레이터 네트워크를 확인해 주세요."
                    val msg = when {
                        e.message == "DUPLICATE_LOGIN_ID" -> "이미 사용 중인 아이디입니다."
                        e.message == "DUPLICATE_EMAIL" -> "이미 사용 중인 이메일입니다."
                        e.message == "DUPLICATE_RESOURCE" ->
                            "이미 가입된 정보입니다. 아이디·이메일 중복을 확인해 주세요."
                        e is RemoteApiException -> e.message?.takeIf { it.isNotBlank() } ?: generic
                        else -> generic
                    }
                    Toast.makeText(this@SignUpActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
        }

        updatePasswordGuide("")
        validateInputs()
    }
}