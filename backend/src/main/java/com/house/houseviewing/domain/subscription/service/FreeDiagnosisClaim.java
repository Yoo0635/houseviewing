package com.house.houseviewing.domain.subscription.service;

import com.house.houseviewing.domain.subscription.enums.FreeDiagnosisStage;
import com.house.houseviewing.domain.subscription.enums.FreeDiagnosisStatus;

public record FreeDiagnosisClaim(
        boolean executable,
        boolean premium,
        FreeDiagnosisStatus status,
        FreeDiagnosisStage stage
) {
}
