package com.grandfatherpikhto.blescan

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
import com.grandfatherpikhto.blescan.service.BtLeScanner
import com.grandfatherpikhto.blescan.service.BtLeService
import com.grandfatherpikhto.blescan.service.BtLeServiceConnector

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivityModel.ready.observe(viewLifecycleOwner, { ready ->
            requireActivity().invalidateOptionsMenu()
        })
        bindAction(view)
        mainActivityModel.enabled.observe(viewLifecycleOwner, { enabled ->
            requireActivity().invalidateOptionsMenu()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        mainActivityModel.enabled.value?.let { enabled ->
            if(enabled) {
                mainActivityModel.ready.value?.let { ready ->
                    menuInflater.inflate(R.menu.menu_scan, menu)
                    super.onCreateOptionsMenu(menu, menuInflater)
                    bindMenuReaction(menu)
                }
                Log.d(TAG, "onCreateOptionsMenu()")
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.scan_bluetooth -> {
                if(btLeModel.state.value == BtLeService.State.Scanning) {
                    Log.d(TAG, "Stop Scan")
                    btLeModel.changeAction(Action.None)
                } else {
                    Log.d(TAG, "Start Scan")
                    btLeModel.changeAction(Action.Scan)
                }
                true
            }
            R.id.paired_bluetooth -> {
                BtLeServiceConnector.stopScan()
                BtLeServiceConnector.pairedDevices()
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
        BtLeServiceConnector.stopScan()
    }

    private fun initRvAdapter() {
        rvBtAdapter.setOnItemClickListener(object : RvItemClick<BtLeDevice> {
            override fun onItemClick(model: BtLeDevice, view: View) {
                Toast.makeText(
                    requireContext(),
                    "Сканируем адрес ${model.address}",
                    Toast.LENGTH_LONG).show()
                BtLeServiceConnector.stopScan()
                btLeModel.clean()
                BtLeServiceConnector.scanLeDevices(addresses = arrayOf(model.address), mode = BtLeScanner.Mode.StopOnFind)
            }

            override fun onItemLongClick(model: BtLeDevice, view: View) {
                Toast.makeText(
                    requireContext(),
                    "Подключаемся к ${model.address}",
                    Toast.LENGTH_LONG).show()
                connectToBt(model)
            }

        })

        bindRvAdapter()
    }

    /**
     *
     */
    private fun bindMenuReaction(menu: Menu) {
        val menuItemScanStart = menu.findItem(R.id.scan_bluetooth)
        btLeModel.state.observe(viewLifecycleOwner, { state ->
            if(state == BtLeService.State.Scanning) {
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
            if(btLeModel.devices != null) {
                rvBtAdapter.setBtDevices(btLeModel.devices.value!!.toSet())
            }

            btLeModel.devices.observe(viewLifecycleOwner, { devices ->
                rvBtAdapter.setBtDevices(devices.toSet())
            })
            btLeModel.bound.observe(viewLifecycleOwner, { isBond ->
                // btLeScanService = BtLeScanServiceConnector.service
                // btLeScanService?.scanLeDevices(name = AppConst.DEFAULT_NAME)
            })
        }
    }

    private fun bindAction (view: View) {
        btLeModel.bound.observe(viewLifecycleOwner, { isBond ->
            if(isBond) {
                btLeModel.action.observe(viewLifecycleOwner, { action ->
                    Log.d(TAG, "$action")
                    when(action) {
                        Action.None -> {
                            BtLeServiceConnector.service?.stopScan()
                        }
                        Action.Scan -> {
                            btLeModel.clean()
                            BtLeServiceConnector.scanLeDevices(names = settings.getString("names_filter", ""),
                                addresses = settings.getString("addresses_filter", ""))
                        }
                        Action.Paired -> {
                            btLeModel.clean()
                            BtLeServiceConnector.service?.stopScan()
                            BtLeServiceConnector.service?.pairedDevices()
                        }
                        else -> {}
                    }
                })
            }
        })
    }

    private fun connectToBt(model: BtLeDevice) {
        mainActivityModel.changeDevice(model)
        mainActivityModel.changeCurrent(MainActivity.Current.Device)
    }
}