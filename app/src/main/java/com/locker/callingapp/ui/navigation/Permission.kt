package com.locker.callingapp.ui.navigation

import android.Manifest

enum class Permission(val androidPermission: String, val friendlyName: String) {
    RECORD_AUDIO(
        androidPermission = Manifest.permission.RECORD_AUDIO,
        friendlyName = "Record Audio"
    )
}
