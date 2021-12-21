package com.grandfatherpikhto.blescan

import android.bluetooth.BluetoothDevice
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.grandfatherpikhto.blescan.adapter.RvBtAdapter
import com.grandfatherpikhto.blescan.databinding.FragmentScanBinding
import com.grandfatherpikhto.blescan.model.*
import com.grandfatherpikhto.blin.BluetoothInterface
import com.grandfatherpikhto.blin.BluetoothInterfaceLazy
import com.grandfatherpikhto.blin.BtLeScanner

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
@DelicateCoroutinesApi
@InternalCoroutinesApi
class ScanFragment : Fragment() {
    companion object {
        const val TAG:String = "ScanFragment"
    }

    /** */
    enum class Action (val value:Int) {
        None(0x0),
        Scan(0x1),
        Paired(0x2)
    }

    /** */
    private val bluetoothInterface: BluetoothInterface by BluetoothInterfaceLazy()
    /** */
    private var _binding: FragmentScanBinding? = null
    /** This property is only valid between onCreateView and  onDestroyView. */
    private val binding get() = _binding!!
    /** */
    private val btLeModel: BtLeModel by viewModels()
    /** */
    private val mainActivityModel:MainActivityModel by activityViewModels()
    /** */
    private val rvBtAdapter: RvBtAdapter = RvBtAdapter()
    /** */
    private val settings:SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Включаем обраотку меню в этом фрагменте
        setHasOptionsMenu(true)

        _binding = FragmentScanBinding.inflate(inflater, container, false)
        initRvAdapter()

        return binding.root

    }

    /**
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivityModel.ready.observe(viewLifecycleOwner, { ready ->
            requireActivity().invalidateOptionsMenu()
        })
        mainActivityModel.enabled.observe(viewLifecycleOwner, { enabled ->
            requireActivity().invalidateOptionsMenu()
        })
        bindAction(view)
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        mainActivityModel.enabled.value?.let { enabled ->
            if(enabled) {
                mainActivityModel.ready.value?.let { ready ->
                    menuInflater.inflate(R.menu.menu_scan, menu)
                    super.onCreateOptionsMenu(menu, menuInflater)
                    bindMenuReaction(menu)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.scan_bluetooth -> {
                if(btLeModel.scanner.value == BtLeScanner.State.Scanning) {
                    Log.d(TAG, "Stop Scan")
                    btLeModel.changeAction(Action.None)
                } else {
                    Log.d(TAG, "Start Scan")
                    btLeModel.changeAction(Action.Scan)
                }
                true
            }
            R.id.paired_bluetooth -> {
                bluetoothInterface.stopScan()
                bluetoothInterface.pairedDevices()
                true
            }
            else -> { return super.onOptionsItemSelected(item) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStop() {
        super.onStop()
        bluetoothInterface.stopScan()
    }

    private fun initRvAdapter() {
        rvBtAdapter.setOnItemClickListener(object : RvItemClick<BluetoothDevice> {
            override fun onItemClick(model: BluetoothDevice, view: View) {
                connectToBluetoothDevice(model)
            }
            override fun onItemLongClick(model: BluetoothDevice, view: View) {
                rescanWithAddress(model)
            }
        })

        bindRvAdapter()
    }

    /**
     *
     */
    private fun bindMenuReaction(menu: Menu) {
        val menuItemScanStart = menu.findItem(R.id.scan_bluetooth)
        btLeModel.scanner.observe(viewLifecycleOwner, { state ->
            if(state == BtLeScanner.State.Scanning) {
                menuItemScanStart?.setIcon(R.drawable.ic_baseline_search_off_24)
                menuItemScanStart?.setTitle(R.string.stop_scan)
            } else {
                menuItemScanStart?.setIcon(R.drawable.ic_baseline_search_24)
                menuItemScanStart?.setTitle(R.string.start_scan)
            }
        })
    }

    private fun bindRvAdapter () {
        binding.apply {
            rvBtList.adapter = rvBtAdapter
            rvBtList.layoutManager = LinearLayoutManager(requireContext())
            rvBtAdapter.setBtDevices(btLeModel.devices.value!!.toSet())

            btLeModel.devices.observe(viewLifecycleOwner, { devices ->
                rvBtAdapter.setBtDevices(devices.toSet())
            })
            btLeModel.bond.observe(viewLifecycleOwner, { isBond ->
                // btLeScanService = BtLeScanServiceConnector.service
                // btLeScanService?.scanLeDevices(name = AppConst.DEFAULT_NAME)
            })
        }
    }

    /**
     * Следит за изменением LiveData переменной Action.
     * Запускает/останавливает сканирование или выводит
     * список сопряжённых устройств
     * Обрабатывается, только когда сервис уже привязан к
     * Активности!
     */
    private fun bindAction (view: View) {
        Log.d(TAG, "bindAction, bond = true")
        btLeModel.action.observe(viewLifecycleOwner, { action ->
            Log.d(TAG, "bindAction: $action")
            when(action) {
                Action.None -> {
                    bluetoothInterface.stopScan()
                }
                Action.Scan -> {
                    Log.d(TAG, "Action: $action")
                    btLeModel.clean()
                    bluetoothInterface.leScanDevices(names = settings.getString("names_filter", ""),
                        addresses = settings.getString("addresses_filter", ""))
                }
                Action.Paired -> {
                    btLeModel.clean()
                    bluetoothInterface.stopScan()
                    bluetoothInterface.pairedDevices()
                }
                else -> {}
            }
        })
    }

    private fun connectToBluetoothDevice(model: BluetoothDevice) {
        Toast.makeText(
            requireContext(),
            "Подключаемся к ${model.address}",
            Toast.LENGTH_LONG).show()
        mainActivityModel.changeDevice(model)
        mainActivityModel.changeCurrent(MainActivity.Current.Device)
    }

    private fun rescanWithAddress(model:BluetoothDevice) {
        Toast.makeText(
            requireContext(),
            "Сканируем адрес ${model.address}",
            Toast.LENGTH_LONG).show()
        bluetoothInterface.stopScan()
        btLeModel.clean()
        bluetoothInterface.leScanDevices(addresses = model.address, mode = BtLeScanner.Mode.StopOnFind)
    }
}