package com.capstone.houseviewingapp.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.capstone.houseviewingapp.R
import com.capstone.houseviewingapp.data.local.HouseLocalStore
import com.capstone.houseviewingapp.data.local.model.HouseDetailItem
import com.capstone.houseviewingapp.databinding.FragmentHouseDetailBinding
import java.text.NumberFormat
import java.util.Locale

class HouseDetailFragment : Fragment(R.layout.fragment_house_detail) {

    private var _binding: FragmentHouseDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHouseDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener {
            findNavController().popBackStack(R.id.nav_home, false)
        }
        binding.moreButton.setOnClickListener { }

        val houseId = arguments?.getLong("houseId") ?: -1L
        if (houseId <= 0L) {
            findNavController().popBackStack(R.id.nav_home, false)
            return
        }

        val detail = HouseLocalStore.getHouseDetail(requireContext(), houseId)
        if (detail == null) {
            findNavController().popBackStack(R.id.nav_home, false)
            return
        }

        bindDetail(detail)
    }

    private fun bindDetail(detail: HouseDetailItem) {
        binding.homeNameTextView.text = detail.homeName
        binding.addressTextView.text = detail.fullAddress()

        binding.depositTextView.text = formatMoneyCompact(detail.deposit)
        binding.moveDateTextView.text = detail.moveDate.ifBlank { "-" }
        binding.confirmDateTextView.text = detail.confirmDate.ifBlank { "-" }

        val isJeonse = detail.contractType.equals("JEONSE", ignoreCase = true)
        if (isJeonse || detail.monthlyAmount <= 0L) {
            binding.monthlyRow.visibility = View.GONE
            binding.dividerAfterMonthly.visibility = View.GONE
        } else {
            binding.monthlyRow.visibility = View.VISIBLE
            binding.dividerAfterMonthly.visibility = View.VISIBLE
            binding.monthlyTextView.text = formatMoneyCompact(detail.monthlyAmount)
        }

        if (detail.maintenanceFee <= 0L) {
            binding.maintenanceRow.visibility = View.GONE
            binding.dividerAfterMaintenance.visibility = View.GONE
        } else {
            binding.maintenanceRow.visibility = View.VISIBLE
            binding.dividerAfterMaintenance.visibility = View.VISIBLE
            binding.maintenanceTextView.text = formatMoneyCompact(detail.maintenanceFee)
        }

        bindLtv(detail.ltv)
    }

    private fun bindLtv(ltv: Int?) {
        binding.ltvProgress.max = 100
        val trackColor = ContextCompat.getColor(requireContext(), R.color.textviewgray)
        binding.ltvProgress.trackColor = trackColor
        val safeValue = ltv?.coerceIn(0, 100)
        if (safeValue == null) {
            binding.ltvValueTextView.text = "--"
            binding.ltvProgress.progress = 0
            val neutral = ContextCompat.getColor(requireContext(), R.color.textgray)
            binding.ltvProgress.setIndicatorColor(neutral)
            binding.ltvValueTextView.setTextColor(neutral)
            return
        }
        binding.ltvValueTextView.text = "$safeValue%"
        binding.ltvProgress.progress = safeValue

        val colorRes = when {
            safeValue <= 60 -> R.color.blue
            safeValue <= 70 -> R.color.amber
            else -> R.color.red
        }
        val color = ContextCompat.getColor(requireContext(), colorRes)
        binding.ltvProgress.setIndicatorColor(color)
        binding.ltvValueTextView.setTextColor(color)
    }

    private fun formatMoneyCompact(value: Long): String {
        if (value <= 0L) return "0원"
        val eok = value / 100_000_000L
        val man = (value % 100_000_000L) / 10_000L
        return when {
            eok > 0 && man > 0 -> "${eok}억 ${man}만 원"
            eok > 0 -> "${eok}억 원"
            value >= 10_000L -> "${value / 10_000L}만 원"
            else -> "${NumberFormat.getNumberInstance(Locale.KOREA).format(value)}원"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}