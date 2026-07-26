package com.capstone.houseviewingapp.my

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.capstone.houseviewingapp.databinding.DialogLogoutConfirmBinding

class LogoutConfirmDialogFragment : DialogFragment() {
    private var _binding: DialogLogoutConfirmBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLogoutConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val width = (resources.displayMetrics.widthPixels * 0.9f).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        binding.cancelButton.setOnClickListener { dismiss() }
        binding.logoutButton.setOnClickListener {
            dismiss()
            (activity as? ProfileEditActivity)?.performLogout()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

