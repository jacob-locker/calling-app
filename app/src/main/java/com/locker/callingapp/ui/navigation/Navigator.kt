package com.locker.callingapp.ui.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

object Navigator {
    var StartDestination = NavTarget.Invites
    private val _stateFlow =
      MutableStateFlow<NavTarget>(StartDestination)
    val sharedFlow = _stateFlow.asSharedFlow()



    fun navigateTo(navTarget: NavTarget) {
        _stateFlow.tryEmit(navTarget)
    }

    enum class NavTarget(val label: String) {
        Invites("invites"),
        CallRoom("call_room")
    }
}