package com.capstone.houseviewingapp.notification

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.capstone.houseviewingapp.databinding.DialogNotificationAccessGuideBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NotificationAccessGuideBottomSheetFragment : BottomSheetDialogFragment() {
    private var _binding: DialogNotificationAccessGuideBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogNotificationAccessGuideBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        binding.goButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            dismiss()
        }
        binding.buttonLater.setOnClickListener {
            // TODO: 나중에 알림 권한 설정하기
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}