package com.grandfatherpikhto.blescan.ui

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.grandfatherpikhto.blin.permissions.RequestPermissions
import com.grandfatherpikhto.blescan.BleScanApp
import com.grandfatherpikhto.blescan.R
import com.grandfatherpikhto.blescan.blemanager.AppBleManager
import com.grandfatherpikhto.blescan.blemanager.MainBleManager
import com.grandfatherpikhto.blescan.databinding.ActivityMainBinding
import com.grandfatherpikhto.blescan.blemanager.FakeBleManager
import com.grandfatherpikhto.blescan.helper.linkMenu
import com.grandfatherpikhto.blescan.models.MainActivityViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    companion object {
        const val FAKE = "fake_debug"
    }
    private val tagLog = javaClass.simpleName

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var bleManager : AppBleManager

    private val mainActivityViewModel by viewModels<MainActivityViewModel>()

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_main, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.action_settings -> true
                else -> { false }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initBleManager()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
        requestPermissions()
        linkMenu(true, menuProvider)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        linkMenu(false, menuProvider)
        bleManager.onDestroy()
        super.onDestroy()
    }

    fun closeAppliction() {
        finishAndRemoveTask()
        exitProcess(0)
    }

    fun alertPermissionNotGranted(permission: String ) {
        AlertDialog.Builder(this@MainActivity).let { builder ->
            builder.setMessage(getString(R.string.permission_not_granted, permission))
            builder.create()
            builder.setTitle(R.string.alert_title)
            builder.apply {
                setPositiveButton(R.string.accessibly) { _, _ -> }
            }
            val dialog = builder.create()
            dialog.show()
        }
    }

    fun alertAdapterIsDisabled() {
        AlertDialog.Builder(this).let { builder ->
            builder.setMessage(R.string.request_enable_bluetooth_adapter)
            builder.setTitle(R.string.alert_title)
            builder.apply {
                setPositiveButton(R.string.accessibly) { _, _ ->
                    closeAppliction()
                }
            }
            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun requestPermissions() {
        if (!bleManager.isBluetoothAdapterEnabled) {
            Log.e(tagLog, getString(R.string.request_enable_bluetooth_adapter))
            alertAdapterIsDisabled()
            return
        }

        val requestPermissions = RequestPermissions(this)
        requestPermissions.requestPermissions(listOf(
            "android.permission.ACCESS_FINE_LOCATION",
        ))


        lifecycleScope.launch {
            requestPermissions.stateFlowRequestPermission.filterNotNull().collect { permission ->
                if (permission.granted) {
                    Log.d(tagLog, getString(R.string.permission_granted, permission.permission))
                    Toast.makeText(baseContext, getString(R.string.permission_granted, permission.permission), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(baseContext, getString(R.string.permission_not_granted, permission.permission), Toast.LENGTH_SHORT).show()
                    Log.e(tagLog, getString(R.string.permission_not_granted, permission.permission))
                    alertPermissionNotGranted(permission.permission)
                }
            }
        }
    }

    private fun initBleManager() {
        var fake = false
        intent.extras?.let { extras ->
            fake = extras.getBoolean(FAKE, false)
        }

        bleManager = if (fake) {
            FakeBleManager(applicationContext)
        } else {
            MainBleManager(applicationContext)
        }

        (applicationContext as BleScanApp).bleManager = bleManager
    }
}