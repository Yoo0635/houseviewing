package com.capstone.houseviewingapp.house.model

/** HouseRegisterRequest */
data class HouseRegisterRequest(
    val nickname: String,
    val originAddress: String
)

data class HouseRegisterResponse(
    val houseId: Long
)

enum class MonitoringStatus {
    LIVE,
    OFFLINE
}

/** HousesResponse */
data class HousesResponse(
    val houseId: Long,
    val nickname: String,
    val address: String,
    val ltvScore: Int?,
    val monitoringStatus: MonitoringStatus?
)

/** HouseMeResponse — 날짜는 ISO 문자열(yyyy-MM-dd) */
data class HouseMeResponse(
    val nickname: String,
    val address: String,
    val deposit: Long?,
    val monthlyAmount: Long?,
    val maintenanceFee: Long?,
    val moveDate: String?,
    val confirmDate: String?,
    val ltvScore: Int?
)

/** HouseEditRequest */
data class HouseEditRequest(
    val address: String?,
    val nickname: String?
)

data class HouseEditResponse(
    val address: String,
    val nickname: String
)
