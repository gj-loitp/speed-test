package com.roy.speedtest.ext

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import egcodes.com.speedtest.R

object C {
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

    fun rateApp(
        a: Activity,
        packageName: String
    ) {
        try {
            a.startActivity(
                Intent(
                    Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")
                )
            )
        } catch (e: android.content.ActivityNotFoundException) {
            e.printStackTrace()
            a.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    fun moreApp(
        a: Activity,
        nameOfDeveloper: String = "Roy93Group"
    ) {
        val uri = "https://play.google.com/store/apps/developer?id=$nameOfDeveloper"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        a.startActivity(intent)
    }

    fun shareApp(
        a: Activity,
    ) {
        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_SUBJECT, a.getString(R.string.app_name))
            var sAux = "\nỨng dụng này rất bổ ích, thân mời bạn tải về cài đặt để trải nghiệm\n\n"
            sAux = sAux + "https://play.google.com/store/apps/details?id=" + a.packageName
            intent.putExtra(Intent.EXTRA_TEXT, sAux)
            a.startActivity(Intent.createChooser(intent, "Vui lòng chọn"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private const val URL_POLICY_NOTION =
        "https://loitp.notion.site/loitp/Privacy-Policy-319b1cd8783942fa8923d2a3c9bce60f/"

    fun openBrowserPolicy(
        c: Context
    ) {
        openUrlInBrowser(c = c, url = URL_POLICY_NOTION)
    }

    private fun openUrlInBrowser(
        c: Context,
        url: String?
    ) {
        if (url.isNullOrEmpty()) {
            return
        }
        try {
            val defaultBrowser =
                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)
            defaultBrowser.data = Uri.parse(url)
            c.startActivity(defaultBrowser)
        } catch (e: Exception) {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            c.startActivity(i)
        }
    }
}
