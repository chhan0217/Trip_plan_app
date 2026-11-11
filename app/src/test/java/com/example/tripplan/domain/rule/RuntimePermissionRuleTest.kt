package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.RuntimePermissionNextAction
import com.example.tripplan.domain.model.RuntimePermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimePermissionRuleTest {
    @Test
    fun unsupportedDevice_returnsUnsupportedBeforeOtherStates() {
        assertEquals(
            RuntimePermissionStatus.Unsupported,
            RuntimePermissionRule.status(
                isSupported = false,
                isGranted = true,
                wasRequested = true,
                shouldShowRationale = true,
            ),
        )
    }

    @Test
    fun grantedPermission_canContinue() {
        val status = RuntimePermissionRule.status(
            isSupported = true,
            isGranted = true,
            wasRequested = false,
            shouldShowRationale = false,
        )

        assertEquals(RuntimePermissionStatus.Granted, status)
        assertEquals(RuntimePermissionNextAction.Continue, RuntimePermissionRule.nextAction(status))
    }

    @Test
    fun firstUse_requestsPermission() {
        val status = RuntimePermissionRule.status(
            isSupported = true,
            isGranted = false,
            wasRequested = false,
            shouldShowRationale = false,
        )

        assertEquals(RuntimePermissionStatus.NotRequested, status)
        assertEquals(
            RuntimePermissionNextAction.RequestPermission,
            RuntimePermissionRule.nextAction(status),
        )
    }

    @Test
    fun deniedPermission_withRationale_showsExplanation() {
        val status = RuntimePermissionRule.status(
            isSupported = true,
            isGranted = false,
            wasRequested = true,
            shouldShowRationale = true,
        )

        assertEquals(RuntimePermissionStatus.RationaleRequired, status)
        assertEquals(
            RuntimePermissionNextAction.ShowRationale,
            RuntimePermissionRule.nextAction(status),
        )
    }

    @Test
    fun deniedPermission_withoutRationale_afterRequest_opensSettings() {
        val status = RuntimePermissionRule.status(
            isSupported = true,
            isGranted = false,
            wasRequested = true,
            shouldShowRationale = false,
        )

        assertEquals(RuntimePermissionStatus.PermanentlyDenied, status)
        assertEquals(
            RuntimePermissionNextAction.OpenAppSettings,
            RuntimePermissionRule.nextAction(status),
        )
    }
}
