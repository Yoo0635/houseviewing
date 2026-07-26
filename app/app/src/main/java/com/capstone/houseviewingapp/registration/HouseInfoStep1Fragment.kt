package com.capstone.houseviewingapp.registration

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.houseviewingapp.BuildConfig
import com.capstone.houseviewingapp.R
import com.capstone.houseviewingapp.data.remote.NetworkModule
import com.capstone.houseviewingapp.data.remote.api.KakaoAddressDocument
import com.capstone.houseviewingapp.data.remote.executeApi
import com.capstone.houseviewingapp.databinding.FragmentHouseInfoStep1Binding
import kotlinx.coroutines.launch

class HouseInfoStep1Fragment : Fragment(R.layout.fragment_house_info_step1) {
    private var _binding: FragmentHouseInfoStep1Binding? = null
    private val binding get() = _binding!!
    private var isSearchingAddress = false
    private data class AddressCandidate(
        val primaryAddress: String,
        val secondaryAddress: String?,
        val zoneNo: String?
    )
    private class AddressCandidateAdapter(
        private val items: List<AddressCandidate>,
        private val onClick: (AddressCandidate) -> Unit
    ) : RecyclerView.Adapter<AddressCandidateAdapter.CandidateViewHolder>() {
        class CandidateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val titleText: TextView = view.findViewById(R.id.primaryAddressTextView)
            val subtitleText: TextView = view.findViewById(R.id.secondaryAddressTextView)
            val zoneText: TextView = view.findViewById(R.id.zoneNoTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_address_candidate, parent, false)
            return CandidateViewHolder(view)
        }

        override fun onBindViewHolder(holder: CandidateViewHolder, position: Int) {
            val item = items[position]
            holder.titleText.text = item.primaryAddress
            val hasSecondary = !item.secondaryAddress.isNullOrBlank()
            holder.subtitleText.visibility = if (hasSecondary) View.VISIBLE else View.GONE
            if (hasSecondary) holder.subtitleText.text = "지번 ${item.secondaryAddress}"
            val hasZone = !item.zoneNo.isNullOrBlank()
            holder.zoneText.visibility = if (hasZone) View.VISIBLE else View.GONE
            if (hasZone) holder.zoneText.text = "우편번호 ${item.zoneNo}"
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount(): Int = items.size
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHouseInfoStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    data class Step1Data(
        val nickname: String, // 집 닉네임
        val originAddress: String, // 도로명 주소
        val detailAddress: String // 상세 주소
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.houseNicknameEditText.addTextChangedListener { validateStep1Input() }
        binding.addressEditText.addTextChangedListener { validateStep1Input() }
        binding.addressDescEditText.addTextChangedListener { validateStep1Input() }
        binding.addressEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = binding.addressEditText.compoundDrawablesRelative[2]
                    ?: return@setOnTouchListener false
                val tappedOnEnd = event.x >=
                    binding.addressEditText.width - binding.addressEditText.paddingEnd - drawableEnd.bounds.width()
                if (tappedOnEnd) {
                    triggerAddressSearch()
                    return@setOnTouchListener true
                }
            }
            false
        }

