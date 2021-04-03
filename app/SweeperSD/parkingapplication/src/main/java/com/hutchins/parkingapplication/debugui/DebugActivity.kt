package com.hutchins.parkingapplication.debugui

import com.hutchins.navui.core.NavViewActivity
import com.hutchins.navui.core.NavViewDelegate
import com.hutchins.navui.jetpack.JetpackSideNavDelegate
import com.hutchins.parkingapplication.R

class DebugActivity : NavViewActivity() {
    override val navViewDelegate: NavViewDelegate = JetpackSideNavDelegate(this, R.menu.menu_debug, R.navigation.navigation_debug)
}