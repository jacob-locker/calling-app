package com.locker.callingapp.ui.navigation

import kotlinx.coroutines.flow.SharedFlow

interface Navigator {
    val startDestination: NavTarget
    val navCommandFlow: SharedFlow<NavCommand>

    fun navigateTo(navTarget: NavTarget)
    fun popBackstack()
}