        validateStep1Input()
    }

    // NOTE : 버튼 활성화 비활성화 처리
    private fun validateStep1Input() {
        val nickname = binding.houseNicknameEditText.text?.toString()?.trim().orEmpty()
        val originAddress = binding.addressEditText.text?.toString()?.trim().orEmpty()
        val detailAddress = binding.addressDescEditText.text?.toString()?.trim().orEmpty()

        val ButtonEnable =
            nickname.isNotBlank() && originAddress.isNotBlank() && detailAddress.isNotBlank()
        (activity as? HouseRegistrationActivity)?.setNextButtonEnabled(ButtonEnable)

        // TODO: 주소 검색 API 붙이면 "검색 결과 선택 완료" 조건 추가
        // 예) canGoNext = nickname.isNotBlank() && originAddress.isNotBlank() && isAddressConfirmed
    }

    private fun triggerAddressSearch() {
        if (isSearchingAddress) return
        val query = binding.addressEditText.text?.toString()?.trim().orEmpty()
        if (query.isBlank()) {
            Toast.makeText(requireContext(), "주소를 먼저 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (BuildConfig.KAKAO_REST_API_KEY.isBlank()) {
            Toast.makeText(requireContext(), "카카오 API 키가 설정되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        isSearchingAddress = true
        viewLifecycleOwner.lifecycleScope.launch {
            val result = NetworkModule.kakaoAddressApi
                .searchAddress(
                    authorization = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}",
                    query = query
                )
                .executeApi()
            isSearchingAddress = false

            val response = result.getOrElse {
                Toast.makeText(requireContext(), "주소 검색에 실패했습니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val candidates = response.documents
                .mapNotNull { it.toAddressCandidate() }
                .distinctBy { "${it.primaryAddress}|${it.secondaryAddress.orEmpty()}|${it.zoneNo.orEmpty()}" }
            if (candidates.isEmpty()) {
                Toast.makeText(requireContext(), "검색 결과가 없습니다. 주소를 다시 확인해 주세요.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            showAddressCandidateDialog(query, candidates)
        }
    }

    private fun showAddressCandidateDialog(query: String, candidates: List<AddressCandidate>) {
        val visibleCandidates = candidates.take(12)
        val dialogView = layoutInflater.inflate(R.layout.dialog_address_search_result, null)
        val subtitleTextView = dialogView.findViewById<TextView>(R.id.resultCountTextView)
        val queryTextView = dialogView.findViewById<TextView>(R.id.queryTextView)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.addressResultRecyclerView)
        val cancelButton = dialogView.findViewById<TextView>(R.id.cancelButtonTextView)

        subtitleTextView.text = "검색 결과 ${visibleCandidates.size}건"
        queryTextView.text = "\"$query\""

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = AddressCandidateAdapter(visibleCandidates) { candidate ->
            val selectedAddress = candidate.primaryAddress
            binding.addressEditText.setText(selectedAddress)
            binding.addressEditText.setSelection(selectedAddress.length)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.BOTTOM)
    }

    private fun KakaoAddressDocument.toAddressCandidate(): AddressCandidate? {
        val roadAddress = road_address?.address_name?.trim().orEmpty()
        val jibunAddress = address?.address_name?.trim().orEmpty()
        val fallbackAddress = address_name?.trim().orEmpty()
        val zoneNo = road_address?.zone_no?.trim().orEmpty().ifBlank { null }

        val primary = when {
            roadAddress.isNotBlank() -> roadAddress
            jibunAddress.isNotBlank() -> jibunAddress
            fallbackAddress.isNotBlank() -> fallbackAddress
            else -> return null
        }
        val secondary = when {
            primary == jibunAddress -> null
            jibunAddress.isNotBlank() -> jibunAddress
            fallbackAddress.isNotBlank() && fallbackAddress != primary -> fallbackAddress
            else -> null
        }
        return AddressCandidate(
            primaryAddress = primary,
            secondaryAddress = secondary,
            zoneNo = zoneNo
        )
    }

    // NOTE : UI에서 사용자가 입력한 데이터를 모아서 Step1Data 클래스 인스턴스로 반환하는 함수
    fun collectStep1Data(): Step1Data? {
        val nickname = binding.houseNicknameEditText.text?.toString()?.trim().orEmpty()
        val originAddress = binding.addressEditText.text?.toString()?.trim().orEmpty()
        val detailAddress = binding.addressDescEditText.text?.toString()?.trim().orEmpty()

        if (nickname.isBlank()) return null
        if (originAddress.isBlank()) return null
        if (detailAddress.isBlank()) return null

        return Step1Data(
            nickname = nickname,
            originAddress = originAddress,
            detailAddress = detailAddress
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}