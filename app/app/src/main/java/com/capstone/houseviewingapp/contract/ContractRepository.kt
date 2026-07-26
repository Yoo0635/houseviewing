package com.capstone.houseviewingapp.contract

import com.capstone.houseviewingapp.contract.model.ContractRegisterRequest
import com.capstone.houseviewingapp.contract.model.ContractRegisterResponse

interface ContractRepository {
    fun registerContract(
        accessToken: String,
        request: ContractRegisterRequest
    ): Result<ContractRegisterResponse>

    fun deleteContract(accessToken: String, contractId: Long): Result<Unit>
}
