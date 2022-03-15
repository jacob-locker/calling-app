package com.locker.callingapp.ui.navigation

import android.Manifest

sealed class NavCommand {
    data class Navigate(val target: NavTarget) : NavCommand()
    object PopBackstack : NavCommand()
}

enum class NavTarget(
    val label: String,
    val requiredPermissions: List<Permission> = emptyList(),
    val permissionRequestCode: Int = -1
) {
    Invites("invites"),
    CallRoom(
        "call_room",
        requiredPermissions = listOf(Permission.RECORD_AUDIO),
        permissionRequestCode = 1
    )
}