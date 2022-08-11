package com.grandfatherpikhto.blescan.helper

import android.app.Activity
import android.content.res.Resources
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment

typealias OnClickItemListener<T> = (T, View) -> Unit
typealias OnLongClickItemListener<T> = (T, View) -> Unit


fun linkMenu(menuHost: MenuHost, link: Boolean, menuProvider: MenuProvider) {
    if (link) {
        menuHost.addMenuProvider(menuProvider)
    } else {
        menuHost.removeMenuProvider(menuProvider)
    }
}

fun Activity.linkMenu(link: Boolean, menuProvider: MenuProvider)
    = linkMenu(this as MenuHost, link, menuProvider)

fun Fragment.linkMenu(link: Boolean, menuProvider: MenuProvider)
    = linkMenu(requireActivity() as MenuHost, link, menuProvider)

fun pxToDp(px: Int): Int {
    return (px / Resources.getSystem().displayMetrics.density).toInt()
}

fun dpToPx(dp: Int): Int {
    return (dp * Resources.getSystem().displayMetrics.density).toInt()
}

fun AppCompatActivity.linkMenuProvider(menuProvider: MenuProvider) {
    (this as MenuHost).addMenuProvider(menuProvider)
}

fun AppCompatActivity.unlinkMenuProvider(menuProvider: MenuProvider) {
    (this as MenuHost).removeMenuProvider(menuProvider)
}

fun Fragment.linkMenuProvider(menuProvider: MenuProvider) {
    (requireActivity() as MenuHost).addMenuProvider(menuProvider)
}

fun Fragment.unlinkMenuProvider(menuProvider: MenuProvider) {
    (requireActivity() as MenuHost).removeMenuProvider(menuProvider)
}