package com.capstone.houseviewingapp.home

data class HouseCardItem(
    val houseId : Long? = null, // NOTE : 집 고유 ID (서버 연결전에는 null )
    val homeName : String, // NOTE : 집 이름
    val address : String, // NOTE : 집 주소
    val ltv : Int? = null // TODO : 등기부 분석 API 연동 후 실제 LTV 값으로 설정
)