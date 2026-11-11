package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.RuntimePermission
import com.example.tripplan.domain.model.RuntimePermissionNextAction
import com.example.tripplan.domain.model.RuntimePermissionStatus
import com.example.tripplan.domain.repository.DeviceCapabilityRepository
import com.example.tripplan.domain.rule.RuntimePermissionRule
import javax.inject.Inject

class GetRuntimePermissionStatusUseCase @Inject constructor(
    private val repository: DeviceCapabilityRepository,
) {
    operator fun invoke(
        permission: RuntimePermission,
        wasRequested: Boolean,
        shouldShowRationale: Boolean,
    ): RuntimePermissionStatus = RuntimePermissionRule.status(
        isSupported = repository.isPermissionSupported(permission),
        isGranted = repository.isPermissionGranted(permission),
        wasRequested = wasRequested,
        shouldShowRationale = shouldShowRationale,
    )
}

class GetRuntimePermissionNextActionUseCase @Inject constructor() {
    operator fun invoke(status: RuntimePermissionStatus): RuntimePermissionNextAction {
        return RuntimePermissionRule.nextAction(status)
    }
}
