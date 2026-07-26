package com.capstone.houseviewingapp.house

import com.capstone.houseviewingapp.house.model.HouseEditRequest
import com.capstone.houseviewingapp.house.model.HouseEditResponse
import com.capstone.houseviewingapp.house.model.HouseMeResponse
import com.capstone.houseviewingapp.house.model.HouseRegisterRequest
import com.capstone.houseviewingapp.house.model.HouseRegisterResponse
import com.capstone.houseviewingapp.house.model.HousesResponse
import com.capstone.houseviewingapp.house.model.MonitoringStatus
import java.util.concurrent.atomic.AtomicLong

class MockHouseRepository : HouseRepository {
    private val idGenerator = AtomicLong(1L)
    private data class HouseRow(
        val houseId: Long,
        var nickname: String,
        var address: String
    )

    private val houses = mutableListOf<HouseRow>()

    override fun registerHouse(accessToken: String, request: HouseRegisterRequest): Result<HouseRegisterResponse> {
        if (accessToken.isBlank()) return Result.failure(IllegalStateException("UNAUTHORIZED"))
        if (request.nickname.isBlank() || request.originAddress.isBlank()) {
            return Result.failure(IllegalArgumentException("ADDRESS_REQUIRED"))
        }
        val row = HouseRow(
            houseId = idGenerator.getAndIncrement(),
            nickname = request.nickname.trim(),
            address = request.originAddress.trim()
        )
        houses.add(row)
        return Result.success(HouseRegisterResponse(houseId = row.houseId))
    }

    override fun getHouses(accessToken: String): Result<List<HousesResponse>> {
        if (accessToken.isBlank()) return Result.failure(IllegalStateException("UNAUTHORIZED"))
        return Result.success(
            houses.map {
                HousesResponse(
                    houseId = it.houseId,
                    nickname = it.nickname,
                    address = it.address,
                    ltvScore = null,
                    monitoringStatus = MonitoringStatus.LIVE
                )
            }
        )
    }

    override fun getHouse(accessToken: String, houseId: Long): Result<HouseMeResponse> {
        if (accessToken.isBlank()) return Result.failure(IllegalStateException("UNAUTHORIZED"))
        val row = houses.firstOrNull { it.houseId == houseId }
            ?: return Result.failure(NoSuchElementException("HOUSE_NOT_FOUND"))
        return Result.success(
            HouseMeResponse(
                nickname = row.nickname,
                address = row.address,
                deposit = null,
                monthlyAmount = null,
                maintenanceFee = null,
                moveDate = null,
                confirmDate = null,
                ltvScore = null
            )
        )
    }

    override fun updateHouse(
        accessToken: String,
        houseId: Long,
        request: HouseEditRequest
    ): Result<HouseEditResponse> {
        if (accessToken.isBlank()) return Result.failure(IllegalStateException("UNAUTHORIZED"))
        val index = houses.indexOfFirst { it.houseId == houseId }
        if (index == -1) return Result.failure(NoSuchElementException("HOUSE_NOT_FOUND"))

        val cur = houses[index]
        val newNickname = request.nickname?.trim()?.takeIf { it.isNotBlank() } ?: cur.nickname
        val newAddress = request.address?.trim()?.takeIf { it.isNotBlank() } ?: cur.address
        houses[index] = cur.copy(nickname = newNickname, address = newAddress)
        return Result.success(HouseEditResponse(address = newAddress, nickname = newNickname))
    }

    override fun deleteHouse(accessToken: String, houseId: Long): Result<Unit> {
        if (accessToken.isBlank()) return Result.failure(IllegalStateException("UNAUTHORIZED"))
        val removed = houses.removeAll { it.houseId == houseId }
        if (!removed) return Result.failure(NoSuchElementException("HOUSE_NOT_FOUND"))
        return Result.success(Unit)
    }
}
