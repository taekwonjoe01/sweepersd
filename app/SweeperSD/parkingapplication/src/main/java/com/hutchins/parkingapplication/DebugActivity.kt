package com.hutchins.parkingapplication

import com.hutchins.navui.core.NavViewActivity
import com.hutchins.navui.core.NavViewDelegate
import com.hutchins.navui.jetpack.JetpackSideNavDelegate

class DebugActivity : NavViewActivity() {
    override val navViewDelegate: NavViewDelegate = JetpackSideNavDelegate(this, R.menu.menu_debug, R.navigation.navigation_debug)
}