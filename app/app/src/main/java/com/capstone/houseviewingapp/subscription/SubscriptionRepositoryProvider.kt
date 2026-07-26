package com.capstone.houseviewingapp.subscription

object SubscriptionRepositoryProvider {
    val repository: SubscriptionRepository by lazy { RemoteSubscriptionRepository() }
}

