package com.capstone.houseviewingapp.registration

// TODO : 백엔드 연동 후, 서버에서 받아오는 데이터 형식에 맞게 수정 필요

data class HouseRegistrationDraft(
    val nickname: String = "", // 집 닉네임
    val originAddress: String = "", // 도로명 주소
    val detailAddress: String = "", // 상세 주소
    val contractType: ContractType = ContractType.WOLSE, // 전세면 JEONSE, 월세면 WOLSE
    val deposit: Long = 0L, // 보증금
    val monthlyAmount: Long = 0L,      // 전세면 0
    val maintenanceFee: Long = 0L,  // 관리비
    val moveDate: String = "",         // yyyy-MM-dd
    val confirmDate: String = "",      // yyyy-MM-dd
    val documentUri: String? = null    // 아직 업로드 전이면 null
)

// NOTE : 계약 유형을 나타내는 enum 클래스
enum class ContractType {
    JEONSE,
    WOLSE
}