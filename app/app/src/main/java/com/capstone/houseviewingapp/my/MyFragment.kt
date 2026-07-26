package com.capstone.houseviewingapp.my

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.capstone.houseviewingapp.MainActivity
import com.capstone.houseviewingapp.R
import com.capstone.houseviewingapp.auth.AuthRepositoryProvider
import com.capstone.houseviewingapp.data.local.AuthTokenLocalStore
import com.capstone.houseviewingapp.data.local.BillingLocalStore
import com.capstone.houseviewingapp.data.local.UserProfileLocalStore
import com.capstone.houseviewingapp.databinding.FragmentMyBinding
import kotlinx.coroutines.launch

class MyFragment : Fragment(R.layout.fragment_my) {
    private var _binding: FragmentMyBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentFragmentManager.setFragmentResultListener(
            MainActivity.RESULT_BOTTOM_REFRESH,
            viewLifecycleOwner
        ) { _, result ->
            val targetId = result.getInt(MainActivity.RESULT_KEY_TARGET_ID, -1)
            if (targetId == R.id.nav_my) {
                renderFeeState()
                renderProfileState()
            }
        }
        renderFeeState()
        renderProfileState()

        binding.profileEditCardView.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileEditActivity::class.java))
        }

        binding.feeCardView.setOnClickListener {
            toggleFeeState()
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            renderFeeState()
            renderProfileState()
        }
    }

    private fun renderFeeState() {
        val isPremium = BillingLocalStore.isPremium(requireContext())
        binding.feeTextView.text = if (isPremium) {
            "SafeHome Premium"
        } else {
            "SafeHome Free"
        }
    }

    private fun toggleFeeState() {
        val context = requireContext()
        val nextIsPremium = !BillingLocalStore.isPremium(context)
        BillingLocalStore.setPremium(context, nextIsPremium)
        renderFeeState()
        val message = if (nextIsPremium) {
            "요금제가 Premium으로 변경되었습니다."
        } else {
            "요금제가 Free로 변경되었습니다."
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun renderProfileState() {
        val context = requireContext()
        val accessToken = AuthTokenLocalStore.getAccessToken(context).orEmpty()
        if (accessToken.isBlank()) {
            val localName = UserProfileLocalStore.getName(context)
            val localEmail = UserProfileLocalStore.getEmail(context)
            binding.profileNameTextView.text = if (localName.isBlank()) "사용자" else localName
            binding.profileEmailTextView.text = if (localEmail.isBlank()) "-" else localEmail
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val me = AuthRepositoryProvider.repository.me(accessToken).getOrNull()
            if (me != null) {
                val loginId = AuthTokenLocalStore.getLoginId(context).orEmpty()
                binding.profileNameTextView.text = me.name
                binding.profileEmailTextView.text = me.email
                UserProfileLocalStore.save(context, me.name, me.email, loginId)
            } else {
                val localName = UserProfileLocalStore.getName(context)
                val localEmail = UserProfileLocalStore.getEmail(context)
                binding.profileNameTextView.text = if (localName.isBlank()) "사용자" else localName
                binding.profileEmailTextView.text = if (localEmail.isBlank()) "-" else localEmail
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}