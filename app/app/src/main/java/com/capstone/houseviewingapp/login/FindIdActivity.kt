package com.capstone.houseviewingapp.login

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
import com.capstone.houseviewingapp.auth.model.FindIdRequest
import com.capstone.houseviewingapp.R
import com.capstone.houseviewingapp.databinding.ActivityFindIdBinding
import kotlinx.coroutines.launch

class FindIdActivity : AppCompatActivity() {
    private lateinit var binding : ActivityFindIdBinding

    private fun isEmailFormatValid(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun firstValidationError(): String? {
        val name = binding.nameEditText.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) return "이름을 입력해 주세요."
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
        val email = binding.emailEditText.text?.toString()?.trim().orEmpty()
        val emailOk = email.isNotBlank() && isEmailFormatValid(email)
        setConfirmEnabled(nameOk && emailOk)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityFindIdBinding.inflate(layoutInflater)
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
//            intent = Intent(this, LoginActivity::class.java)
//            startActivity(intent) // NOTE: 이렇게 작성하면 앱 화면이 쌓여서 나중에 오류 생김
            finish()
        }
        binding.nameEditText.addTextChangedListener { validateInputs() }
        binding.emailEditText.addTextChangedListener { validateInputs() }

        binding.confirmButton.setOnClickListener {
            firstValidationError()?.let { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = FindIdRequest(
                name = binding.nameEditText.text?.toString()?.trim().orEmpty(),
                email = binding.emailEditText.text?.toString()?.trim().orEmpty()
            )

            lifecycleScope.launch {
                val result = AuthRepositoryProvider.repository.findId(request)
                result.onSuccess { res ->
                    FindIDResultFragment(res.loginId).show(supportFragmentManager, "FindIDResultTag")
                }.onFailure {
                    Toast.makeText(this@FindIdActivity, "일치하는 계정을 찾지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        validateInputs()
    }
}