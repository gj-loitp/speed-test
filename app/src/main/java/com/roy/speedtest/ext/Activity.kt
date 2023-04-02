package com.roy.speedtest.ext

import android.app.Activity
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu

object Activity {
    fun showPopup(
        a: Activity,
        showOnView: View,
        menuRes: Int,
        callback: ((MenuItem) -> Unit)? = null,
    ) {
        val popup = PopupMenu(a, showOnView)
        popup.menuInflater.inflate(menuRes, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            callback?.invoke(menuItem)
            true
        }
        popup.show()
    }
}
