package com.capstone.houseviewingapp.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.capstone.houseviewingapp.databinding.DialogFindPwSuccessBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FindPWResultFragment : BottomSheetDialogFragment() {
    private var _binding: DialogFindPwSuccessBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFindPwSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.resettingPWButton.setOnClickListener {
            var intent = Intent(requireContext(), ResetPasswordActivity::class.java)
            startActivity(intent)
            dismiss()
            requireActivity().finish() // NOTE: 아이디 찾기 화면 종료
        }

        binding.closeButton.setOnClickListener {
            dismiss() // NOTE : 바텀시트를 닫는 명령어
        }

    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}