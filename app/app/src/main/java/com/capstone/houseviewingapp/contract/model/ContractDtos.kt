package com.capstone.houseviewingapp.contract.model

enum class ContractType {
    JEONSE,
    MONTHLY
}

/** ContractRegisterRequest — 날짜는 "yyyy-MM-dd" 문자열 */
data class ContractRegisterRequest(
    val houseId: Long,
    val contractType: ContractType,
    val deposit: Long,
    val monthlyAmount: Long,
    val maintenanceFee: Long,
    val moveDate: String,
    val confirmDate: String
)

data class ContractRegisterResponse(
    val houseId: Long,
    val contractId: Long
)
