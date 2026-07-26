package com.capstone.houseviewingapp.my

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.capstone.houseviewingapp.R
import com.capstone.houseviewingapp.auth.AuthRepositoryProvider
import com.capstone.houseviewingapp.data.local.AuthTokenLocalStore
import com.capstone.houseviewingapp.data.local.UserProfileLocalStore
import com.capstone.houseviewingapp.databinding.ActivityProfileEditBinding
import com.capstone.houseviewingapp.login.LoginActivity
import kotlinx.coroutines.launch

class ProfileEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileEditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottom = maxOf(systemBars.bottom, ime.bottom)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottom)
            insets
        }

        binding.backButton.setOnClickListener { finish() }

        binding.logoutCardView.setOnClickListener {
            LogoutConfirmDialogFragment()
                .show(supportFragmentManager, "LogoutConfirmDialog")
        }
    }

    fun performLogout() {
        lifecycleScope.launch {
            val accessToken = AuthTokenLocalStore.getAccessToken(this@ProfileEditActivity)
            if (!accessToken.isNullOrBlank()) {
                AuthRepositoryProvider.repository.logout(accessToken)
            }
            AuthTokenLocalStore.clear(this@ProfileEditActivity)
            UserProfileLocalStore.clear(this@ProfileEditActivity)

            val intent = Intent(this@ProfileEditActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
