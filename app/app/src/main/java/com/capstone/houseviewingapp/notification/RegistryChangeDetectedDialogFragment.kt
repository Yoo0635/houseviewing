package com.capstone.houseviewingapp.notification

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.capstone.houseviewingapp.MainActivity
import com.capstone.houseviewingapp.analysis.AnalysisFlow
import com.capstone.houseviewingapp.data.local.HouseLocalStore
import com.capstone.houseviewingapp.databinding.DialogRegistryChangeDetectedBinding
import com.capstone.houseviewingapp.registration.HouseRegistrationActivity
import android.content.Intent

class RegistryChangeDetectedDialogFragment: DialogFragment() {
    private var _binding: DialogRegistryChangeDetectedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRegistryChangeDetectedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        // 창 너비를 화면의 90%로 제한 (높이는 wrap_content 유지)
        val width = (resources.displayMetrics.widthPixels * 0.9f).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        binding.analysisButton.setOnClickListener {
            dismiss()
            val latestHouseId = HouseLocalStore.getHouses(requireContext())
                .lastOrNull()
                ?.houseId
                ?: -1L
            if (latestHouseId > 0L) {
                val intent = Intent(requireContext(), MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(MainActivity.EXTRA_SHOW_ANALYSIS_LOADING, true)
                    putExtra(MainActivity.EXTRA_ANALYSIS_SOURCE, AnalysisFlow.SOURCE_AUTO)
                    putExtra(AnalysisFlow.ARG_HOUSE_ID, latestHouseId)
                }
                startActivity(intent)
            } else {
                Toast.makeText(
                    requireContext(),
                    "등록된 집이 없어 빠른 진단으로 이동합니다.",
                    Toast.LENGTH_SHORT
                ).show()
                val intent = Intent(requireContext(), HouseRegistrationActivity::class.java).apply {
                    putExtra(HouseRegistrationActivity.EXTRA_QUICK_DIAGNOSIS_MODE, true)
                }
                startActivity(intent)
            }
        }
        binding.buttonLater.setOnClickListener {
            dismiss()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}