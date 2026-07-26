package com.capstone.houseviewingapp.login

import android.content.ClipData.*
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.*
import com.capstone.houseviewingapp.databinding.DialogFindIdSuccessBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.core.graphics.drawable.toDrawable

class FindIDResultFragment(private val id: String ) : BottomSheetDialogFragment() { // NOTE : id가 찾은 아이디 (저런건 val -> 수정될 일 없으니깐)
    private var _binding : DialogFindIdSuccessBinding? = null
    private val binding get() =  _binding!!

    override fun onCreateView( // NOTE : 화면 생성
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogFindIdSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { // NOTE : 화면 꾸미기, 기능 넣기
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        //NOTE : maskedId -> 별표 만들기 로직
        val maskedId = if (id.length > 4) {
            //NOTE : 아이디 0~ 3글자까지는 그냥 가져오고 그 뒤부턴 *로 반복해서 붙임 (남은 글자로)
            id.substring(0,4) + "*".repeat(id.length - 4)
        } else {
            id
        }

        binding.foundIDTextView.text = "아이디 : $maskedId"

        binding.copyButton.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = newPlainText("Found id: ", id)
            clipboard.setPrimaryClip(clip)

            makeText(requireContext(), "아이디가 복사되었습니다", LENGTH_SHORT).show()
        }

        binding.loginButton.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP // NOTE : 뒤로가기 꼬임 방지
            startActivity(intent)
            dismiss() // NOTE : 팝업창 종료
        }
        binding.findPWButton.setOnClickListener {
            val intent = Intent(requireContext(), FindPasswordActivity::class.java)
            startActivity(intent)
            dismiss()
            requireActivity().finish() // NOTE: 아이디 찾기 화면 종료
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}