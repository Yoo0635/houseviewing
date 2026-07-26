package com.capstone.houseviewingapp.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.capstone.houseviewingapp.databinding.DialogPaidQuickDiagnosisBinding

class PaidQuickDiagnosisDialogFragment : DialogFragment() {
    private var _binding: DialogPaidQuickDiagnosisBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPaidQuickDiagnosisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val width = (resources.displayMetrics.widthPixels * 0.9f).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        binding.continueButton.setOnClickListener {
            parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle().apply {
                putBoolean(KEY_CONTINUE, true)
            })
            dismiss()
        }
        binding.cancelButton.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "paid_quick_diagnosis_result"
        const val KEY_CONTINUE = "continue"
    }
}

