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
import com.capstone.houseviewingapp.R
import com.capstone.houseviewingapp.auth.model.VerifyPasswordRequest
import com.capstone.houseviewingapp.databinding.ActivityFindPasswordBinding
import kotlinx.coroutines.launch

class FindPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFindPasswordBinding

    private fun isEmailFormatValid(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun firstValidationError(): String? {
        val name = binding.nameEditText.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) return "이름을 입력해 주세요."
        val loginId = binding.idEditText.text?.toString()?.trim().orEmpty()
        if (loginId.isBlank()) return "아이디를 입력해 주세요."
        val email = binding.emailEditText.text?.toString()?.trim().orEmpty()
        if (email.isBlank()) return "이메일을 입력해 주세요."
        if (!isEmailFormatValid(email)) return "이메일 형식이 올바르지 않습니다."
        return null
    }

    private fun setConfirmEnabled(enabled: Boolean) {
        binding.confirmButton.isEnabled = true
        val color = androidx.core.content.ContextCompat.getColor(
            this,
            R.color.blue
        )
        binding.confirmButton.backgroundTintList = ColorStateList.valueOf(color)
    }

    private fun validateInputs() {
        val nameOk = binding.nameEditText.text?.toString()?.trim().orEmpty().isNotBlank()
        val idOk = binding.idEditText.text?.toString()?.trim().orEmpty().isNotBlank()
        val email = binding.emailEditText.text?.toString()?.trim().orEmpty()
        val emailOk = email.isNotBlank() && isEmailFormatValid(email)
        setConfirmEnabled(nameOk && idOk && emailOk)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityFindPasswordBinding.inflate(layoutInflater)
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

        // NOTE: 뒤로가기 버튼 (로그인 화면으로)
        binding.backButton.setOnClickListener {
            finish()
        }
        binding.nameEditText.addTextChangedListener { validateInputs() }
        binding.idEditText.addTextChangedListener { validateInputs() }
        binding.emailEditText.addTextChangedListener { validateInputs() }

        binding.confirmButton.setOnClickListener {
            firstValidationError()?.let { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val inputName = binding.nameEditText.text?.toString()?.trim().orEmpty()
            val inputEmail = binding.emailEditText.text?.toString()?.trim().orEmpty()
            val inputLoginId = binding.idEditText.text?.toString()?.trim().orEmpty()

            lifecycleScope.launch {
                val result = AuthRepositoryProvider.repository.verifyPassword(
                    VerifyPasswordRequest(
                        email = inputEmail,
                        name = inputName,
                        loginId = inputLoginId
                    )
                )
                result.onSuccess { resetToken ->
                    startActivity(
                        Intent(this@FindPasswordActivity, ResetPasswordActivity::class.java).apply {
                            putExtra(ResetPasswordActivity.EXTRA_RESET_TOKEN, resetToken)
                        }
                    )
                }.onFailure {
                    Toast.makeText(this@FindPasswordActivity, "입력 정보가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        validateInputs()
    }
}