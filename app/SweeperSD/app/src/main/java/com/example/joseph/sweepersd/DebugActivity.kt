package com.example.joseph.sweepersd

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.hutchins.navcore.NavigationActivity
import com.hutchins.navui.core.NavViewActivity
import com.hutchins.navui.core.NavViewDelegate
import com.hutchins.navui.jetpack.JetpackSideNavDelegate

class DebugActivity : NavViewActivity() {

    override val navViewDelegate: NavViewDelegate = JetpackSideNavDelegate(this, R.menu.menu_debug)
    override val navigationGraphResourceId: Int = R.navigation.navigation_debug


}