package com.locker.callingapp.ui.navigation

import kotlinx.coroutines.flow.SharedFlow

object PreviewNavigator : Navigator {
    override val startDestination: NavTarget
        get() = TODO("Not yet implemented")
    override val navCommandFlow: SharedFlow<NavCommand>
        get() = TODO("Not yet implemented")

    override fun navigateTo(navTarget: NavTarget) {
        TODO("Not yet implemented")
    }

    override fun popBackstack() {
        TODO("Not yet implemented")
    }
}