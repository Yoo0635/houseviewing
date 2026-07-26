package com.capstone.houseviewingapp.data.local.model

data class HouseDetailItem(
    val houseId: Long,
    val homeName: String,
    val originAddress: String,
    val detailAddress: String,
    val documentUri: String? = null,
    val contractType: String, // "JEONSE" | "WOLSE"
    val deposit: Long,
    val monthlyAmount: Long,
    val maintenanceFee: Long,
    val moveDate: String,
    val confirmDate: String,
    val ltv: Int? = null
) {
    fun fullAddress(): String = listOf(originAddress, detailAddress)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}