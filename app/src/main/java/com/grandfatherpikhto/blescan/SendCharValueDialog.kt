package com.grandfatherpikhto.blescan

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.grandfatherpikhto.blescan.databinding.SendCharDialogBinding
import com.grandfatherpikhto.blescan.helper.splitToByteArray
import com.grandfatherpikhto.blin.listeners.BtIoInterface
import com.grandfatherpikhto.blin.listeners.loaders.BtIoInterfaceLazy
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*

@InternalCoroutinesApi
@DelicateCoroutinesApi
class SendCharValueDialog() : DialogFragment() {
    companion object {
        const val TAG:String = "SendCharValueDialog"
    }

    enum class Regime(val value:Int) {
        String(R.string.regime_string),
        Array(R.string.regime_array),
        HexArray(R.string.regime_hex_array)
    }

    private val btIoInterface: BtIoInterface by BtIoInterfaceLazy()
    var uuid:UUID? = null

    /** */
    private val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private var _binding:SendCharDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * https://stackoverflow.com/questions/61205151/how-to-correctly-use-android-view-binding-in-dialogfragment
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = SendCharDialogBinding.inflate(LayoutInflater.from(context))
        initElements()
        Log.e(TAG, "onCreateView $binding")
        val builder = AlertDialog.Builder(requireActivity())
        builder.setView(binding.root)
        builder.setMessage(R.string.dialog_char_val_title)
        builder.setPositiveButton(R.string.send_ok,
            DialogInterface.OnClickListener { dialog, id ->
                Log.d(TAG, "Dialog: $dialog")
                sendValue()
            })
            .setNegativeButton(R.string.send_cancel,
                DialogInterface.OnClickListener { dialog, id ->
                    // User cancelled the dialog
                    Log.d(TAG, "Cancel")
            })

        return builder.create()
    }

    override fun onPause() {
        binding.apply {
            preferences.edit {
                putInt("REGIME", npRegime.value)
            }
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.apply {
            preferences.let{
                npRegime.value = it.getInt("REGIME", 0)
            }
        }
    }

    private fun initElements() {
        binding.apply {
            npRegime.minValue = 0
            npRegime.maxValue = Regime.values().size - 1
            val values = arrayListOf<String>()
            Regime.values().forEach { value ->
                values.add(requireContext().getString(value.value))
            }
            npRegime.displayedValues = values.toTypedArray()
        }
    }

    @ExperimentalUnsignedTypes
    private fun sendValue() {
        binding.apply {
            when(npRegime.value) {
                0 -> {
                    Log.d(TAG, etValue.text.toString().toByteArray().joinToString(","))
                    uuid?.let {
                        btIoInterface.writeCharacteristic(it, etValue.text.toString().toByteArray())
                    }
                }
                1 -> {
                    Log.d(TAG, etValue.text.toString().splitToByteArray().joinToString(","))
                    uuid?.let {
                        btIoInterface.writeCharacteristic(it, etValue.text.toString().splitToByteArray())
                    }
                }
                2 -> {
                    Log.d(TAG, etValue.text.toString().splitToByteArray(16).joinToString(","))
                    uuid?.let {
                        btIoInterface.writeCharacteristic(it, etValue.text.toString().splitToByteArray(16))
                    }
                }
            }
        }
    }
}