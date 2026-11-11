package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.RuntimePermissionNextAction
import com.example.tripplan.domain.model.RuntimePermissionStatus

object RuntimePermissionRule {
    fun status(
        isSupported: Boolean,
        isGranted: Boolean,
        wasRequested: Boolean,
        shouldShowRationale: Boolean,
    ): RuntimePermissionStatus = when {
        !isSupported -> RuntimePermissionStatus.Unsupported
        isGranted -> RuntimePermissionStatus.Granted
        shouldShowRationale -> RuntimePermissionStatus.RationaleRequired
        wasRequested -> RuntimePermissionStatus.PermanentlyDenied
        else -> RuntimePermissionStatus.NotRequested
    }

    fun nextAction(status: RuntimePermissionStatus): RuntimePermissionNextAction = when (status) {
        RuntimePermissionStatus.Granted -> RuntimePermissionNextAction.Continue
        RuntimePermissionStatus.NotRequested -> RuntimePermissionNextAction.RequestPermission
        RuntimePermissionStatus.RationaleRequired -> RuntimePermissionNextAction.ShowRationale
        RuntimePermissionStatus.PermanentlyDenied -> RuntimePermissionNextAction.OpenAppSettings
        RuntimePermissionStatus.Unsupported -> RuntimePermissionNextAction.ShowUnsupported
    }
}
