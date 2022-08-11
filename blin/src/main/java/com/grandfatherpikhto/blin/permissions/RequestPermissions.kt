package com.grandfatherpikhto.blin.permissions

import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RequestPermissions constructor(private val activity: AppCompatActivity)
    : DefaultLifecycleObserver {
    private val logTag = this.javaClass.simpleName

    private val mutableStateFlowRequestPermission =
        MutableStateFlow<RequestPermission?>(null)
    val stateFlowRequestPermission get() = mutableStateFlowRequestPermission.asStateFlow()
    val valueRequestPermission get() = mutableStateFlowRequestPermission.value

    private val requestPermissions: MutableList<RequestPermission> = mutableListOf()

    private var currentRequestPermission: String = ""

    /**
     * Запрос группы разрешений
     * Ланчер необходимо вынести в глобальные переменные, потому что
     * он должен быть инициализирован ДО запуска Активности.
     * В противном случае, будет ошибка запроса, если мы вздумаем
     * перезапросить разрешения после запуска полного запуска приложения
     */
    private val launcherMultiplePermissions = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { results ->
        results?.forEach { result ->
            addResult(RequestPermission(result.key, result.value))
        }
    }

    private val launcherPermission = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
        addResult(RequestPermission(currentRequestPermission, granted))
    }

    private fun addResult(requestPermission: RequestPermission) {
        mutableStateFlowRequestPermission
            .tryEmit(requestPermission)
        if ( !requestPermissions.contains(requestPermission)) {
            requestPermissions.add(requestPermission)
            mutableStateFlowRequestPermission.tryEmit(requestPermission)
        }
    }

    fun requestPermission(permission: String) {
        if ( activity.checkSelfPermission(permission)
            != PackageManager.PERMISSION_GRANTED ) {
            currentRequestPermission = permission
            launcherPermission.launch(permission)
        }
    }

    fun requestPermissions(permissions: List<String>) {
        val requesting = mutableListOf<String>()
        permissions.forEach { permission ->
            if (activity.checkSelfPermission(permission)
                != PackageManager.PERMISSION_GRANTED) {
                requesting.add(permission)
            }
        }

        if (requesting.isNotEmpty()) {
            launcherMultiplePermissions.launch(requesting.toTypedArray())
        }
    }
}