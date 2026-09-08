package com.capstone.houseviewingapp.registration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.UUID

class HouseRegistrationViewModelTest {

    @Test
    fun quickDiagnosisRequestId_isStableUntilDraftIsCleared() {
        val viewModel = HouseRegistrationViewModel()

        val first = viewModel.currentQuickDiagnosisRequestId()
        val second = viewModel.currentQuickDiagnosisRequestId()

        UUID.fromString(first)
        assertEquals(first, second)

        viewModel.clearDraft()
        assertNotEquals(first, viewModel.currentQuickDiagnosisRequestId())
    }
}
