package com.grandfatherpikhto.blin

import android.content.Context
import com.grandfatherpikhto.blin.orig.AbstractBleBondManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class BleBondManager(context: Context, dispatcher: CoroutineDispatcher = Dispatchers.IO)
    : AbstractBleBondManager(context, dispatcher)