package com.capstone.houseviewingapp.login

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.widget.addTextChangedListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.capstone.houseviewingapp.auth.AuthRepositoryProvider
import com.capstone.houseviewingapp.auth.model.ResetPasswordRequest
import com.capstone.houseviewingapp.R
import com.capstone.houseviewingapp.databinding.ActivityResetPasswordBinding
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_RESET_TOKEN = "extra_reset_token"
    }

    private lateinit var binding: ActivityResetPasswordBinding
    private var resetToken: String = ""

    private fun firstValidationError(): String? {
        val password = binding.passwordEditText.text?.toString().orEmpty()
        val passwordCheck = binding.passwordCheckEditText.text?.toString().orEmpty()
        if (password.isBlank()) return "비밀번호를 입력해 주세요."
        if (password.length < 8) return "비밀번호는 영문, 숫자 포함 8자 이상으로 입력해 주세요."
        if (!password.any { it.isLetter() } || !password.any { it.isDigit() }) {
            return "비밀번호는 영문, 숫자 포함 8자 이상으로 입력해 주세요."
        }
        if (passwordCheck.isBlank()) return "비밀번호 확인을 입력해 주세요."
        if (password != passwordCheck) return "비밀번호가 일치하지 않습니다."
        return null
    }

    private fun setConfirmButtonStyleAlwaysEnabled() {
        binding.confirmButton.isEnabled = true
        val color = androidx.core.content.ContextCompat.getColor(
            this,
            R.color.blue
        )
        binding.confirmButton.backgroundTintList = ColorStateList.valueOf(color)
    }

    private fun validateInputs() {
        val password = binding.passwordEditText.text?.toString().orEmpty()
        val passwordCheck = binding.passwordCheckEditText.text?.toString().orEmpty()
        val validColor = androidx.core.content.ContextCompat.getColor(this, R.color.blue)
        val invalidColor = androidx.core.content.ContextCompat.getColor(this, R.color.red)
        val neutralColor = androidx.core.content.ContextCompat.getColor(this, R.color.textgray)

        val hasMinLength = password.length >= 8
        val hasLetterAndNumber = password.any { it.isLetter() } && password.any { it.isDigit() }
        val passwordOk = hasMinLength && hasLetterAndNumber

        if (password.isBlank()) {
            binding.passwordInfoTextView.text = "영문, 숫자 포함 8자 이상"
            binding.passwordInfoTextView.setTextColor(neutralColor)
            binding.passwordInfoIcon.imageTintList = ColorStateList.valueOf(neutralColor)
        } else if (passwordOk) {
            binding.passwordInfoTextView.text = "사용 가능한 비밀번호입니다."
            binding.passwordInfoTextView.setTextColor(validColor)
            binding.passwordInfoIcon.imageTintList = ColorStateList.valueOf(validColor)
        } else {
            binding.passwordInfoTextView.text = "영문, 숫자 포함 8자 이상"
            binding.passwordInfoTextView.setTextColor(invalidColor)
            binding.passwordInfoIcon.imageTintList = ColorStateList.valueOf(invalidColor)
        }

        if (passwordCheck.isBlank()) {
            binding.passwordCheckStatusLayout.visibility = android.view.View.GONE
        } else {
            val matched = password == passwordCheck && passwordOk
            binding.passwordCheckStatusLayout.visibility = android.view.View.VISIBLE
            binding.passwordCheckStatusTextView.text = if (matched) {
                "비밀번호가 일치합니다."
            } else {
                "비밀번호가 일치하지 않습니다."
            }
            val statusColor = if (matched) validColor else invalidColor
            binding.passwordCheckStatusTextView.setTextColor(statusColor)
            binding.passwordCheckStatusIcon.imageTintList = ColorStateList.valueOf(statusColor)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        resetToken = intent.getStringExtra(EXTRA_RESET_TOKEN).orEmpty()
        if (resetToken.isBlank()) {
            Toast.makeText(this, "인증 정보가 없습니다. 비밀번호 찾기부터 다시 진행해 주세요.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottom = maxOf(systemBars.bottom, ime.bottom)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        binding.passwordEditText.addTextChangedListener { validateInputs() }
        binding.passwordCheckEditText.addTextChangedListener { validateInputs() }
        setConfirmButtonStyleAlwaysEnabled()

        //NOTE : 비밀번호 변경 완료 버튼
        binding.confirmButton.setOnClickListener {
            firstValidationError()?.let { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pwd = binding.passwordEditText.text?.toString().orEmpty()
            val pwdCheck = binding.passwordCheckEditText.text?.toString().orEmpty()
            val request = ResetPasswordRequest(
                refreshToken = resetToken,
                newPassword = pwd,
                confirmPassword = pwdCheck
            )
            lifecycleScope.launch {
                val result = AuthRepositoryProvider.repository.resetPassword(request)
                result.onSuccess {
                    val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    Toast.makeText(this@ResetPasswordActivity, "비밀번호 변경이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                }.onFailure { e ->
                    val msg = when (e.message) {
                        "UNAUTHORIZED" -> "인증이 필요합니다. 다시 로그인해 주세요."
                        "INVALID_PASSWORD" -> "유효하지 않은 비밀번호입니다."
                        "PASSWORD_MISMATCH" -> "비밀번호가 일치하지 않습니다."
                        else -> "비밀번호 변경에 실패했습니다."
                    }
                    Toast.makeText(this@ResetPasswordActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
        validateInputs()
    }
}