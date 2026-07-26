package com.capstone.houseviewingapp.registration

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// viewmodel은 등록 과정에서 입력된 데이터를 임시로 보관하는 역할을 합니다.
class HouseRegistrationViewModel : ViewModel() {
    private val _draft = MutableStateFlow(HouseRegistrationDraft())
    val draft: StateFlow<HouseRegistrationDraft> = _draft

    // NOTE : Step 1에서 입력받은 닉네임, 주소 정보를 업데이트하는 함수
    fun updateStep1(
        nickname: String,
        originAddress: String,
        detailAddress: String
    ) {
        _draft.value = _draft.value.copy(
            nickname = nickname,
            originAddress = originAddress,
            detailAddress = detailAddress
        )
    }

    // NOTE : Step 2에서 입력받은 계약 유형, 보증금, 월세 금액, 관리비 정보를 업데이트하는 함수
    fun updateStep2(
        contractType: ContractType,
        deposit: Long,
        monthlyAmount: Long,
        maintenanceFee: Long,
        moveDate: String,
        confirmDate: String
    ) {
        _draft.value = _draft.value.copy(
            contractType = contractType,
            deposit = deposit,
            monthlyAmount = monthlyAmount,
            maintenanceFee = maintenanceFee,
            moveDate = moveDate,
            confirmDate = confirmDate
        )
    }

    // NOTE : Step 3에서 입력받은 계약서 이미지 URI 정보를 업데이트하는 함수
    fun updateStep3(documentUri: String?) {
        // TODO : 나중에 백엔드랑 연동할 때, documentUri를 서버에 업로드하고, 서버에서 반환된 URL을 저장하도록 수정 필요
        _draft.value = _draft.value.copy(
            documentUri = documentUri?.toString()
        )
    }

    // NOTE : 등록 완료, 취소하면 초기 상태로 되돌리는 함수
    fun clearDraft() {
        _draft.value = HouseRegistrationDraft()
    }

    // TODO : userId 전달 방식(JWT 추출 vs body) 서버와 확정
    // TODO : contractType 값(JEONSE/WOLSE) 서버 enum과 최종 일치 확인
}