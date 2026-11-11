package com.example.tripplan.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.example.tripplan.domain.model.RuntimePermission
import com.example.tripplan.domain.repository.DeviceCapabilityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidDeviceCapabilityRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceCapabilityRepository {
    private val packageManager: PackageManager
        get() = context.packageManager

    override fun isNetworkAvailable(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override fun isPermissionSupported(permission: RuntimePermission): Boolean = when (permission) {
        RuntimePermission.ApproximateLocation,
        RuntimePermission.PreciseLocation,
        -> packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION)
        RuntimePermission.Camera ->
            packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    override fun isPermissionGranted(permission: RuntimePermission): Boolean {
        return ContextCompat.checkSelfPermission(context, permission.androidPermission) ==
            PackageManager.PERMISSION_GRANTED
    }
}

private val RuntimePermission.androidPermission: String
    get() = when (this) {
        RuntimePermission.ApproximateLocation -> Manifest.permission.ACCESS_COARSE_LOCATION
        RuntimePermission.PreciseLocation -> Manifest.permission.ACCESS_FINE_LOCATION
        RuntimePermission.Camera -> Manifest.permission.CAMERA
    }
