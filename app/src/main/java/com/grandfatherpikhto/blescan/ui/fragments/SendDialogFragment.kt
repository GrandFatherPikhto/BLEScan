package com.grandfatherpikhto.blescan.ui.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.grandfatherpikhto.blescan.R
import com.grandfatherpikhto.blescan.databinding.DialogSendValueBinding
import java.nio.ByteBuffer

class SendDialogFragment : DialogFragment() {
    private val tagLog = javaClass.simpleName

    private var _binding:DialogSendValueBinding? = null
    private val binding get() = _binding!!

    private var valueListener: ((value: ByteArray?) -> Unit) ? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { fragmentActivity ->
            val inflater = fragmentActivity.layoutInflater
            _binding = DialogSendValueBinding.inflate(inflater)
            bindingElements()

            val builder = AlertDialog.Builder(fragmentActivity)
                .setView(binding.root)
                .setPositiveButton(R.string.write_value, null)
                .setNegativeButton(R.string.write_cancell, null)


            val dialog = builder.create()

            dialog.setOnShowListener { _ ->
                val buttonPositive = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                Log.d(tagLog, "buttonPositive: $buttonPositive")
                buttonPositive.setOnClickListener {
                    Log.d(tagLog, "Button Positive Clicked")
                    valueListener?.let { listener ->
                        parseInputValue()?.let { value ->
                            listener(value)
                            dismiss()
                        }
                    }
                }
            }

            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setOnSelectValueListener(listener: (value: ByteArray?) -> Unit) {
        valueListener = listener
    }

    private fun bindingElements() {
        binding.apply {
            cbHex.isVisible = chipInteger.isChecked || chipBytes.isChecked
            chipGroupFormat.setOnCheckedStateChangeListener { _, _ ->
                cbHex.isVisible = chipInteger.isChecked || chipBytes.isChecked
            }
        }
    }

    private fun parseInputValue(): ByteArray? {
        binding.apply {
            val radix = if (cbHex.isChecked) 16 else 10
            return when {
                chipBytes.isChecked -> {
                    etSend.runCatching {
                        text.split(Regex("\\s[\\W]\\s*"))
                            .map { it.toInt(radix).toByte() }
                            .toByteArray()
                    }.onFailure {
                        Log.d(tagLog, "Error: $it")
                        Toast.makeText(requireContext(),
                            getString(R.string.error_bytes, etSend.text), Toast.LENGTH_LONG).show()
                    }.getOrNull()
                }
                chipText.isChecked -> {
                    etSend.text.toString().toByteArray()
                }
                chipFloat.isChecked -> {
                    etSend.text.toString().runCatching {
                        toFloat()
                    }.onFailure {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error_float, etSend.text), Toast.LENGTH_LONG
                        ).show()
                    }.getOrNull()?.let {
                        it.runCatching {
                            ByteBuffer
                                .allocate(Float.SIZE_BYTES)
                                .putFloat(it).array()
                        }.onFailure {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.error_bytes, etSend.text), Toast.LENGTH_LONG
                            ).show()
                        }.getOrNull()
                    }
                }
                chipInteger.isChecked -> {
                    etSend.text.toString().runCatching {
                        toInt()
                    }.getOrNull()?.let {
                        ByteBuffer.allocate(Int.SIZE_BYTES)
                            .putInt(it)
                    }
                    null
                }
                else -> null
            }
        }
    }
}