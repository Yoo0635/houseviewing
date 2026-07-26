package com.capstone.houseviewingapp.contract

object ContractRepositoryProvider {
    val repository: ContractRepository by lazy { MockContractRepository() }
}

