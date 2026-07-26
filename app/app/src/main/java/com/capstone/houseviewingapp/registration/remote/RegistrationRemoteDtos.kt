package com.capstone.houseviewingapp.registration.remote

data class HouseRegisterRequest(
    val nickname: String,
    val originAddress: String
)

data class HouseRegisterResponse(
    val houseId: Long
)

data class ContractRegisterRequest(
    val houseId: Long,
    val contractType: String,
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
