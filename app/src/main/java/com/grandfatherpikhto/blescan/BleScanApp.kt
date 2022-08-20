package com.grandfatherpikhto.blescan

import android.app.Application
import com.grandfatherpikhto.blescan.blemanager.AppBleManager
import com.grandfatherpikhto.blin.BleManagerInterface

class BleScanApp : Application() {
    var bleManager: AppBleManager? = null
}