# Простейший сканнер BLE
![Программа BLEScan, скрин 01](./blescan01.png "Программа BLEScan, скрин 1")
![Программа BLEScan, скрин 02](./blescan02.png "Программа BLEScan, скрин 2")

## Зачем?
Честно говоря, библиотека 
[NordicSemiconductor Android BLE Library](https://github.com/NordicSemiconductor/Android-BLE-Library/)
полностью устраивает всех, кто хочет работать со стеком BLE на платформе Android.
Быть может, с проблемой просто не стоит связываться, учитвая просто огромный список сложностей (Issues), 
которые возникают при создании собственного стека BLE Android. К сожалению, официальном руководстве 
[Android BLE](https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview) об этом говорится очень немного.

Насколько понимаю, 
[проблема работы фильтров при сканировании устройств](https://stackoverflow.com/questions/34065210/android-ble-device-scan-with-filter-is-not-working/34092300)
на Android-устройствах, так до сих пор и не решена на многих устройствах.

Кроме того, есть прекрасная облегчённая библиотека Мартина Велле [BLESSED](https://github.com/weliem/blessed-android) написанная на Java
и аналогичная версия на Kotlin [Coroutines BLESSED](https://github.com/weliem/blessed-android-coroutines)

Однако, иногда бывает нужно сделать что-то совершенно своё, особенное. Для этого надо хорошее понимание основных проблемм работы со стеком BLE.

## Основные проблемы
Пожалуй, основная проблема BLE -- это нестабильное подключение к устройству. 
1. [`BluetoothGatt.discoverServices`](https://developer.android.com/reference/android/bluetooth/BluetoothGatt#discoverServices())
   Довольно часто возвращает `ложь`
   
2. [`BluetoothDevice.connectGatt`](https://developer.android.com/reference/android/bluetooth/BluetoothDevice#connectGatt(android.content.Context,%20boolean,%20android.bluetooth.BluetoothGattCallback))
   при неправильном использовании параметра `autoConnect` так же может вернуть ошибку со статусом 6 или 131 (плохо объяснённые в официальном руководстве
   ошибки). Причём значение параметра, насколько понимаю, зависит от версии Android и модели мобильного телефона. Мистика!
   
3. [BluetoothGattCallback.onConnectionStateChange](https://stackoverflow.com/questions/38666462/android-catching-ble-connection-fails-disconnects)
   не всегда срабатывает при отключении устройства, если скажем, оно не сопряжено с телефоном (некоторые устройства без сопряжения автоматически
   разрывают связь через 30 секунд) 

4. Количество [разрешений](https://developer.android.com/reference/android/Manifest.permission), 
   необходимых для 
   [включения/выключения, сканирования, считывания рекламы](https://developer.android.com/guide/topics/connectivity/bluetooth/permissions) 
   [`Bluetooth`](https://developer.android.com/guide/topics/connectivity/bluetooth), 
   [BLE](https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview), 
   постоянно меняется от одной версии к другой и выглядит несколько кошмарно:
   
```xml
<manifest>
    <!-- Запросить устаревшие разрешения Bluetooth на старых устройствах -->
    <uses-permission android:name="android.permission.BLUETOOTH"
        android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"
        android:maxSdkVersion="30" />

    <!-- Требуется только в том случае, если ваше приложение ищет устройства Bluetooth. 
         Если ваше приложение не использует результаты сканирования Bluetooth для получения 
         информации о физическом местоположении, вы можете твердо утверждать, 
         что ваше приложение не определяет физическое местоположение. -->
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />

    <!-- Требуется только в том случае, если ваше приложение позволяет обнаруживать устройство для 
         устройств Bluetooth. -->
    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />

    <!-- Требуется только в том случае, если ваше приложение обменивается данными с уже сопряженными 
         устройствами Bluetooth.. -->
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

    <!-- Требуется только в том случае, если ваше приложение использует результаты сканирования 
         Bluetooth для определения физического местоположения. -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    ...
</manifest>
```

Запросы уровня `dangerous`
[android.permission.ACCESS_FINE_LOCATION](https://developer.android.com/reference/android/Manifest.permission#ACCESS_FINE_LOCATION),
[android.permission.ACCESS_COARSE_LOCATION](https://developer.android.com/reference/android/Manifest.permission#ACCESS_COARSE_LOCATION),
[android.permission.ACCESS_BACKGROUND_LOCATION](https://developer.android.com/reference/android/Manifest.permission#ACCESS_COARSE_LOCATION)
нужно запрашивать напрямую, скажем, из `Активности`. Для этого созданы две функции, чтобы реализовать запрос массива разрешений:

```kotlin
    /**
     * Проверка группы разрешений
     */
    private fun requestPermissions(permissions: MutableList<String>) {
        var launchPermissions: MutableList<String> = arrayListOf()
        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Разрешение на $permission уже есть")
            } else {
                launchPermissions.add(permission)
            }
        }

        permissionsLauncher(launchPermissions)
    }

    /**
     * Запрос группы разрешений
     */
    private fun permissionsLauncher(permissions: List<String>) {
        if (permissions.isNotEmpty()) {
            val launcher =
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                    results?.entries?.forEach { result ->
                        val name = result.key
                        val isGranted = result.value
                        if (isGranted) {
                            Toast.makeText(this, "Разрешение на $name получено", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Toast.makeText(this, "Разрешение на $name не дано", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            launcher.launch(permissions.toTypedArray())
        }
    }
```

И запрос будет выглядеть так:

```kotlin
        requestPermissions(
            mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        )
```

## Простейший сканнер
Учитывая, что фильтры на многих устройствах не работают, лучше сразу заложить возможность фильтрования имён и адресов.

### Сервисы
Стоит ли делать сканнер сервисом, если он и так уже сервис или можно вызвать его в Активности/Фрагменте? 
В данном случае, вызов будет удобнее сделать именно в сервисе, поскольку, для стабильного подключения к устройству
иногда придётся делать кратковременное сканирование устройств с фильтром по адресу подключаемогоу устройства,
а значит сканирование должно быть доступно из разных объектов программы, в т.ч. из сервиса подключения к BLE-устройству.

Таким образом, будет два сервиса:

1. [`BtLeScanService`](./app/src/main/java/com/grandfatherpikhto/blescan/service/BtLeScanService.kt) -- сервис сканера
2. [`BtLeService`](./app/src/main/java/com/grandfatherpikhto/blescan/service/BtLeService.kt) -- сервис подключения к BLE устройству

После того, как созданы классы сервисов, надо прописать их в [`AndroidManifest.xml`](./app/src/main/AndroidManifest.xml):

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
        package="com.grandfatherpikhto.blescan">
    <application>
        <service android:name=".service.BtLeScanService" android:enabled="true" />
        <service android:name=".service.BtLeService" android:enabled="true" />
    </application>
</manifest>
```

в данной ситуации не будем использовать
[`LifecycleService`](https://developer.android.com/reference/androidx/lifecycle/LifecycleService))
хотя его возможности позволят отлично синхронизировать жизненные циклы `MainActivity` и сервиса.
Воспользуемся старым добрым [`Service`](https://developer.android.com/guide/components/services)
с привязкой при помощи [`IBinder`](https://developer.android.com/reference/android/os/IBinder) и
[`ServiceConnection`](https://developer.android.com/reference/android/content/ServiceConnection).

### Передачу данных между сервисами и объектами можно организовать при помощи трёх способов:

1. Можно создать широковещательные приемники/передатчики и рассылать/получать уведомления.
   при помощи [`Broadcastreceiver`](https://developer.android.com/reference/android/content/BroadcastReceiver),
   [`IntentFilter`](https://developer.android.com/reference/android/content/IntentFilter),
   [`sendBroadcast`](https://developer.android.com/reference/android/content/Context#sendBroadcast(android.content.Intent)),
   как это достаточно подробно описано в примерах [`broadcasts`](https://developer.android.com/guide/components/broadcasts).
   
```java
Intent intent = new Intent("com.grandfatherpikhto.blescan.NEW_DEVICE_DETECTED");
// Intent intent = new Intent();
// intent.setAction(NEW_DEVICE_DETECTED);

intent.putExtra("device", device.name);
intent.putExtra("address", device.address);
intent.putExtra("bond", device.getBondState());
sendBroadcast(intent);
```   

```kotlin
Intent().also { intent ->
    intent.setAction("com.grandfatherpikhto.blescan.NEW_DEVICE_DETECTED")
    intent.putExtra("device", device.name);
    intent.putExtra("address", device.address);
    intent.putExtra("bond", device.bondState);
    sendBroadcast(intent)
}
```

И не забыть добавить описание сообщений в [`AndroidManifest.xml`](./app/src/main/AndroidManifest.xml)

```xml
<receiver android:name=".MyBroadcastReceiver"  android:exported="true">
   <intent-filter>
      <action android:name="com.grandfatherpikhto.blescan.NEW_DEVICE_DETECTED"/>
   </intent-filter>
</receiver>
```

Недостаток широковещательного приёмника/передатчик состоит в том, что нельзя сразу передавать
объекты и скорость передачи относительно невысока, а нагрузка на Android при больших потоках 
данных, возрастает.

2. Если бы нагрузка была небольшой, можно было бы использовать 
  [`Preferences`](https://developer.android.com/reference/androidx/preference/package-summary).
  
3. Для кратковременного совместного использования сложных непостоянных определенных пользователем объектов рекомендуются следующие подходы:
  a. Класс [`android.app.Application`](https://developer.android.com/reference/android/app/Application).
  Он имеет несколько методов жизненного цикла и будет автоматически создан Android, если зарегистрировать его в 
  [AndroidManifest.xml](./app/src/main/AndroidManifest.xml):

```xml
  <application
      android:name=".BLEScanApp">
        
  </application>
``` 

  Доступ к синглетону приложения можно получить через 
  [`getApplication()`](https://developer.android.com/reference/android/app/Activity#getApplication()) 
  из любого действия или из [службы](https://developer.android.com/reference/android/app/Service#getApplication()).
   
4. Публичное статическое поле/метод
  Альтернативный способ сделать данные доступными для всех действий/служб — использовать 
  общедоступные статические поля и/или методы. Вы можете получить доступ к этим статическим 
  полям из любого другого класса в вашем приложении. Чтобы поделиться объектом, действие, 
  которое создает ваш объект, устанавливает статическое поле, указывающее на этот объект, 
  а любое другое действие, которое хочет использовать этот объект, просто обращается к 
  этому статическому полю.

5. Можно использовать [`HashMap`](https://developer.android.com/reference/java/util/HashMap)
  слабюых ссылок на объекты 
  [`WeakReferences`](https://developer.android.com/reference/java/lang/ref/WeakReference) 
  для объектов с длинными ключами.
   
   Когда действие хочет передать объект другому действию, оно просто помещает объект на карту 
   и отправляет ключ (который является уникальным Long на основе счетчика или отметки времени) 
   действию получателя через дополнительные функции намерения. 
   Действие получателя извлекает объект с помощью этого ключа.

6. Синглтон-класс
   
   У использования статического синглтона есть преимущества. Например, можно ссылаться на объекты, 
   не используя [`getApplication()`](https://developer.android.com/reference/android/app/Activity#getApplication())
   к классу, зависящему от приложения, 
   или можно сделать интерфейс на все подклассы Application, чтобы различные модули могли 
   ссылаться на этот интерфейс вместо этого.
  
   Жизненный цикл статики не находится под вашим контролем; поэтому, чтобы соответствовать 
   модели жизненного цикла, класс приложения должен инициировать и удалять эти статические 
   объекты в методах `onCreate()` и `onTerminate()` класса приложения.
      
7. Постоянные объекты. Даже если кажется, что приложение продолжает работать, система может 
   остановить его процесс и перезапустить его позже. Если у нас есть данные, которые необходимо 
   сохранять от одного вызова действия к другому, необходимо представить эти данные как состояние, 
   которое сохраняется действием, когда ему сообщается, что оно может исчезнуть.
   Для совместного использования сложных постоянных определенных пользователем объектов есть
   следующие подходы:
   
    • [`Application Preferences`](https://developer.android.com/jetpack/androidx/releases/preference)
    • [`Files`](https://developer.android.com/reference/java/io/File)
    • [`contentProviders`](https://developer.android.com/reference/android/content/ContentProvider)
    • [`SQLite`](https://developer.android.com/training/data-storage/sqlite) DB
  
   Если общие данные необходимо сохранить в точках, где процесс приложения может быть остановлен, 
   данные можно помкестить в постоянное хранилище, такое как настройки приложения, 
   база данных [`SQLite`](https://developer.android.com/training/data-storage/sqlite), 
   файлы или [`ContentProviders`](https://developer.android.com/reference/android/content/ContentProvider). 
   Подробнее в разделе [хранилище данных](https://developer.android.com/training/data-storage/room).
   
   В данном случае выбран синглтон (объект). Может быть, то не очень оптимально в смысле экономии
   памяти, но удобно в использовании.
   Создано два класса: [`BtLeScanServiceConnector`](./app/src/main/java/com/grandfatherpikhto/blescan/service/BtLeScanServiceConnector.kt)
   и [`BtLeServiceConnector`](./app/src/main/java/com/grandfatherpikhto/blescan/service/BtLeServiceConnector.kt)

### Активация сервисов

Пока, вызов сервисов находится в 
[`MainActivity`](./app/src/main/java/com/grandfatherpikhto/blescan/MainActivity.kt).
Можно использовать [`BLEScanApp`](./app/src/main/java/com/grandfatherpikhto/blescan/BLEScanApp.kt)
и наследовать сервисы от [`LifecycleService`](https://developer.android.com/reference/androidx/lifecycle/LifecycleService)
при повороте экрана сервисы не будут уничтожаться, скажем, при повороте экрана или уходе приложения в
фоновый режим.

### Навигация фрагментов

В этом примере навигация сделана довольно грубая. В [MainActivity](./app/src/main/java/com/grandfatherpikhto/blescan/MainActivity.kt)
создан `enum class Current`, значения которого указывают на объекты навигации из 
[nav_graph.xml](./app/src/main/res/navigation/nav_graph.xml)

```kotlin
    enum class Current (val value: Int) {
        None(0x00),
        Scanner(R.id.ScanFragment),
        Device(R.id.DeviceFragment)
    }
```

Объект [MutableLiveData](https://developer.android.com/reference/androidx/lifecycle/MutableLiveData) в
модели [MainActivityModel](./app/src/main/java/com/grandfatherpikhto/blescan/model/MainActivityModel.kt)
хранит идентификатор текущего активного фрейма:
```kotlin
    private val _current = MutableLiveData<MainActivity.Current>(MainActivity.Current.Scanner)
    val current:LiveData<MainActivity.Current> = _current
    
    fun changeCurrent(value: MainActivity.Current) {
        _current.postValue(value)
    }
```

Всё, что остаётся -- просто менять запись на нужное значение и, соответственно, переключаться между
[ScanFragment](./app/src/main/java/com/grandfatherpikhto/blescan/ScanFragment.kt) и 
[DeviceFragment](./app/src/main/java/com/grandfatherpikhto/blescan/DeviceFragment.kt)

События запуска сканирования и подключения к устройству обрабатываются внутри фрагментов.

Важно, что по-умолчанию генератор приложения AndroidStudio `Basic Activity` создаёт тэг `fragment`

```xml
    <fragment
        android:id="@+id/nav_host_fragment_content_main"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:defaultNavHost="true"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:navGraph="@navigation/nav_graph" />

```

Однако, этот тэг является устаревшим и если последовать совету автокорректировщика

```
Use FragmentContainerView instead of the <fragment> tag

Replace the <fragment> tag with FragmentContainerView.

FragmentContainerView replaces the <fragment> tag as the preferred way of adding fragments via XML. 
Unlike the <fragment> tag, FragmentContainerView uses a normal FragmentTransaction under the hood 
to add the initial fragment, allowing further FragmentTransaction operations on the 
FragmentContainerView and providing a consistent timing for lifecycle events.  
Issue id: FragmentTagUsage 
https://developer.android.com/reference/androidx/fragment/app/FragmentContainerView.html 
Vendor: Android Open Source Project (fragment-1.3.6) 
Identifier: fragment-1.3.6 Feedback: https://issuetracker.google.com/issues/new?component=192731

Fix: Replace with androidx.fragment.app.FragmentContainerView  
```
И заменить &lt;fragment&gt; на &lt;FragmentContainerView&gt;

```xml
    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/nav_host_fragment_content_main"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:defaultNavHost="true"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:navGraph="@navigation/nav_graph" />
```
Штатный код вызова 
[`findNavController`](https://developer.android.com/reference/androidx/navigation/Navigation#findNavController(android.app.Activity,kotlin.Int))
```kotlin
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
```

Работать уже не будет. Так, что его надо заменить на обращение к 
[`supportFragmentManager`](https://developer.android.com/reference/androidx/fragment/app/FragmentActivity#getSupportFragmentManager())
(см. [Navigation](https://developer.android.com/guide/navigation))

```kotlin
    private fun bindNavBar() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment?.findNavController()
        if(navController != null) {
            appBarConfiguration = AppBarConfiguration(navController.graph)
            setupActionBarWithNavController(navController, appBarConfiguration)
        }
    }
```

И навигация по фрагментам тогда будет работать так:
```kotlin
    private fun doNavigate(current: Current) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment?.findNavController()
        if(navController.currentDestination?.id != current.value) {
            navController.navigate(current.value)
        }
    }
```

Осталось «привязать» изменения значения поля `current` в модели `MainActivityModel` к навигации по текущему фрагменту:

```kotlin
        mainActivityModel.current.observe(this, { current ->
            doNavigate(current)
        })
```

## Материалы
1.  [Все работы Мартина Ван Велле](https://medium.com/@martijn.van.welie) Самое толковое и подробное описание работы с Bluetooth BLE, с кучей ссылок на различные источники.
    Подробно о сканировании устройств. Почему-то не отражена проблема сканирования устройств с фильтрами.
2.  [Making Android BLE work — part 1 // Martijn van Welie](https://medium.com/@martijn.van.welie/making-android-ble-work-part-1-a736dcd53b02?source=user_profile---------3-------------------------------) Часть 1. Как заставить Android BLE работать - часть 1
3.  [Making Android BLE work — part 2 // Martijn van Welie](https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07?source=user_profile---------2-------------------------------) Часть 2. Подключение, отключение, исследование сервисов
4.  [Making Android BLE work — part 3 // Martijn van Welie](https://medium.com/@martijn.van.welie/making-android-ble-work-part-3-117d3a8aee23?source=user_profile---------1-------------------------------) Часть 3. чтение/запись характеристик; включение/выключение уведомлений
5.  [Making Android BLE work — part 4 // Martijn van Welie](https://medium.com/@martijn.van.welie/making-android-ble-work-part-4-72a0b85cb442?source=user_profile---------0-------------------------------) Часть 4. Сопряжение с устройствами
6.  [Перевод работы Мартина Велле](https://habr.com/ru/post/536392/) Часть 1. Сканирование
7.  [Перевод работы Мартина Велле](https://habr.com/ru/post/537526/) Часть 2. Подключение/Отключение
8.  [Перевод работы Мартина Велле](https://habr.com/ru/post/538768/) Часть 3. Чтение/Запись характеристик
9.  [Перевод работы Мартина Велле](https://habr.com/ru/post/539740/) Часть 4. Сопряжение устройств
10. [BLESSED](https://github.com/weliem/blessed-android) A very compact Bluetooth Low Energy (BLE) library for Android 5 and higher, that makes working with BLE on Android very easy.
11. [BLESSED](https://github.com/weliem/blessed-android-coroutines) A very compact Bluetooth Low Energy (BLE) library for Android 8 and higher, that makes working with BLE on Android very easy. It is powered by Kotlin's Coroutines and turns asynchronous GATT methods into synchronous methods! It is based on the Blessed Java library and has been rewritten in Kotlin using Coroutines.
12. [(Talk) Bluetooth Low Energy On Android // Stuart Kent](https://www.stkent.com/2017/09/18/ble-on-android.html) (Обсуждение) Bluetooth Low Energy на Android // Стюарт Кент //
13. [Gist by Stuart Kent to Android BLE Talk](https://gist.github.com/stkent/a7f0d6b868e805da326b112d60a9f59b) Огромное количество ссылок на разные ресурсы.
14. [Android BLE Library](https://github.com/NordicSemiconductor/Android-BLE-Library/) Пожалуй, единственная Android библиотека, которая реально решает множество проблем Android с низким энергопотреблением Bluetooth и действительно нормально работает.
15. [Samsung Bluetooth Knox API](https://docs.samsungknox.com/dev/knox-sdk/bluetooth-support.htm) Работа с BLE на Samsung
16. [Samsung API](https://developer.samsung.com/smarttv/develop/api-references/tizen-web-device-api-references/systeminfo-api/getting-device-capabilities-using-systeminfo-api.html)
17. [Android BLE Issues](https://sweetblue.io/docs/Android-BLE-Issues) This is a short list of issues you will encounter if you try to use the native Android BLE stack directly // Краткий список проблем, с которыми вы столкнетесь, если попытаетесь напрямую использовать собственный стек Android BLE
18. [NordicSemiconductor - BLE Issues](https://github.com/NordicSemiconductor/Android-Ble-library/issues) Список проблем работы с BLE на GitHub
19. [Google: Fix Bluetooth problems on Android](https://support.google.com/android/answer/9769184?hl=en) Список проблем работы с Bluetooth от Google
20. [Android BLE Issues - SweetBlue](https://github.com/iDevicesInc/SweetBlue/wiki/Android-BLE-Issues) Ещё один, немного устаревший список проблем работы со стеком BLE
21. [Android BLE scan with filter issue](https://stackoverflow.com/questions/34065210/android-ble-device-scan-with-filter-is-not-working/34092300) Проблемы сканирования с фильтром. Похоже, до сих пор не исправлены
22. [We’ll prevent applications from starting and stopping scans more than 5 times in 30 second](https://github.com/NordicSemiconductor/Android-Scanner-Compat-Library/issues/18)
23. [Описание Bluetooth](https://ru.wikipedia.org/wiki/Bluetooth) Подробная статья о Bluetooth на Википедии.
24. [Bluetooth specifications](https://www.bluetooth.com/specifications/specs/) Спецификации Bluetooth.
25. [BLE Android official guide](https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview) Официальное руководство по работе с BLE.
26. [Find BLE Devices](https://developer.android.com/guide/topics/connectivity/bluetooth/find-ble-devices) Официальное руководство по работе с BLE. Сканирование.
27. [Connect GATT Server](https://developer.android.com/guide/topics/connectivity/bluetooth/connect-gatt-server) Подключение к серверу GATT.
28. [Transver BLE Data](https://developer.android.com/guide/topics/connectivity/bluetooth/transfer-ble-data) Передача/Приём данных через GATT.
29. [Android connectivity samples](https://github.com/android/connectivity-samples) Официальный набор отдельных проектов Android Studio, которые помогут вам приступить к написанию приложений Connectivity на Android.
