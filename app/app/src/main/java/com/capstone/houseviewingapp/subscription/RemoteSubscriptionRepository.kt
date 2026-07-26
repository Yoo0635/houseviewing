package com.capstone.houseviewingapp.subscription

import com.capstone.houseviewingapp.data.remote.NetworkModule
import com.capstone.houseviewingapp.data.remote.executeApiVoid

class RemoteSubscriptionRepository : SubscriptionRepository {
    override suspend fun subscribePremium(accessToken: String): Result<Unit> {
        if (accessToken.isBlank()) return Result.failure(IllegalStateException("UNAUTHORIZED"))
        return NetworkModule.subscriptionApi
            .subscribePremium("Bearer $accessToken")
            .executeApiVoid()
    }
}
