package com.capstone.houseviewingapp.registration

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.capstone.houseviewingapp.databinding.DialogRegistrationParsingBinding

class RegistrationParsingDialogFragment : DialogFragment() {
    private var _binding: DialogRegistrationParsingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRegistrationParsingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val width = (resources.displayMetrics.widthPixels * 0.9f).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.progressIndicator.isIndeterminate = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "RegistrationParsingDialog"
    }
}

