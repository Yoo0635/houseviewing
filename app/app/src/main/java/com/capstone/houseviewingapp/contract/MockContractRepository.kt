package com.capstone.houseviewingapp.contract

import com.capstone.houseviewingapp.contract.model.ContractRegisterRequest
import com.capstone.houseviewingapp.contract.model.ContractRegisterResponse
import java.util.concurrent.atomic.AtomicLong

class MockContractRepository : ContractRepository {
    private val contractIdGen = AtomicLong(1L)
    private val contracts = mutableMapOf<Long, ContractRegisterResponse>()

    override fun registerContract(
        accessToken: String,
        request: ContractRegisterRequest
    ): Result<ContractRegisterResponse> {
        if (accessToken.isBlank()) return Result.failure(IllegalStateException("UNAUTHORIZED"))
        if (request.houseId <= 0L) return Result.failure(NoSuchElementException("HOUSE_NOT_FOUND"))

        val response = ContractRegisterResponse(
            houseId = request.houseId,
            contractId = contractIdGen.getAndIncrement()
        )
        contracts[response.contractId] = response
        return Result.success(response)
    }

    override fun deleteContract(accessToken: String, contractId: Long): Result<Unit> {
        if (accessToken.isBlank()) return Result.failure(IllegalStateException("UNAUTHORIZED"))
        val removed = contracts.remove(contractId) != null
        if (!removed) return Result.failure(NoSuchElementException("CONTRACT_NOT_FOUND"))
        return Result.success(Unit)
    }
}
