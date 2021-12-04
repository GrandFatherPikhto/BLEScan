package com.grandfatherpikhto.blescan.model

import android.view.View

interface RvItemClick<T> {
    fun onItemClick(model: T, view: View)
    fun onItemLongClick(model: T, view: View)
}