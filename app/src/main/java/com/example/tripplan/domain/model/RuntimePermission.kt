package com.example.tripplan.domain.model

enum class RuntimePermission {
    ApproximateLocation,
    PreciseLocation,
    Camera,
}

enum class RuntimePermissionStatus {
    Granted,
    NotRequested,
    RationaleRequired,
    PermanentlyDenied,
    Unsupported,
}

enum class RuntimePermissionNextAction {
    Continue,
    RequestPermission,
    ShowRationale,
    OpenAppSettings,
    ShowUnsupported,
}
