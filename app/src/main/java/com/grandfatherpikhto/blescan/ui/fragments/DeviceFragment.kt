package com.grandfatherpikhto.blescan.ui.fragments

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.grandfatherpikhto.blescan.BleScanApp
import com.grandfatherpikhto.blin.orig.AbstractBleBondManager
import com.grandfatherpikhto.blin.orig.AbstractBleGattManager
import com.grandfatherpikhto.blescan.R
import com.grandfatherpikhto.blescan.blemanager.AppBleManager
import com.grandfatherpikhto.blescan.databinding.FragmentDeviceBinding
import com.grandfatherpikhto.blescan.helper.linkMenuProvider
import com.grandfatherpikhto.blescan.helper.unlinkMenuProvider
import com.grandfatherpikhto.blescan.models.*
import com.grandfatherpikhto.blescan.ui.fragments.adapters.RvBleDeviceAdapter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class DeviceFragment : Fragment() {

    private val tagLog = this.javaClass.simpleName
    private var _binding: FragmentDeviceBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val mainActivityViewModel by activityViewModels<MainActivityViewModel>()
    private val deviceViewModel by viewModels<DeviceViewModel>()

    private val _bleManager: AppBleManager? by lazy {
        (requireActivity().application as BleScanApp).bleManager
    }
    private val bleManager get() = _bleManager!!

    private val rvBleDeviceAdapter = RvBleDeviceAdapter()

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_device, menu)
            menu.findItem(R.id.action_connect)?.let { actionConnect ->
                lifecycleScope.launch {
                    deviceViewModel.stateFlowConnectState.collect { state ->
                        when(state) {
                            AbstractBleGattManager.State.Disconnected -> {
                                actionConnect.setIcon(R.drawable.ic_connect)
                                actionConnect.title = getString(R.string.device_connect)
                            }
                            AbstractBleGattManager.State.Connected -> {
                                actionConnect.setIcon(R.drawable.ic_disconnect)
                                actionConnect.title = getString(R.string.device_disconnect)
                            }
                            else -> { }
                        }
                    }
                }
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when(menuItem.itemId) {
                R.id.action_connect -> {
                    when(deviceViewModel.connectState) {
                        AbstractBleGattManager.State.Connected -> {
                            bleManager.disconnect()
                            deviceViewModel.connected = false
                        }
                        AbstractBleGattManager.State.Disconnected -> {
                            mainActivityViewModel.scanResult?.let { scanResult ->
                                Log.d(tagLog, "Try Connecting(${scanResult.device.address})")
                                bleManager.connect(scanResult.device.address)
                                deviceViewModel.connected = true
                            }
                        }
                        else -> {

                        }
                    }
                    true
                }
                else -> { false }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDeviceBinding.inflate(inflater, container, false)
        deviceViewModel.changeBleManager(bleManager)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(tagLog, "Connected: ${deviceViewModel.connected}")

        if (deviceViewModel.connected) {
            mainActivityViewModel.scanResult?.let {
                bleManager.connect(it.device.address)
            }
        }

        linkMenuProvider(menuProvider)
        binding.apply {
            rvServices.adapter = rvBleDeviceAdapter
            rvServices.layoutManager = LinearLayoutManager(requireContext())
        }

        lifecycleScope.launch {
            deviceViewModel.stateFlowGatt.collect { bleGatt ->
                rvBleDeviceAdapter.bleGatt = bleGatt
            }
        }

        mainActivityViewModel.scanResult?.let { scanResult ->
            binding.apply {
                tvBleAddress.text = scanResult.device.address
                tvBleName.text = scanResult.device.name ?: getString(R.string.unknown_device)
                tvBleRssi.text = getString(R.string.rssi_title, scanResult.rssi)
                Log.d(tagLog, "bondState = ${scanResult.device.bondState} {${BluetoothDevice.BOND_BONDED}}")
                if (scanResult.device.bondState == BluetoothDevice.BOND_BONDED) {
                    ivBlePaired.setImageResource(R.drawable.ic_paired)
                } else {
                    ivBlePaired.setImageResource(R.drawable.ic_unpaired)
                }
            }
        }

        lifecycleScope.launch {
            deviceViewModel.stateFlowConnectState.collect { state ->
                when(state) {
                    AbstractBleGattManager.State.Disconnected -> {
                        binding.ivBleConnected.setImageResource(R.drawable.ic_connect_big)
                    }

                    AbstractBleGattManager.State.Connected -> {
                        binding.ivBleConnected.setImageResource(R.drawable.ic_disconnect_big)
                    }

                    else -> { }
                }
            }
        }

        binding.ivBleConnected.setOnClickListener { _ ->
            when(deviceViewModel.connectState) {
                AbstractBleGattManager.State.Connected -> {
                    bleManager.disconnect()
                    deviceViewModel.connected = false
                }
                AbstractBleGattManager.State.Disconnected -> {
                    mainActivityViewModel.scanResult?.let { scanResult ->
                        bleManager.connect(scanResult.device.address)
                        deviceViewModel.connected = true
                    }
                }
                else -> { }
            }
        }

        binding.ivBlePaired.setOnClickListener { _ ->
            mainActivityViewModel.scanResult?.let { scanResult ->
                if (scanResult.device.bondState != BluetoothDevice.BOND_BONDED) {
                    bleManager.bondRequest(scanResult.device.address)
                }
            }
        }

        lifecycleScope.launch {
            deviceViewModel.stateFlowBondState.filterNotNull().collect { bondState ->
                if (bondState.state == AbstractBleBondManager.State.Bonded) {
                    binding.ivBlePaired.setImageResource(R.drawable.ic_paired)
                } else if (bondState.state == AbstractBleBondManager.State.Reject) {
                    binding.ivBlePaired.setImageResource(R.drawable.ic_error)
                }
            }
        }

        rvBleDeviceAdapter.setOnCharacteristicReadClickListener { bleItem, _ ->
            bleManager.addGattData(bleItem.bleReadGattData)
        }

        rvBleDeviceAdapter.setOnCharacteristicWriteClickListener { bleItem, _ ->
            val sendDialogFragment = SendDialogFragment()
            sendDialogFragment.setOnSelectValueListener { value ->
                value?.let { characteristicValue ->
                    Log.d(tagLog, characteristicValue.joinToString(",") { String.format("%02X", it)})
                    bleItem.value = characteristicValue
                    bleManager.addGattData(bleItem.bleWriteGattData)
                }
            }
            sendDialogFragment.show(requireActivity().supportFragmentManager, "Dialog")
        }

        rvBleDeviceAdapter.setOnCharacteristicNotifyClickListener { bleItem, _ ->
            bleManager.notifyCharacteristic(bleItem.bleReadGattData)
        }

        rvBleDeviceAdapter.setOnDescriptorReadClickListener { bleItem, _ ->
            bleManager.addGattData(bleItem.bleReadGattData)
        }

        lifecycleScope.launch {
            deviceViewModel.sharedFlowCharacteristicNotify.collect {
                rvBleDeviceAdapter.changeCharacteristicNotify(it)
            }
        }

        lifecycleScope.launch {
            deviceViewModel.sharedFlowCharacteristic.collect {
                rvBleDeviceAdapter.changeCharacteristicValue(it)
            }
        }

        lifecycleScope.launch {
            deviceViewModel.sharedFlowDescriptor.collect {
                rvBleDeviceAdapter.changeDescriptorValue(it)
            }
        }
    }

    override fun onPause() {
        bleManager.disconnect()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unlinkMenuProvider(menuProvider)
        _binding = null
    }
}