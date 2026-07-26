package com.capstone.houseviewingapp.analysis

object AnalysisRepositoryProvider {
    val repository: AnalysisRepository by lazy { RemoteAnalysisRepository() }
}

