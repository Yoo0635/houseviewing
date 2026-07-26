package com.capstone.houseviewingapp.data.local

import android.content.Context
import android.content.SharedPreferences
import com.capstone.houseviewingapp.data.local.model.HouseDetailItem
import com.capstone.houseviewingapp.home.HouseCardItem
import org.json.JSONArray
import org.json.JSONObject

// TODO : 백엔드 연동 후 Repository(Remote + Room 등)로 교체 예정
object HouseLocalStore {

    // NOTE : SharedPreferences 파일 이름
    private const val PREF_NAME_PREFIX = "house_local_pref"

    // NOTE : 신형(상세 저장) 키
    private const val KEY_HOUSE_DETAILS_JSON = "house_details_json"

    // NOTE : 구형(카드 저장) 키 - 하위호환 용도
    private const val KEY_HOUSES_JSON = "houses_json"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(resolvePrefName(context), Context.MODE_PRIVATE)

    private fun resolvePrefName(context: Context): String {
        val loginId = AuthTokenLocalStore.getLoginId(context).orEmpty()
            .ifBlank { "guest" }
        val safeLoginId = loginId.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        return "${PREF_NAME_PREFIX}_$safeLoginId"
    }

    // NOTE : 홈 카드 목록 조회 함수 (외부 공개 API)
    // NOTE : 내부적으로 상세 목록을 읽어서 카드 요약으로 변환함
    fun getHouses(context: Context): List<HouseCardItem> = getHouseSummaries(context)

    // NOTE : 기존 코드 호환용 함수 (예전 HouseCardItem 직접 저장)
    // TODO : 신규 등록 플로우에서는 addHouseDetail 사용 권장
    fun addHouse(context: Context, item: HouseCardItem) {
        val details = getHouseDetails(context).toMutableList()
        val nextId = item.houseId ?: generateHouseId(details.map { it.houseId }.toSet())
        details.add(
            HouseDetailItem(
                houseId = nextId,
                homeName = item.homeName,
                originAddress = item.address,
                detailAddress = "",
                documentUri = null,
                contractType = "WOLSE",
                deposit = 0L,
                monthlyAmount = 0L,
                maintenanceFee = 0L,
                moveDate = "",
                confirmDate = "",
                ltv = item.ltv
            )
        )
        saveHouseDetails(context, details)
    }

    // NOTE : 신규 상세 데이터 저장 함수
    fun addHouseDetail(context: Context, detail: HouseDetailItem) {
        val list = getHouseDetails(context).toMutableList()
        list.add(detail)
        saveHouseDetails(context, list)
    }

    // NOTE : 홈 카드용 요약 목록 조회 함수
    fun getHouseSummaries(context: Context): List<HouseCardItem> {
        return getHouseDetails(context).map { detail ->
            HouseCardItem(
                houseId = detail.houseId,
                homeName = detail.homeName,
                address = detail.fullAddress(),
                ltv = detail.ltv
            )
        }
    }

    // NOTE : houseId 기준 상세 1건 조회 함수
    fun getHouseDetail(context: Context, houseId: Long): HouseDetailItem? {
        return getHouseDetails(context).firstOrNull { it.houseId == houseId }
    }

    // NOTE : 인덱스 기준 삭제 함수 (현재 홈 UI 호환)
    // NOTE : 내부 데이터는 상세 목록에서 제거됨
    fun removeHouse(context: Context, index: Int) {
        val list = getHouseDetails(context).toMutableList()
        if (index !in list.indices) return // NOTE : 잘못된 인덱스면 조용히 종료
        list.removeAt(index)
        saveHouseDetails(context, list)
    }

    // NOTE : 기존 코드 호환용 업데이트 함수
    // NOTE : 상세 정보는 유지하고 카드 필드만 갱신함
    fun updateHouse(context: Context, index: Int, updatedItem: HouseCardItem) {
        val list = getHouseDetails(context).toMutableList()
        if (index !in list.indices) return
        val old = list[index]
        list[index] = old.copy(
            homeName = updatedItem.homeName,
            originAddress = updatedItem.address,
            detailAddress = "",
            ltv = updatedItem.ltv
        )
        saveHouseDetails(context, list)
    }

    // NOTE : 내부 구현 함수들

