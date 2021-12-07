package com.grandfatherpikhto.blescan

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.grandfatherpikhto.blescan.adapter.RvGattAdapter
import com.grandfatherpikhto.blescan.databinding.BtDeviceBinding
import com.grandfatherpikhto.blescan.databinding.FragmentDeviceBinding
import com.grandfatherpikhto.blescan.model.BtLeModel
import com.grandfatherpikhto.blescan.model.MainActivityModel
import com.grandfatherpikhto.blescan.service.BtLeService
import com.grandfatherpikhto.blescan.service.BtLeServiceConnector
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
@InternalCoroutinesApi
@DelicateCoroutinesApi
class DeviceFragment : Fragment() {
    companion object {
        const val TAG:String = "DeviceFragment"
    }

    private var _binding: FragmentDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var bindingDevice: BtDeviceBinding


    /** */
    private val btLe:BtLeModel by viewModels()
    private val mainActivityModel:MainActivityModel by activityViewModels()

    private val rvGattAdapter by lazy {
        RvGattAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentDeviceBinding.inflate(inflater, container, false)
        bindRvAdapter()
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindingDevice   = BtDeviceBinding.bind(view)
        bindingDevice.apply {
            mainActivityModel.device.value?.let { device ->
                tvBtDeviceAddress.text = device.address
                tvBtDeviceName.text    = device.name
                if (device.bondState == BluetoothDevice.BOND_NONE) {
                    ivBtPaired.setImageResource(R.drawable.ic_baseline_bluetooth_24)
                } else {
                    ivBtPaired.setImageResource(R.drawable.ic_baseline_bluetooth_connected_24)
                }
            }
        }

        btLe.state.observe(viewLifecycleOwner, { state ->
            when(state) {
//                BtLeService.State.Error         -> { setSnakeMessage(view, getString(R.string.state_unkown))  }
//                BtLeService.State.Rescan        -> { setSnakeMessage(view, getString(R.string.start_scan)) }
                BtLeService.State.Connected     -> { setSnakeMessage(view, getString(R.string.state_connected)) }
//                BtLeService.State.Disconnected  -> { setSnakeMessage(view, getString(R.string.state_disconnected)) }
//                BtLeService.State.Connecting    -> { setSnakeMessage(view, getString(R.string.state_connecting)) }
                BtLeService.State.FatalError      -> { mainActivityModel.changeCurrent(MainActivity.Current.Scanner) }
                else -> { Log.d(TAG, "Неизвестное состояние: $state") }
            }
        })

        btLe.gatt.observe(viewLifecycleOwner, { gatt ->
            gatt?.let { rvGattAdapter.setGatt(it) }
            gatt?.services?.forEach { service ->
                Log.d(TAG, "Service: ${service.uuid} ${service.type}")
                service?.characteristics?.forEach { characteristic ->
                    Log.d(TAG, "Characteristic: ${characteristic.uuid} ${characteristic.properties}")
                    characteristic?.descriptors?.forEach { descriptor ->
                        Log.d(TAG, "Descriptor: ${descriptor.uuid}")
                    }
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        mainActivityModel.device.value?.let { device ->
            BtLeServiceConnector.connect(device.address)
        }
    }

    override fun onStop() {
        super.onStop()
        BtLeServiceConnector.close()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setSnakeMessage(view: View, message: String) {
        Log.d(TAG, "Snake message: $message")
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
    }

    private fun bindRvAdapter() {
        binding?.apply {
            rvGatt.adapter = rvGattAdapter
            rvGatt.layoutManager = LinearLayoutManager(requireContext())
        }
    }
}