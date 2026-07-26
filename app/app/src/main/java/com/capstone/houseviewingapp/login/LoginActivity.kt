package com.capstone.houseviewingapp.login

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.core.widget.addTextChangedListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.capstone.houseviewingapp.MainActivity
import com.capstone.houseviewingapp.R
import com.capstone.houseviewingapp.auth.AuthRepositoryProvider
import com.capstone.houseviewingapp.auth.model.LoginRequest
import com.capstone.houseviewingapp.data.local.AuthTokenLocalStore
import com.capstone.houseviewingapp.data.local.UserProfileLocalStore
import com.capstone.houseviewingapp.data.remote.RemoteApiException
import com.capstone.houseviewingapp.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        gotoMain()
    }

    private fun validateRequiredInputs(): Boolean {
        val loginId = normalizeLoginId(binding.idEditText.text?.toString())
        val password = binding.passwordEditText.text?.toString().orEmpty()

        if (loginId.isBlank()) {
            Toast.makeText(this, "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show()
            binding.idEditText.requestFocus()
            return false
        }
        if (password.isBlank()) {
            Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            binding.passwordEditText.requestFocus()
            return false
        }
        return true
    }

    private fun normalizeLoginId(raw: String?): String {
        return raw.orEmpty().filterNot { it.isWhitespace() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityLoginBinding.inflate(layoutInflater)
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

        binding.signupTextView.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
        binding.findIDTextView.setOnClickListener {
            startActivity(Intent(this, FindIdActivity::class.java))
        }
        binding.findPWTextView.setOnClickListener {
            startActivity(Intent(this, FindPasswordActivity::class.java))
        }
        binding.idEditText.addTextChangedListener { editable ->
            val current = editable?.toString().orEmpty()
            val normalized = normalizeLoginId(current)
            if (current != normalized) {
                binding.idEditText.setText(normalized)
                binding.idEditText.setSelection(normalized.length)
            }
        }
        binding.passwordEditText.addTextChangedListener { }
        binding.loginButton.setOnClickListener {
            if (!validateRequiredInputs()) return@setOnClickListener

            val normalizedLoginId = normalizeLoginId(binding.idEditText.text?.toString())
            val request = LoginRequest(
                loginId = normalizedLoginId,
                password = binding.passwordEditText.text?.toString().orEmpty()
            )
            lifecycleScope.launch {
                val result = AuthRepositoryProvider.repository.login(request)
                val token = result.getOrElse {
                    val remote = it as? RemoteApiException
                    val msg = when (remote?.code) {
                        "NF001", "AU004" -> "아이디 또는 비밀번호를 확인해 주세요."
                        else -> "로그인에 실패했습니다."
                    }
                    Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                AuthTokenLocalStore.saveTokens(this@LoginActivity, token.accessToken, token.refreshToken)
                AuthTokenLocalStore.saveLoginId(this@LoginActivity, token.loginId)
                AuthRepositoryProvider.repository.me(token.accessToken).getOrNull()?.let { me ->
                    UserProfileLocalStore.save(
                        this@LoginActivity,
                        me.name,
                        me.email,
                        token.loginId
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@LoginActivity,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        return@launch
                    }
                }
                gotoMain()
            }
        }
        binding.loginButton.isEnabled = true
        binding.loginButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.blue)
        )
    }

    private fun gotoMain() {
        if(isNotificationListenerEnabled()) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_SHOW_NOTIFICATION_ACCESS_GUIDE, true)
            })
        }
        finish()
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val pkg = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        for (name in flat.split(":")) {
            if (TextUtils.isEmpty(name)) continue
            val cn = ComponentName.unflattenFromString(name) ?: continue
            if (cn.packageName == pkg) return true
        }
        return false
    }
}