    private fun getHouseDetails(context: Context): List<HouseDetailItem> {
        val p = prefs(context)

        // NOTE : 1) 신형 키 우선 사용
        val newJson = p.getString(KEY_HOUSE_DETAILS_JSON, null)
        if (!newJson.isNullOrBlank()) return parseDetailList(newJson)

        // NOTE : 2) 구형 키 fallback (기존 데이터 살리기)
        val oldJson = p.getString(KEY_HOUSES_JSON, null)
        if (!oldJson.isNullOrBlank()) {
            val legacyCards = parseLegacyCardList(oldJson)
            var nextIdSeed = generateHouseId(legacyCards.mapNotNull { it.houseId }.toSet())
            return legacyCards.map { card ->
                val resolvedId = card.houseId ?: nextIdSeed++
                HouseDetailItem(
                    houseId = resolvedId,
                    homeName = card.homeName,
                    originAddress = card.address,
                    detailAddress = "",
                    documentUri = null,
                    contractType = "WOLSE",
                    deposit = 0L,
                    monthlyAmount = 0L,
                    maintenanceFee = 0L,
                    moveDate = "",
                    confirmDate = "",
                    ltv = card.ltv
                )
            }
        }

        return emptyList()
    }

    private fun saveHouseDetails(context: Context, list: List<HouseDetailItem>) {
        val arr = JSONArray()
        for (d in list) {
            val obj = JSONObject()
            obj.put("houseId", d.houseId)
            obj.put("homeName", d.homeName)
            obj.put("originAddress", d.originAddress)
            obj.put("detailAddress", d.detailAddress)
            if (!d.documentUri.isNullOrBlank()) obj.put("documentUri", d.documentUri)
            obj.put("contractType", d.contractType)
            obj.put("deposit", d.deposit)
            obj.put("monthlyAmount", d.monthlyAmount)
            obj.put("maintenanceFee", d.maintenanceFee)
            obj.put("moveDate", d.moveDate)
            obj.put("confirmDate", d.confirmDate)
            if (d.ltv != null) obj.put("ltv", d.ltv)
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_HOUSE_DETAILS_JSON, arr.toString()).apply()
    }

    private fun parseDetailList(json: String): List<HouseDetailItem> {
        val list = mutableListOf<HouseDetailItem>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                HouseDetailItem(
                    houseId = o.getLong("houseId"),
                    homeName = o.getString("homeName"),
                    originAddress = o.optString("originAddress", ""),
                    detailAddress = o.optString("detailAddress", ""),
                    documentUri = o.optString("documentUri", "").ifBlank { null },
                    contractType = o.optString("contractType", "WOLSE"),
                    deposit = o.optLong("deposit", 0L),
                    monthlyAmount = o.optLong("monthlyAmount", 0L),
                    maintenanceFee = o.optLong("maintenanceFee", 0L),
                    moveDate = o.optString("moveDate", ""),
                    confirmDate = o.optString("confirmDate", ""),
                    ltv = if (o.has("ltv") && !o.isNull("ltv")) o.getInt("ltv") else null
                )
            )
        }
        return list
    }

    private fun parseLegacyCardList(json: String): List<HouseCardItem> {
        val list = mutableListOf<HouseCardItem>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val houseId =
                if (obj.has("houseId") && !obj.isNull("houseId")) obj.getLong("houseId") else null
            val homeName = obj.optString("homeName", "")
            val address = obj.optString("address", "")
            val ltv = if (obj.has("ltv") && !obj.isNull("ltv")) obj.getInt("ltv") else null
            list.add(
                HouseCardItem(
                    houseId = houseId,
                    homeName = homeName,
                    address = address,
                    ltv = ltv
                )
            )
        }
        return list
    }

    private fun generateHouseId(existing: Set<Long>): Long {
        return (existing.maxOrNull() ?: 0L) + 1L
    }

    fun updateHouseDetailById(context: Context, houseId: Long, updated: HouseDetailItem): Boolean {
        val list = getHouseDetails(context).toMutableList()
        val index = list.indexOfFirst { it.houseId == houseId }
        if (index == -1) return false
        list[index] = updated.copy(houseId = houseId)
        saveHouseDetails(context, list)
        return true
    }

    fun removeHouseById(context: Context, houseId: Long): Boolean {
        val list = getHouseDetails(context).toMutableList()
        val removed = list.removeAll { it.houseId == houseId }
        if (removed) saveHouseDetails(context, list)
        return removed
    }
}