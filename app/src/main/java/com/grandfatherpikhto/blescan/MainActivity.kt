package com.grandfatherpikhto.blescan

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.grandfatherpikhto.blescan.databinding.ActivityMainBinding
import com.grandfatherpikhto.blescan.model.MainActivityModel
import com.grandfatherpikhto.blescan.service.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*


@DelicateCoroutinesApi
@InternalCoroutinesApi
class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG:String = "MainActivity"
    }

    enum class Current (val value: Int) {
        None(0x00),
        Scanner(R.id.ScanFragment),
        Device(R.id.DeviceFragment),
        Settings(R.id.SettingsFragment)
    }

    /** Менеджер блютуз. Нужен для запроса адаптера. Очень хочется убрать эту фигню из MainActivity */
    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    /** Адаптер Bluetooth. Обычно, он один, поэтому не заморачиваемся с проверкой */
    private val bluetoothAdapter:BluetoothAdapter by lazy {
        bluetoothManager.adapter
    }

    /** Главная модель. Видна везде */
    private val mainActivityModel:MainActivityModel by viewModels()

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    /**
     * Запрос группы разрешений
     */
    private val permissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { results ->
            results?.entries?.forEach { result ->
                val name = result.key
                val isGranted = result.value
                if (isGranted) {
                    Toast.makeText(this, "Разрешение на $name получено", Toast.LENGTH_SHORT)
                        .show()
                    mainActivityModel.andReady(true)
                } else {
                    Toast.makeText(this, "Разрешение на $name не дано", Toast.LENGTH_SHORT)
                        .show()
                    mainActivityModel.andReady(false)
                }
            }
        }

    /**
     * Ланчер для запроса на включение bluetooth
     */
    private val bluetoothLauncher
            = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == RESULT_OK) {
            mainActivityModel.andReady(true)
        } else {
            mainActivityModel.andReady(false)
        }
    }


    /**
     * Событие жизненного цикла Activity()
     * создание экземпляра
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        bindNavBar()

        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )

        requestEnableBluetooth()

        mainActivityModel.current.observe(this, { current ->
            doNavigate(current)
        })

        mainActivityModel.changeEnabled(bluetoothAdapter.isEnabled)
    }

    /**
     * Привязка главного меню
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        bindMenuReaction(menu)
        return true
    }

    /**
     * Обработка нажатий опций меню
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                mainActivityModel.changeCurrent(Current.Settings)
                true
            }
            R.id.action_enable_bluetooth -> {
                changeBluetoothState()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     *
     */
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
            || super.onSupportNavigateUp()
    }

    /**
     * Событие жизненного цикла Activity() onPause()
     */
    override fun onPause() {
        super.onPause()
        unbindService(BtLeServiceConnector)
    }

    /**
     * Событие жизненного цикла Activity() onResume()
     */
    override fun onResume() {
        super.onResume()
        Intent(this, BtLeService::class.java).also { intent ->
            bindService(intent, BtLeServiceConnector, Context.BIND_AUTO_CREATE)
        }
    }

    /**
     * Привязать обновление меню ко включению/выключению bluetoothAdapter
     */
    private fun bindMenuReaction(menu: Menu) {
        val itemEnableBluetooth = menu.findItem(R.id.action_enable_bluetooth)
        mainActivityModel.enabled.observe(this, { enabled ->
            if(enabled) {
                itemEnableBluetooth?.title = getString(R.string.action_disable_bluetooth)
            } else {
                itemEnableBluetooth?.title = getString(R.string.action_enable_bluetooth)
            }
            Log.d(TAG, "bindMenuReaction $enabled")
        })
    }

    /**
     * Запрос на включение Bluetooth
     */
    private fun requestEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        bluetoothLauncher.launch(enableBtIntent)
    }

    /**
     * Проверка группы разрешений
     */
    private fun requestPermissions(permissions: Array<String>) {
        val launchPermissions:MutableList<String> = mutableListOf<String>()

        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Разрешение на $permission уже есть")
                mainActivityModel.andReady(true)
            } else {
                launchPermissions.add(permission)
            }
        }

        if(launchPermissions.isNotEmpty()) {
            permissionsLauncher.launch(launchPermissions.toTypedArray())
        }
    }

    /**
     * Просто, вытащил функцию привязки навигационного бара из onCreate()
     */
    private fun bindNavBar() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment?.findNavController()
        if(navController != null) {
            appBarConfiguration = AppBarConfiguration(navController.graph)
            setupActionBarWithNavController(navController, appBarConfiguration)
        }
    }

    /**
     * Навигация между фрагментами
     */
    private fun doNavigate(current: Current) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment?.findNavController()
        if(navController.currentDestination?.id != current.value) {
            navController.navigate(current.value)
        }
    }

    /**
     * Если Блютуз выключен, включить. Если включён, выключить
     */
    private fun changeBluetoothState() {
        if(bluetoothAdapter.isEnabled) {
            bluetoothAdapter.disable()
        } else {
            requestEnableBluetooth()
        }
    }
}
