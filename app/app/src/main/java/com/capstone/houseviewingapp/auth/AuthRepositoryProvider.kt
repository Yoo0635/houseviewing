package com.capstone.houseviewingapp.auth

object AuthRepositoryProvider {
    val repository: AuthRepository by lazy { RemoteAuthRepository() }
}
