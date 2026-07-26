package com.capstone.houseviewingapp.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.capstone.houseviewingapp.databinding.BottomSheetEditHouseBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EditHouseBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "edit_house_bottom_sheet"
        const val RESULT_KEY = "edit_house_result"
        const val ARG_HOUSE_ID = "arg_house_id"
        const val ARG_HOME_NAME = "arg_home_name"
        const val ARG_ADDRESS = "arg_address"
        const val RESULT_HOUSE_ID = "result_house_id"
        const val RESULT_HOME_NAME = "result_home_name"
        const val RESULT_ADDRESS = "result_address"

        fun newInstance(houseId: Long, homeName: String, address: String): EditHouseBottomSheetFragment {
            return EditHouseBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_HOUSE_ID, houseId)
                    putString(ARG_HOME_NAME, homeName)
                    putString(ARG_ADDRESS, address)
                }
            }
        }
    }

    private var _binding: BottomSheetEditHouseBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEditHouseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val houseId = arguments?.getLong(ARG_HOUSE_ID) ?: -1L
        val homeName = arguments?.getString(ARG_HOME_NAME).orEmpty()
        val address = arguments?.getString(ARG_ADDRESS).orEmpty()

        binding.nameEditText.setText(homeName)
        binding.detailAddressEditText.setText(address)

        binding.cancelButton.setOnClickListener { dismiss() }

        binding.saveButton.setOnClickListener {
            val newName = binding.nameEditText.text?.toString()?.trim().orEmpty()
            val newAddress = binding.detailAddressEditText.text?.toString()?.trim().orEmpty()

            if (houseId <= 0L || newName.isBlank()) return@setOnClickListener

            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                Bundle().apply {
                    putLong(RESULT_HOUSE_ID, houseId)
                    putString(RESULT_HOME_NAME, newName)
                    putString(RESULT_ADDRESS, newAddress)
                }
            )
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
