package com.capstone.houseviewingapp.house

object HouseRepositoryProvider {
    val repository: HouseRepository by lazy { MockHouseRepository() }
}

