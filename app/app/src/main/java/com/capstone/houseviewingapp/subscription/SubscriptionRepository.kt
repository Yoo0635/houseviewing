package com.capstone.houseviewingapp.subscription

interface SubscriptionRepository {
    /** POST /subscriptions/premium — 바디 없음 */
    suspend fun subscribePremium(accessToken: String): Result<Unit>
}
