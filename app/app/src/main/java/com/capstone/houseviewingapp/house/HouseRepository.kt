package com.capstone.houseviewingapp.house

import com.capstone.houseviewingapp.house.model.HouseEditRequest
import com.capstone.houseviewingapp.house.model.HouseEditResponse
import com.capstone.houseviewingapp.house.model.HouseMeResponse
import com.capstone.houseviewingapp.house.model.HouseRegisterRequest
import com.capstone.houseviewingapp.house.model.HouseRegisterResponse
import com.capstone.houseviewingapp.house.model.HousesResponse

interface HouseRepository {
    fun registerHouse(accessToken: String, request: HouseRegisterRequest): Result<HouseRegisterResponse>
    fun getHouses(accessToken: String): Result<List<HousesResponse>>
    fun getHouse(accessToken: String, houseId: Long): Result<HouseMeResponse>
    fun updateHouse(accessToken: String, houseId: Long, request: HouseEditRequest): Result<HouseEditResponse>
    fun deleteHouse(accessToken: String, houseId: Long): Result<Unit>
}
