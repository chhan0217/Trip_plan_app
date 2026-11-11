package com.example.tripplan.domain.repository

import com.example.tripplan.domain.model.RuntimePermission

interface DeviceCapabilityRepository {
    fun isNetworkAvailable(): Boolean
    fun isPermissionSupported(permission: RuntimePermission): Boolean
    fun isPermissionGranted(permission: RuntimePermission): Boolean
}
