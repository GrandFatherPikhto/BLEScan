# Простейший сканнер BLE

![Logo](images/blescan.svg)

## Внешний вид программы

![BLEScan, скрин №1](images/blescan01_min.png)
![BLEScan, скрин №2](images/blescan02_min.png)
![BLEScan, скрин №3](images/blescan03_min.png)
![BLEScan, скрин №4](images/blescan04_min.png)
![BLEScan, скрин №5](images/blescan05_min.png)
![BLEScan, скрин №6](images/blescan06_min.png)
![BLEScan, скрин №7](images/blescan07_min.png)

## Что уже есть?

[NordicSemiconductor Android BLE Library](https://github.com/NordicSemiconductor/Android-BLE-Library/) полностью устраивает большую часть разработчиков интерфейсов c BLE. Эта библиотека достаточно просто позволяет решить большую часть проблем, работы со стеком BLE на платформе Android.

Также, есть прекрасная облегчённая библиотека Мартейна ван Вейлли [BLESSED](https://github.com/weliem/blessed-android) написанная на Java
и аналогичная версия на Kotlin [Coroutines BLESSED](https://github.com/weliem/blessed-android-coroutines)

Живая и вполне поддерживаемая небольшая библиотека [BluetoothCommunicator](https://github.com/niedev/BluetoothCommunicator)

Одним словом, есть вполне работоспособные и действующие проекты, которые в значительной степени избавляют от необходимости изобретения очередного велосипеда и серии самоподрывов в собственном приложении из-за багов библиотеки BLE, накладывающихся на баги интерфейса.

Просмотр кода библиотеки [NordicSemiconductor Android BLE Library](https://github.com/NordicSemiconductor/Android-BLE-Library/) быстро отрезвляет и приводит к выводу, что лучше пользоваться громоздким, сложным, но уже готовым, чем изобретать велосипед. Создание собственной библиотеки BLE имеет, скорее учебное значение, нежели практическое применение. Однако, своей библиотекой [BLIN](https://github.com/GrandFatherPikhto/BLEScan/tree/master/blin) интерсивно пользуюсь в собственных разработках. Например, для создания интерфейсов управления микропроцессорными устройствами на базе [ESP32](https://espressif.com), [STM32](https://st.com) [PIC, AVE](https://www.microchip.com//) и т.д.

## Траблы (Issues)

Что касается 'нативого' стека BLE, с ним, по ряду вполне понятных причин, среди разработчиков мало кто хочет связываться. За десять лет 'нарисовался' изрядный список, порой, довольно странных сложностей (Issues), которые неизбежно возникают при создании собственного стека работы с BLE Android и которые сама компания Google, похоже исправлять не собирается от слова "совсем". К сожалению, официальном руководстве [Android BLE](https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview) об этом списке проблем почти ничего не говорится.

1. Самый мистичный момент ­— параметр autoConnect функции [public BluetoothGatt connectGatt (Context context, boolean autoConnect, BluetoothGattCallback callback)](https://developer.android.com/reference/android/bluetooth/BluetoothDevice#connectGatt(android.content.Context,%20boolean,%20android.bluetooth.BluetoothGattCallback)).

    Можно, конечно «ломануться» напрямую в стиле:

    ```Kotlin
        bleDeviceManager.connect(selectedBluetoothDevice.getDevice())
            .retry(3, 200)
            .useAutoConnect(false)
            .enqueue()
    ```

    Однако, это далеко не всегда помогает.

    Цитирую дословно [Мартейна Велле](https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07):
    > «Чтобы узнать, было ли кэшировано устройство, вы можете использовать небольшой трюк. После создания BluetoothDevice вы должны сделать это, getType() и если он вернет TYPE_UNKNOWN, устройство явно не закэшировано.

    Это решение, придуманно программистами NordicSemiconductor: в качестве значения `autoConnect` использовать

    ```bluetoothDevice!!.type == bluetoothDevice.DEVICE_TYPE_UNKNOWN```.

    Подробнее, см. [Martin van Wellie // Making Android BLE work — part 2](https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07?source=user_profile---------2-------------------------------)

    Вообще, правильное значение параметра, `autoConnect`, зависит от версии Android и модели мобильного телефона. Недокументированная мистика.

    В коде же получается что-то такое:

    ```Kotlin
        @SuppressLint("MissingPermission")
        private fun connect(address: String) : BluetoothGatt? {
            bleManager.bluetoothAdapter.getRemoteDevice(address)?.let { bluetoothDevice ->
                return device.connectGatt(
                    bleManager.applicationContext,
                    device.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN,
                    bleGattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
            }

            return null
        }
    ```

2. Ошибка `133`. Функция [public boolean discoverServices ()](https://developer.android.com/reference/android/bluetooth/BluetoothGatt#discoverServices()) часто возвращает `false` и генерирует ошибки с кодом `6/133`.

    ```log
    …
    E/DfuBaseService: Service discovery error: 129
    E/DfuBaseService: An error occurred while connecting to the device:16513
    D/BluetoothGatt: cancelOpen() - device: FA:B0:BE:04:6C:7E
    D/BluetoothGatt: onClientConnectionState() - status=133 clientIf=7 device=FA:B0:BE:04:6C:7E
    E/DfuBaseService: Connection state change error: 133 newState: 0
    …
    ```

    Разработчики [Nordic Semiconductor](https://devzone.nordicsemi.com/f/nordic-q-a/33313/android-gatt-133-error) проснифили обмен с устройством блютуз и обнаружили, что в работе стека BLE Android нарушен протокол Bluetooth 4+.
    > «Это известная проблема с Android, у нас были похожие проблемы, когда возвращалась ошибка `133`. В случае появления этой ошибки мы запускали сниффер, и увидели, что телефон сначала отправляет LL_VERSION_IND, а затем отправляет LL_FEATURE_REQ  до  того, как периферийное устройство отправило свою LL_VERSION_IND. Другими словами, телефон инициирует вторую процедуру управления LL до завершения первой, и это явное нарушение спецификации Bluetooth. Из-за этой ошибки SoftDevice отключается.» ©

    Не исключено, что это сделано намеренно, чтобы предотвратить действия злоумышленников, но это безумно раздражает!

    Самый простой способ решения проблемы, предложенный, опять-таки программистами [Nordic-Semiconductor](https://github.com/NordicSemiconductor/Android-DFU-Library/issues/1) (значится, как «BLE status = 133 problem #1») — при получении ошибки быстрое сканирование устройства с фильтром по адресу и повторная попытка подключения. Есть небольшая проблема: согласно официальному руководству, за 30 секунд у нас всего 5 попыток. После этого устройство блокируется системой примерно на 1 минуту.

    В штатном описании `133`-й ошибки, вообще отсутствует. Есть одинокая константа в файле [gatt_api.h](https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/adc9f28ad418356cb81640059b59eee4d862e6b4/stack/include/gatt_api.h#54)

    ```#define  GATT_ERROR                          0x85```

    Опять-таки, у Мартейна ван Велле говорится, что в этом случае, нужно просканировать устройство с фильтром по его mac-адресу (используя неагрессивный режим сканирования). После этого можно снова использовать автоподключение»
    [Мартин Веллие](https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07) пишет, что при возврате такой ошибки надо пересканировать устройство и повторить попытку подключения.

3. [Проблема работы фильтров при сканировании BLE устройств](https://stackoverflow.com/questions/34065210/android-ble-device-scan-with-filter-is-not-working/34092300), так до сих пор и не решена. Это известная «умолчанка» про [BluetoothLeScanner](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner) висит в списке issues уже лет десять. Так же, официально не заявлено, что при шестикратном повторном запуске сканирования, сканирование вообще блокируется на минуту.

4. [В официальном руководстве](https://android-doc.github.io/guide/topics/connectivity/bluetooth-le.html) не сказано о том, что не существует какого либо специального флага уведомлений о том, что характеристика работает в режиме ответа на запись [ENABLE_NOTIFICATION_VALUE](https://developer.android.com/reference/android/bluetooth/BluetoothGattDescriptor#ENABLE_NOTIFICATION_VALUE)/[ENABLE_INDICATION_VALUE](https://developer.android.com/reference/android/bluetooth/BluetoothGattDescriptor#ENABLE_INDICATION_VALUE). На самом деле, этот параметр не принадлежит характеристики. Это — значение дескриптора c UUID `00002902-0000-1000-8000-00805f9b34fb`. 
    Так, что придётся самим создать список характеристик, которые переведены в режим 'NOTIFICATION' и не забыть их вернуть в обычный режим по окончании работы приложения.
    Есть метод [setCharacteristicNotification(BluetoothGattCharacteristic characteristic, Boolean enable)](https://developer.android.com/reference/android/bluetooth/BluetoothGatt#setCharacteristicNotification(android.bluetooth.BluetoothGattCharacteristic,%20boolean)). Но толку от него мало, если Вы вручную не поменяете значение соответствующей notification/indication характеристики.

    ```Kotlin
        @SuppressLint("MissingPermission")
        private fun enableNotifyCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
            bluetoothGatt?.let { gatt ->
                bluetoothGattCharacteristic.getDescriptor(NOTIFY_DESCRIPTOR_UUID)
                    ?.let { bluetoothGattDescriptor ->
                        gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true)
                        bluetoothGattDescriptor.value =
                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        writeGattData(GattData(bluetoothGattDescriptor))
                    }
            }
        }

        @SuppressLint("MissingPermission")
        fun notifyCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
            if (isCharacteristicNotified(bluetoothGattCharacteristic)) {
                disableNotifyCharacteristic(bluetoothGattCharacteristic)
            } else {
                enableNotifyCharacteristic(bluetoothGattCharacteristic)
            }
        }
    ```

    И чтобы отследить изменения состояния Характеристики, нужно добавить слежение за изменением значений соответствующего дескриптора

    ```val NOTIFY_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb".uppercase())```

    Штатный метод [public void onCharacteristicChanged (BluetoothGatt gatt,     BluetoothGattCharacteristic characteristic, byte[] value)](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback#onCharacteristicChanged(android.bluetooth.BluetoothGatt,%20android.bluetooth.BluetoothGattCharacteristic,%20byte[])) срабатывает *только*, на включение `NOTIFICATION`/`INDICATION`.

    Придётся отлавливать изменения режима при помощи функции [public void onDescriptorRead (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status, byte[] value)](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback#onDescriptorRead(android.bluetooth.BluetoothGatt,%20android.bluetooth.BluetoothGattDescriptor,%20int,%20byte[])) и соорудить что-то вроде

    ```Kotlin
    private fun onDescriptorWrite(gatt : BluetoothGatt?, descriptor: BluetoothGattDescriptor?, ) {
    if (gatt != null && descriptor != null && descriptor.uuid == NOTIFY_DESCRIPTOR_UUID) {
        val notify = ByteBuffer
            .wrap(bluetoothGattDescriptor.value)
            .order(ByteOrder.LITTLE_ENDIAN).short.toInt()
        when(notify) {
            0 -> {
                if (isCharacteristicNotified(bluetoothGattDescriptor.characteristic)) {
                    Log.d(tagLog, "onCharacteristicChanged(${bluetoothGattDescriptor.characteristic.uuid}, notifyDisable)")
                    mutableListNotifiedCharacteristic.remove(bluetoothGattDescriptor.characteristic)
                    mutableSharedFlowCharacteristicNotify
                        .tryEmit(BleCharacteristicNotify(bluetoothGattDescriptor.characteristic.uuid, false))
                }
            }
            1 -> {
                if (!isCharacteristicNotified(bluetoothGattDescriptor.characteristic)) {
                    Log.d(tagLog, "onCharacteristicChanged(${bluetoothGattDescriptor.characteristic.uuid}, notifyEnable)")
                    mutableListNotifiedCharacteristic.add(bluetoothGattDescriptor.characteristic)
                    mutableSharedFlowCharacteristicNotify
                        .tryEmit(BleCharacteristicNotify(bluetoothGattDescriptor.characteristic.uuid, true))
                }
            }
            2 -> {


            }
            else -> {


            }
        }
    }
    }
    ```

5. Буфферы обмена надо делать самому. И, в данной ситуации это даже хорошо. Можно реализовать фичи, вроде Back-pressure и т.д.

6. Про [Knox-проблемы](https://docs.samsungknox.com/dev/knox-sdk/bluetooth-support.htm), которые получаются в жизни корейских устройств можно написать целую книгу. В частности, [public boolean createBond()](https://developer.android.com/reference/android/bluetooth/BluetoothDevice#createBond()) у меня так и не заработала на Samsung Galaxy 10se

7. [BluetoothGattCallback.onConnectionStateChange](https://stackoverflow.com/questions/38666462/android-catching-ble-connection-fails-disconnects) не всегда срабатывает при отключении устройства, если скажем, оно не сопряжено с телефоном (некоторые устройства без сопряжения автоматически  разрывают связь через 30 секунд) Поэтому, надо устанавливать ожидание сообщения об отключении, перед тем, как закрыть приложение. Иначе, некий внутренний счётчик подключений переполнится и устройство будет всё-время возвращать 6/133. Придётся очищать стек подключения (Настройки/Приложения/Показать Системны/Bluetooth/Очистить память, перезагрузка устройства), а заодно и поубирать Характеристики "взведённые" в режим подтверждения получения данных.

    ```    @SuppressLint("MissingPermission")
    private fun doDisconnect() = runBlocking {
        bluetoothGatt?.let { gatt ->
            mutableListNotifiedCharacteristic.forEach { characteristic ->
                disableNotifyCharacteristic(characteristic)
            }

            while (mutableListNotifiedCharacteristic.isNotEmpty()) {
                delay(20)
            }

            Log.d(tagLog, "doDisconnect($bluetoothDevice)")
            gatt.disconnect()
            while (connectState != State.Disconnected) {
                delay(20)
            }
            gatt.close()
        }
    }
    ```

8. Количество [разрешений](https://developer.android.com/reference/android/Manifest.permission), необходимых для [включения/выключения, сканирования, считывания рекламы](https://developer.android.com/guide/topics/connectivity/bluetooth/permissions) [`Bluetooth`](https://developer.android.com/guide/topics/connectivity/bluetooth), [BLE](https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview), постоянно растёт от одной версии к другой и нужно учитывать с какой версией мы имеем дело, чтобы программа нормально работала на наибольшем количестве устройств. Причём, с каждой версией список только растёт.
   Это не совсем баг, но явление довольно неприятное, хотя и нормальное для нормального развития любой библиотеки.

9. У Вас есть только 5 попыток пересканирования за 30 секунд. В противном случае, сканирование блокируется примерно на 1 минуту. Причём, «Штатный» [ScanCallback](https://developer.android.com/reference/android/bluetooth/le/ScanCallback) вообще не возвращает никаких ошибок, кроме сообщения в консоли отладчика, типа

    ```D/BluetoothLeScanner: onScannerRegistered() - status=6 scannerId=-1 mScannerId=0```

    Отловить эту ошибку фактически невозможно: она никуда не передаётся. Можете сами убедиться. Код предельно простой:

    ```Kotlin
        repeat(6) {
            runBlocking {
                startScan()
                delay(100)
                stopScan()
            }
        }
    ```

    Однако, `scanCallback` на основе [PendingIntent](https://developer.android.com/reference/android/app/PendingIntent) вполне решает проблему с контролем за ошибками подключения, но об этом, позже.

10. Режим фильтрации сканирования по адресу или UUID-Сервиса на основе [ScanFilter](https://developer.android.com/reference/android/bluetooth/le/ScanFilter) на многих устройствах до сих пор не работает. У меня не заработал ни на одном доступном мне телефоне. По какой-то причине этот дефект не устранён за последние... да лет пять тому, как, чтобы не соврать. Поэтому, нужно делать свой фильтр, что совсем не сложно, но не понятно, почему до сих пор не реализован в `штатном` режиме?

*Однако*, несмотря на изрядный набор ошибок и недоработок, иногда бывает нужно сделать что-то совершенно своё, особенное. Для этого надо хорошее понимание основных проблемм работы со стеком BLE и умение с ним обращаться. Например, захотелось триангулировать своё положение при помощи стека BLE. Возможно? Вполне. Например, [Determining the Proximity to an iBeacon Device](https://developer.apple.com/documentation/corelocation/determining_the_proximity_to_an_ibeacon_device)

## Что читать?

1. Официальная документация по стеку [Bluetooth LE](https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview) Android написана внятно и прозрачно. Однако, существует содержит длинный, список упорно не исправляемых разработчиками Google проблем (Issues). Если хотите, загляните в финал документа, там есть несколько ссылок, в т.ч. [Android BLE Issues от Google](https://support.google.com/android/answer/9769184?hl=en).

2. Однако, список не документированных проблем достаточно длинный. Скажем, вполне опрятный список есть у [SweetBlue Android BLE Issues](https://sweetblue.io/docs/Android-BLE-Issues)

3. Наконец, самый толковый документ для начинающего BLE-разработчика — это цикл статей Мартейна ван Велли (Martjein van Wellie, Amsterdam):

   1. [Making Android BLE work — part 1 // Martin van Welie](https://medium.com/@martijn.van.welie/making-android-ble-work-part-1-a736dcd53b02?source=user_profile---------3-------------------------------) Мартейн ван Велли. Часть 1. Как заставить Android BLE работать - часть 1
   2. [Making Android BLE work — part 2 // Martin van Welie](https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07?source=user_profile---------2-------------------------------) Мартейн ван Велли. Часть 2. Подключение, отключение, исследование сервисов
   3. [Making Android BLE work — part 3 // Martin van Welie](https://medium.com/@martijn.van.welie/making-android-ble-work-part-3-117d3a8aee23?source=user_profile---------1-------------------------------) Мартейн ван Велли. Часть 3. чтение/запись характеристик; включение/выключение уведомлений
   4. [Making Android BLE work — part 4 // Martin van Welie](https://medium.com/@martijn.van.welie/making-android-ble-work-part-4-72a0b85cb442?source=user_profile---------0-------------------------------) Мартейн ван Велли. Часть 4. Сопряжение с устройствами
   5. Есть отличный перевод этого цикла на Хабре:
      1. [Перевод статьи Мартейна ван Велле. Часть 1.](https://habr.com/ru/post/536392/) Сканирование
      2. [Перевод статьи Мартейна ван Велле. Часть 2.](https://habr.com/ru/post/537526/) Подключение/Отключение
      3. [Перевод статьи Мартейна ван Велле. Часть 3.](https://habr.com/ru/post/538768/) Чтение/Запись характеристик
      4. [Перевод статьи Мартейна ван Велле. Часть 4.](https://habr.com/ru/post/539740/) Сопряжение. Очереди. Включение/Выключение уведомлений характеристик
   6. Библиотеки Мартейна ван Велле
      1. [Wellie Blessed](https://github.com/weliem/blessed-android) — Библиотека для Андроид Blessed (Java)
      2. [Wellie Blessed Coroutine](https://github.com/weliem/blessed-android-coroutines) — Библиотека Мартейна ван Велле Blessed на Котлин

4. Существует небольшой, но очень дельный ~~китаёзный~~ гайд [Chee Yi Ong](https://punchthrough.com/author/cong/) — [The Ultimate Guide to Android Bluetooth Low Energy](https://punchthrough.com/android-ble-guide/). Настоятельно рекомендую для чтения, если Вы всё-таки решили «залезть» в тему BLE.
    Можно с уверенностью утверждать, что если Вы будете следовать в разработке своей библиотеки рекомендациям эти руководства, Ваше приложение будет работать хотя бы на 80% современных мобильных устройств, учитывая что особо не хочется поддерживать всё, что ниже версии Marshmallow (хотя, это не так уж и трудно — описано довольно подробно) и на 12-й версии описывают какие-то трудно объяснимые проблемы.
    В частности, [Android 12 Новые разрешения Bluetooth](https://stackoverflow.com/questions/67722950/android-12-new-bluetooth-permissions). Однако, утверждается, что эти проблемы исправлены. Нет гарантии, что не возникнут новые.

5. Разработка [Nordic Semiconductor](https://devzone.nordicsemi.com/f/nordic-q-a/33313/android-gatt-133-error)
   1. [BLE on Android v1.0.1](https://devzone.nordicsemi.com/cfs-file/__key/communityserver-blogs-components-weblogfiles/00-00-00-00-04-DZ-1046/2604.BLE_5F00_on_5F00_Android_5F00_v1.0.1.pdf) — несколько устаревшее, но подробное и внятное описание работы со стэком Bluetooth Low Energy на Android.
   2. [Android BLE Library](https://github.com/NordicSemiconductor/Android-BLE-Library) — лучшая на сей день работа в тематике Android BLE. Если Вы хотите залезть в тему по-настоящему, читайте исходные коды. Найдёте много ~~ада~~ удивительного.

6. Китаёзный гайд от [Espressif](https://espressif.com)
   1. [Рекомендации к Андроид-приложению](https://docs.espressif.com/projects/espressif-esp-faq/zh_CN/latest/application-solution/android-application.html)
   2. [Пример приложения RainMaker](https://github.com/espressif/esp-rainmaker-android) — Это официальное приложение для Android для ESP RainMaker — комплексного решения, предлагаемого Espressif для удаленного управления и мониторинга продуктов на базе ESP32-S2 и ESP32 без какой-либо настройки в облаке.
   3. [Библиотека для комплексной работы с EPS32](https://github.com/espressif/esp-idf-provisioning-android) ­— Библиотека для отправки сетевых учетных данных и/или пользовательских данных на устройства ESP32 (или его варианты, такие как S2, S3, C3 и т. д.) или устройств ESP8266.

## Структура программы

1. Проект состоит из Приложения ('app'),

2. Библиотеки Bluetooth Low Energy, которую почему-то назвал BLIN (Bluetooth Interface, а не то, что Вы подумали),

3. Зачем-то сделал виджет [MultistateButton](https://github.com/GrandFatherPikhto/BLEScan/tree/master/multistateButton). Честно говоря, сделал его просто так. Просто, чтобы разгрузить основной код. Хотя... нет. Думаю, хотел понять, почему большая часть самопальных виджетов неправильно позиционируется в [ConstraintLayout](https://developer.android.com/reference/androidx/constraintlayout/widget/ConstraintLayout). Может быть, это какой-то заговор?
   Увы, нет, не заговор. Дамы и господа, когда что-то клепаете, [мануалы](https://developer.android.com/guide/topics/appwidgets) всё-таки, надо читать!

## BLIN (Bluetooth Interface)

## Класс сканирования устройств [BtLeScanner](./app/src/main/java/com/grandfatherpikhto/blescan/service/BtLeScanner.kt)

Библиотека состоит из четырёх основных модулей:

1. [BleManager.kt](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleManager.kt)
    1. [BleScanManager.kt](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleScanManager.kt) — Модуль сканирования с использованием [BroadcastReceiver](https://developer.android.com/reference/android/content/BroadcastReceiver) и [PendingIntent](https://developer.android.com/reference/android/app/PendingIntent)
    2. [BleGattManager.kt](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleGattManager.kt) — Модуль подключения к [BluetoothGatt](https://developer.android.com/reference/android/bluetooth/BluetoothGatt). Подключение, исследование [GATT](https://www.bluetooth.com/bluetooth-resources/intro-to-bluetooth-gap-gatt/), буферизация IO данных, отправка и получение значений Характеристик и Дескрипторов.
    3. [BleBondManager.kt](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleBondManager.kt) — Модуль сопряжения устройства с телефоном. Без сопряжения устройство, скажем на основе [ESP32](https://espressif.com) держит связь около 30 секунд, а потом подключение закрывается
    4. [RequestPermissions.kt](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/permissions/RequestPermissions.kt) — Отдельно в библиотеке реализован класс для запроса Permissions, хотя это не совсем правильно, ведь разрешения привызываются к [Activity](https://developer.android.com/reference/android/app/Activity).
    5. [Idling](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/idling/) — Набор ожидалок для инструментального тестирования приложения.
    6. [FakeBleManager.kt](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/FakeBleManager.kt) ­— Фейковая заглушка, имитирующая работу всех сервисов, Сканирование, Сопряжение, Подключение, обмен данными и т.д. Поскольку [BleManager.kt](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleManager.kt), [FakeBleManager.kt](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/FakeBleManager.kt) наследованы от [BleManagerInterface.kt](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleManagerInterface.kt) вполне можно писать собственные тестовые фейки и подменять экземпляр `bleManager`, который традиционно лежит в [Application](https://developer.android.com/reference/android/app/Application)-классе. Других вариантов для инструментального тестирования, увы у меня для вас нет :(
    7. Обмен данными реализован при помощи связок [MutableStateFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-shared-flow/)/[StateFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/) и [MutableSharedFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-shared-flow.html)/[SharedFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-shared-flow/). Согласитесь, что библиотека Корутин и реализация холдных/горячих потоков, их преобразование «на лету» из одних в другие, выглядит потрясающе даже на фоне знаменитой [RxJava](https://github.com/ReactiveX/RxJava). А уж на фоне унылых [LiveData](https://developer.android.com/topic/libraries/architecture/livedata), так и вовсе, Космос рядом с крестьянской лошадкой...

## [AndroidManifest.xml](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/AndroidManifest.xml) — Файл манифеста библиотеки BLIN.

Содержит в себе только запросы разрешений, слизанные из заметки [Bluetooth permissions](https://developer.android.com/guide/topics/connectivity/bluetooth/permissions):

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.grandfatherpikhto.blin">
    <!-- Request legacy Bluetooth permissions on older devices. -->
    <uses-permission android:name="android.permission.BLUETOOTH"
        android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"
        android:maxSdkVersion="30" />
    <!-- Needed only if your app looks for Bluetooth devices.
             If your app doesn't use Bluetooth scan results to derive physical
             location information, you can strongly assert that your app
             doesn't derive physical location. -->
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />

    <!-- Needed only if your app makes the device discoverable to Bluetooth
         devices. -->
    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />

    <!-- Needed only if your app communicates with already-paired Bluetooth
         devices. -->
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

    <!-- Needed only if your app uses Bluetooth scan results to derive physical location. -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
</manifest>
```

Понятно, что «опасные» разрешения, такие как ```android.permission.ACCESS_COARSE_LOCATION``` необходимо запрашивать программно при помощи [registerForActivityResult](https://developer.android.com/training/basics/intents/result)

## [BleScanManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleScanManager.kt) ­— Менеджер Сканирования

Реализованы примитивные фильтры по имени устройства, адресу и UUID.

```kotlin
    @SuppressLint("MissingPermission")
    fun startScan(addresses: List<String> = listOf(),
                  names: List<String> = listOf(),
                  services: List<String> = listOf(),
                  stopOnFind: Boolean = false,
                  filterRepeatable: Boolean = false,
                  stopTimeout: Long = 0L
    ) : Boolean {
        // ...
        return true
    }
```

Фильтрация по адресу ```addresses: List<String>``` понадобится при подключении к устройству. Как и было сказано, часто случается, так, что метод
[BluetoothDevice.connectGatt()](https://developer.android.com/reference/android/bluetooth/BluetoothDevice?hl=en#connectGatt(android.content.Context,%20boolean,%20android.bluetooth.BluetoothGattCallback)) возвращает ошибку (6, 133). Чтобы устранить её, надо провести быстрое сканирование по адресу устройства подключения и снова повторить попытку подключения.

Для этого добавлена переменная ```stopOnFind: Boolean = false``` ­— после первого найденного совпадения, сканирование останавливается. Можно делать повторную попытку подключения.

Если параметр ```stopTimeout: Long = 0L``` не равен нулю, сканирование будет остановленно через указанное количество миллисекунд, вне зависимости от результатов. В противном случае, сканирование будет длиться вечно. Ну или, пока аппарат не разрядится или не сдохнет.

Параметр ```filterRepeatable: Boolean = false``` применяется для фильтрации повторяющихся значений. Сканнер периодически обходит цепочку доступных устройств, и генерирует сообщение каждый раз, а это не всегда нужно. В некоторых случаях бывает нужно эмитировать устройство с уникальными именем/адресом единожды. Для однократного оповещения надо включить ```filterRepeateble = true```

В сканнере реализована функция обратного вызова, через [PendingIntent](https://developer.android.com/reference/android/app/PendingIntent) и, соответственно [BroadcastReceiver](https://developer.android.com/reference/android/content/BroadcastReceiver). [BcScanReceiver.kt](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/receivers/BcScanReceiver.kt) получает уведомления о найденных устройствах в виде [ScanResult](https://developer.android.com/reference/android/bluetooth/le/ScanResult) и уведомления об ошибках. И при помощи функции обработного вызова отдаёт результаты в [BleScanManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleScanManager.kt), где они филтруются (если фильтры списков не пустые) и эмитируется событие `stateFlowScanResult` и `stateFlowBleScanResult`.

Эмитирование [ScanResult](https://developer.android.com/reference/android/bluetooth/le/ScanResult) «висит в пустоте» (никуда не передаётся). Антресольный рефлекс — на всякий случай.

Принял решение передавать только обёрнутые данные, в которых находятся только необходимые данные. Это, конечно, ограничивает возможности библиотеки, зато облегчает задачу тестирования. Генерирование и «мокание» [BluetoothDevice](https://developer.android.com/reference/android/bluetooth/BluetoothDevice), [ScanResult](https://developer.android.com/reference/android/bluetooth/le/ScanResult) — на Андроид устройстве, занятие муторное. Есть, конечно, пакет [Robolectric](http://robolectric.org/androidx_test/) с его чудными [Тенями](http://robolectric.org/extending/), но... он тоже работает только на компьютере. Система Андроид фактически мокинг не поддерживает.

В принципе, есть два способа подключения [PendingIntent](https://developer.android.com/reference/android/app/PendingIntent):

1. Статический.
   В этом случае надо создать статические «получатели» [PendingIntent()](https://developer.android.com/reference/android/app/PendingIntent), в классе, наследованном от [BroadcastReceiver()](https://developer.android.com/reference/android/content/BroadcastReceiver):

   ```kotlin
    // ...
    companion object Receiver {
        private const val TAG="BleScanReceiver"
        private const val ACTION_BLE_SCAN = "com.rqd.testscanbt.ACTION_BLE_SCAN"
        private fun newIntent(context: Context): Intent {
            Log.e(TAG, "newIntent()")
            val intent = Intent(
                context,
                BleScanReceiver::class.java
            )
            intent.action = ACTION_BLE_SCAN
            return intent
        }

        @SuppressLint("UnspecifiedImmutableFlag")
        fun getBroadcast(context: Context, requestCode: Int): PendingIntent {
            Log.e(TAG, "getBroadcast(requestCode = $requestCode, "
                + "flag = ${PendingIntent.FLAG_UPDATE_CURRENT})")
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                newIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    private var bleScanManager:BleScanManager? = null
    // ...
   ```

   В этом случае, нам остаётся получить в [BleScanManager.kt](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleScanManager.kt) экземпляр Намерения при помощи обращения к ленивому неизменяемому свойству bleScanPendingIntent

   ```kotlin
    private val bleScanPendingIntent: PendingIntent by lazy {
        BleScanReceiver.getBroadcast(
            context,
            REQUEST_CODE_BLE_SCANNER_PENDING_INTENT
        )
    }   
   ```

    Однако, необходимо не забыть прописать в ```AndroidManifest.xml``` этот рессивер и фильтры к нему:

    ```xml
        <application
        android:allowBackup="true"
        android:name=".BleApplication"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.LessonBleScan03">

        <receiver
            android:name=".BleScanReceiver"
            android:enabled="true"
            android:exported="false">
            <intent-filter>
                <category android:name="android.intent.category.DEFAULT" />
                <action android:name="com.rqd.testscanbt.ACTION_BLE_SCAN" />
                <!--                <action android:name="android.bluetooth.le.extra.LIST_SCAN_RESULT" />-->
                <!--                <action android:name="android.bluetooth.le.extra.CALLBACK_TYPE" />-->
                <!--                <action android:name="android.bluetooth.le.extra.ERROR_CODE" />-->
            </intent-filter>
        </receiver>
    </application>
    ```

    К существенным недостаткам этого способа стоит отнести, что надо где-то подвешивать либо [SingleTone](https://en.wikipedia.org/wiki/Singleton_pattern#:~:text=In%20software%20engineering%2C%20the%20singleton,coordinate%20actions%20across%20the%20system.), либо [companion object](https://kotlinlang.org/docs/object-declarations.html), через которые обмениваться данными между элементами UI, [Repsitory](https://developer.android.com/codelabs/basic-android-kotlin-training-repository-pattern#0), [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) и т.д. что само по себе уже плохо (в частности, для отладки — куча проблем). Андроидоводы вполне заслуженно уже давно отнесли [SingleTone](https://en.wikipedia.org/wiki/Singleton_pattern#:~:text=In%20software%20engineering%2C%20the%20singleton,coordinate%20actions%20across%20the%20system.) к антипаттернам.

    Достоинство: не надо загромождать код созданием объекта [BroadcastReceiver](https://developer.android.com/reference/android/content/BroadcastReceiver) и писать линейки фильтров. Весьма сомнительная радость... в нашем проекте точно не подойдёт. Библиотека, даже если она называется [BLIN](https://github.com/GrandFatherPikhto/BLEScan/tree/master/blin), должна быть максимально независима от приложения, поэтому, минимум обращений к ```AndroidManifest.xml```

2. Динамический (программный)
   Этот подход существенно проще. Нам не надо искать значения переменных для ```AndroidManifest.xml```.

   В рессивере пишем ленивую [lazy](https://kotlinlang.org/docs/delegated-properties.html) (да здравствуют делегаты!) константную переменную

   ```kotlin
   /**
     * Почему Missing PendingIntent mutability flag?
     */
    private val bcPendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            bleScanManager.applicationContext,
            REQUEST_CODE_BLE_SCANNER_PENDING_INTENT,
            Intent(ACTION_BLE_SCAN),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
    ```

    И уже в [BleScanManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleScanManager.kt) получаем экземпляр класса и обращаемся к методу:

    ```kotlin
        private var bleScanPendingIntent: PendingIntent = bcScanReceiver.pendingIntent
    ```

    Поскольку, все менеджеры наследованы от [DefaultLifecycleObserver](https://developer.android.com/reference/androidx/lifecycle/DefaultLifecycleObserver), используем штатные коллбэки LifeCycle: [DefaultLifecycleObserver.onCreate()](https://developer.android.com/reference/androidx/lifecycle/DefaultLifecycleObserver#onCreate(androidx.lifecycle.LifecycleOwner)), [DefaultLifecycleObserver.onDestroy()](https://developer.android.com/reference/androidx/lifecycle/DefaultLifecycleObserver#onDestroy(androidx.lifecycle.LifecycleOwner)). И в [DefaultLifecycleObserver.onCreate()](https://developer.android.com/reference/androidx/lifecycle/DefaultLifecycleObserver#onCreate(androidx.lifecycle.LifecycleOwner)) вызываем регистрацию рессивера, а в [DefaultLifecycleObserver.onDestroy()](https://developer.android.com/reference/androidx/lifecycle/DefaultLifecycleObserver#onDestroy(androidx.lifecycle.LifecycleOwner)) — его разрегистрацию:

    ```kotlin
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        Log.d(tagLog, "onCreate()")
        bleManager.applicationContext.registerReceiver(bcScanReceiver, makeIntentFilters())
    }

    override fun onDestroy(owner: LifecycleOwner) {
        bleManager.applicationContext.unregisterReceiver(bcScanReceiver)
        stopScan()
        super.onDestroy(owner)
    }
    ```

    и делаем метод для формирования списка фильтров событий:

    ```kotlin
    private fun makeIntentFilters() : IntentFilter = IntentFilter().let { intentFilter ->
        intentFilter.addAction(Intent.CATEGORY_DEFAULT)
        intentFilter.addAction(BcScanReceiver.ACTION_BLE_SCAN)
        intentFilter
    }
    ```

Основная часть работы совершается в интерфейсе обратных вызовов, наследованном от [BluetoothLeScanner](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner)

В [BcScanReceiver()](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/receivers/BcScanReceiver.kt)

Перегрузка метода [onReceive(context: Context?, intent: Intent?)] перехватывает события сканирования

```kotlin
    override fun onReceive(context: Context?, intent: Intent?) {
        if ( context != null && intent != null ) {
            when (intent.action) {
                ACTION_BLE_SCAN -> {
                    extractScanResult(intent)
                }
                else -> {
                    Log.d(tagLog, "Action: ${intent.action}")
                }
            }
        }
    }
```

Если `intent` не содержит ошибок ```intent.hasExtra(BluetoothLeScanner.EXTRA_ERROR_CODE)```, можно извлечь 'ScanResult':

```kotlin
        if (errorCode == -1 && intent.hasExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT)) {
            intent.getParcelableArrayListExtra<ScanResult>(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT)
                ?.let { results ->
                    results.forEach { result ->
                        result.device?.let { _ ->
                            // Log.d(tagLog, "Device: $device")
                            bleScanManager.onReceiveScanResult(result)
                            return result
                        }
                    }
                }
        }
```

Осталось «прогнать» результат через фильтры (если они заполнены значениями) и можно эмитрировать результат и остановить сканирование при совпадении, если взведена переменная `stopOnFind` (для рескана при получении ошибки `133`)

```kotlin
    private val mutableSharedFlowScanResult = MutableSharedFlow<ScanResult>(replay = 100)
    val sharedFlowScanResult:SharedFlow<ScanResult> get() = mutableSharedFlowScanResult.asSharedFlow()

    @SuppressLint("MissingPermission")
    fun onReceiveScanResult(scanResult: ScanResult) {
        scanResult.device.let { bluetoothDevice ->
            if ( filterName(bluetoothDevice)
                .and(filterAddress(bluetoothDevice))
                .and(filterUuids(bluetoothDevice.uuids))
            ) {
                if (isEmitScanResult(scanResult)) {
                    mutableSharedFlowScanResult.tryEmit(scanResult)
                    mutableSharedFlowBleScanResult.tryEmit(BleScanResult(scanResult))
                }
                if (stopOnFind &&
                    (names.isNotEmpty()
                    .or(addresses.isNotEmpty()
                    .or(uuids.isNotEmpty())))
                ) {
                    stopScan()
                }
            }
        }
    }
```

В некоторых случаях бывает нужен список отсканированных устройств. Перехватывать внутренний буффер [MutableSharedFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-shared-flow.html) — занятие вредное и совершенно бессмысленное. Примерно, как искать последний элемент в горячем общем потоке (Hot Shared Flow). В силу невероятной гибкости Котлин, это можно, конечно, сделать, но по-моему, лучше так делать не надо.
Так что, есть отдельный массив, содержащий неповторяющиеся элементы сканирования.

```kotlin
    val scanResults = mutableListOf<ScanResult>()
```

При помощи которого можно при необходимости отбрасывать повторные результаты сканирования. Вряд ли в списке будет больше 100 элементов, поэтому, нагрузка от такого массива будет не велика. А пока, что можно было и обойтись буффером из [MutableSharedFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-shared-flow.html)

Конструктор класса [BleScanManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleScanManager.kt) получает [context](https://developer.android.com/reference/android/content/Context). Это нужно для того, чтобы получить [BluetoothManager](https://developer.android.com/reference/android/bluetooth/BluetoothManager) и из него [BluetoothAdapter](https://developer.android.com/reference/android/bluetooth/BluetoothAdapter) и [BluetoothLeScanner](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner), необходимый для сканирования BLE-устройств.

Класс [BleScanManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleScanManager.kt) наследован от [DefaultLifecycleObserver](https://developer.android.com/reference/androidx/lifecycle/DefaultLifecycleObserver). Это не очень хорошо, так как лишает его универсальности. Ведь события создания класса и его разрушения привязаны к [DefaultLifecycleObserver.onCreate()](https://developer.android.com/reference/androidx/lifecycle/DefaultLifecycleObserver#onCreate(androidx.lifecycle.LifecycleOwner)) и [DefaultLifecycleObserver.onDestroy()](https://developer.android.com/reference/androidx/lifecycle/DefaultLifecycleObserver#onDestroy(androidx.lifecycle.LifecycleOwner)) родительского объекта. Потому, что надо обязательно закрывать подключение или останавливать сканирование при закрытии приложения. Иначе при повтороном подключении/сканировании, получим довольно удивительные ошибки.

В принципе, правильным было бы сделать метод `onDestroy()` и на этом «закрыть» тему на ответственность программиста, использующего библиотеку. Но...

А где Вы его собираетесь создавать? В классе, наследованным от [Application](https://developer.android.com/reference/android/app/Application)? Его разрушение можно отследить только из [Service](https://developer.android.com/reference/android/app/Service). Так, что придётся использовать в качестве создателя либо [Activity](https://developer.android.com/reference/android/app/Activity), либо [Service](https://developer.android.com/reference/android/app/Service). А у этих классов с `lifecycle` всё в порядке.

Второй аргумент, который получает класс [BleScanManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleScanManager.kt) — [CoroutineDispatcher](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-dispatcher/). Он нужен для того, чтобы при тестировании превратить асинхронные события в последовательные синхронные при помощи [UnconfinedTestDispatcher](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/kotlinx.coroutines.test/-unconfined-test-dispatcher.html). У аргумента есть «умолчальное» значение — [Dispatchers.IO](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-i-o.html).

## Unit-тестирование [BleScanManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleScanManager.kt)

Используется классическая связка [Mockito](https://site.mockito.org/)/[Robolectric](http://robolectric.org/androidx_test/).

Сделано несколько примитивных вспомогательных функций для генерирования [ScanResult](https://developer.android.com/reference/android/bluetooth/le/ScanResult), [BluetoothDevice](https://developer.android.com/reference/android/bluetooth/BluetoothDevice), [BluetoothGatt](https://developer.android.com/reference/android/bluetooth/BluetoothGatt) в [MockHelper](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/test/java/com/grandfatherpikhto/blin/helper/MockHelper.kt). Например, генерация рандомных [ScanResult](https://developer.android.com/reference/android/bluetooth/le/ScanResult) и [BluetoothDevice](https://developer.android.com/reference/android/bluetooth/BluetoothDevice)

```kotlin
fun mockBluetoothDevice(name: String? = null, address: String? = null): BluetoothDevice {
    val bluetoothDevice = mock<BluetoothDevice> { bluetoothDevice ->
        on {bluetoothDevice.name} doReturn name
        on {bluetoothDevice.address} doReturn (
            address ?: Random.nextBytes(6)
                .joinToString (":") {
                    String.format("%02X", it) })
        }

    return bluetoothDevice
}
// ...
```

Так, что код проверки ставится очень простым. Например, можно посмотреть состояние ```bleScanManager.flowState``` после запуска сканирования. Запустить набор сгенерированных BluetoothDevices, проверить список отфильтрованных устройств на совпадение. Проверить, остановилось ли сканирование ```bleScanManager.flowState```

```kotlin
    /**
     * Проверяет состояние после запуска сканирования.
     * "Запускает" набор сгенерированных BluetoothDevices
     * и проверяет список отфильтрованных устройств на совпадение
     * Останавливает сканирование и проверяет состояние flowState
     */
    @Test
    fun testScan() = runTest(UnconfinedTestDispatcher()) {
        bleManager.startScan()
        assertEquals(BleScanManager.State.Scanning, bleManager.scanState)
        val scanResults = mockRandomScanResults(7)
        scanResults.forEach { scanResult ->
            bleManager.bleScanManager.onReceiveScanResult(scanResult)
        }
        assertEquals(bleManager.bleScanManager.scanResults.map { it.device }, scanResults.map { it.device })
        bleManager.stopScan()
        assertEquals(BleScanManager.State.Stopped, bleManager.scanState)
    }
```

И так далее. Здесь можно использовать теневые объекты и творить что Вам заблагорассудится на любой фасон и вкус. Во-всяком случае, логику сканнера можно проверить полностью, хватило бы терпения и изобретательности.

Важно не забывать, что для тестирования асинхронных процессов надо использовать библиотеку [org.jetbrains.kotlinx:kotlinx-coroutines-test](https://github.com/Kotlin/kotlinx.coroutines/tree/master/kotlinx-coroutines-test)

```gradle
dependicies {
    ...
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito.kotlin:mockito-kotlin:4.0.0'
    testImplementation 'org.mockito:mockito-inline:4.6.1'
    testImplementation 'org.robolectric:robolectric:4.8.1'

    testImplementation 'androidx.test:core:1.5.0-alpha01'
    testImplementation 'androidx.test:core-ktx:1.5.0-alpha01'

    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4'
    ...
}
```

Это нам даст

1. [kotlinx-coroutines-test](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/) даст возможность использовать ```runTest(UnconfinedTestDispatcher())``` см. [UnconfinedTestDispatcher](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/kotlinx.coroutines.test/-unconfined-test-dispatcher.html) и [runTest](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/kotlinx.coroutines.test/run-test.html)
2. [androidx-test:core](https://developer.android.com/training/testing/index.html) и [androidx-test:core-ktx](https://developer.android.com/training/testing/index.html) дадут возможность использовать управляемый хорошо замоканный ```ApplicationProvider.getApplicationContext<Context?>().applicationContext```
3. [mockito-inline](https://github.com/mockito/mockito/wiki/What%27s-new-in-Mockito-2#mock-the-unmockable-opt-in-mocking-of-final-classesmethods) — доступ к `final` классам `Java`.
4. [mockito-kotlin](https://github.com/mockito/mockito-kotlin) — упрощает доступ к «моканию» в языке Котлин и избегать труднонабираемых конструкций, типа

   ```kotlin
    `when`(bluetoothDevice.name).thenReturn(name)
    ```

    связанных с совпадением ключевого слова ```when``` в Котлин и в [Mockito](https://site.mockito.org/). Так, что пользуйтесь классическим стилем [mockito-kotlin](https://github.com/mockito/mockito-kotlin) и вместо привычного явистам, переползшим на Котлин

    ```kotlin
    lenient().`when`(bluetoothDevice.name).thenReturn(name)
    ```

    делайте более котлински:

    ```kotlin
    mock<BluetoothDevice> { bluetoothDevice ->
        on { bluetoothDevice.name } doReturn name
        // ...
    }
    ```

5. [JUnit 4](https://junit.org/junit5/) — думаю, в разъяснениях особо не нуждается. Хотя, всё время маячит вопрос, почему, не [JUnit 5](https://junit.org/junit5/), который гораздо удобнее, чем Андроидовская четвёрка. Ответ простой: гугловцы до сих пор не удосужились перетащить [Jupiter](https://junit.org/junit5/) в Andoird Framework. Хотя, есть некоторые манипуляции с бубном, от [MannoDerMaus](https://github.com/mannodermaus/android-junit5) которые позволяют использовать в Unit-тестах пятую версию. [Правильный мануал](https://github.com/mannodermaus/android-junit5/wiki/Instrumentation-Tests-Setup) придётся читать очень внимательно. Там много действий и они весьма запутанные. Стоит вся эта возня ожидаемого результата или нет, решайте сами. Но всё прилично работает и вполне соответствует документации.

*`Примечание`: не забудьте добавить в [build.gradle](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/build.gradle) в разделе `andoird`,*

```gradle
    testOptions {
        unitTests {
            includeAndroidResources = true
        }
    }
```

*иначе, [Robolectric](http://robolectric.org/androidx_test/) выдаст сообщение о невозможности получить ресурсы проекта.*

## [BleGattManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleGattManager.kt) — Менеджер подключения и обмена данными с Bluetooth устройством 

Этот класс, в отличие от [BleScanManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleScanManager.kt) получает в конструкторе ещё и указатель на объект [BleScanManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleScanManager.kt), поскольку в случае получения пресловутой ошибки `133`, необходимо пересканировать устройства с фильтром адреса устройства, к которому подключаемся.

Так, что главный конструктор выглядит так:

```kotlin
class BleGattManager constructor(private val context: Context,
                                 private val bleScanManager: BleScanManager,
                                 dispatcher: CoroutineDispatcher = Dispatchers.IO)
    : DefaultLifecycleObserver {
        // ...
    }
```

главные методы этого класса — подключение к устройству с указанным адресом

```kotlin
connect(address: String) {
    // ...
}
```

и отключение

```kotlin
disconnect()
```

Причём, отключение реализуется в ждущем режиме: пока не будет получено уведомление об отключении, объект класса не разрушается. Это связано с тем, что внутренний счётчик подключений системы Андроид может переполниться и подключение к устройству будет постоянно возвращать ошибку `6`.

```kotlin
    @SuppressLint("MissingPermission")
    private fun doDisconnect() = runBlocking {
        bluetoothGatt?.let { gatt ->
            mutableListNotifiedCharacteristic.forEach { characteristic ->
                disableNotifyCharacteristic(characteristic)
            }

            withTimeout(WAIT_TIMEOUT) {
                while (mutableListNotifiedCharacteristic.isNotEmpty()) {
                    delay(20)
                }
            }

            gatt.disconnect()
            withTimeout(WAIT_TIMEOUT) {
                while (connectState != State.Disconnected) {
                    delay(20)
                }
            }
            gatt.close()
        }
    }
```

Тогда придётся сбрасывать кэш памяти Bluetooth в настройках телефона: Настройки / Приложения(Показать системные) / Bluetooth / Сбросить память.

Система сообщений [BluetoothGattCallback](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback) обёрнута в собственную систему оповещений, поскольку, может понадобиться отображение процесса подключения к устройству, отключения от него и т.д. и из-за того, что устройство считается подключённым только после того как обработан коллбэк [onServicesDiscovered](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback#onServicesDiscovered(android.bluetooth.BluetoothGatt,%20int)), а это событие происходит далеко не внутри [onConnectionStateChange](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt,%20int,%20int))

Так, что создан `enum` с набором состояний:

```kotlin
    enum class State(val value:Int) {
        Disconnected  (0x00), // Отключены
        Disconnecting (0x01), // Отключаемся
        Connecting    (0x02), // Подключаемся
        Connected     (0x02), // Подключены
        Error         (0xFF), // Получена ошибка
    }
```

Соответственно, `Disconnected` генерируется, когда в [onConnectionStateChange](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt,%20int,%20int)) приходит [BluetoothProfile.STATE_DISCONNECTED](https://developer.android.com/reference/android/bluetooth/BluetoothProfile#STATE_DISCONNECTED), `Disconnecting` эмитируется после вызова функции `disconnect()`. `Connecting` — в момент вызова функции `connect(address: String)` и `Connected`, после успешного обратного вызова [onConnectionStateChange](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt,%20int,%20int)). Можно, конечно, усложнить градацию состояний оповещения, скажем, добавить что-то вроде `Rescan`, `Reconnect`, но на мой взгляд это ненужно усложнит систему оповещения.

В методе `connect(address: String)` умышленно передаётся именно строковый адрес устройства. Это дополнительная проверка того, что устройство может быть вообще подключено при помощи метода [getRemoteDevice(address: String)](https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#getRemoteDevice(java.lang.String)). Однако, не следует забывать, что согласно [официальной документации](https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#getRemoteDevice(java.lang.String)), адрес надо передавать в `верхнем регистре`, иначе можем получать непонятные сообщения об ошибке. Поэтому, в код добавлена статическая функция (обратите внимание, *статическая*, а то будете искать её в объекте `bluetoothManager`...)

> Действительные аппаратные адреса Bluetooth должны быть указаны в верхнем регистре, в порядке следования байтов и в таком формате, как «00:11:22:33:AA:BB». Метод  checkBluetoothAddress(String) поможет проверить адрес Bluetooth.
> BluetoothDevice будет считать аппаратный адрес действительным, даже если адаптер [BluetoothAdapter](https://developer.android.com/reference/android/bluetooth/BluetoothAdapter) никогда не видел это устройство.

Итого, метод `connect(address:String)` просто получает объект устройства и записывает его в глобальную переменную `bluetoothDevice`

```kotlin
    fun connect(address:String) : BluetoothGatt? {
        val validAddress = address.uppercase()
        Log.d(tagLog, "connect($validAddress)")
        if (connectState == State.Disconnected) {
            connectIdling?.idling = false
            if (BluetoothAdapter.checkBluetoothAddress(validAddress)) {
                bluetoothAdapter.getRemoteDevice(validAddress)?.let { device ->
                    mutableStateFlowConnectState.tryEmit(State.Connecting)
                    bluetoothDevice = device
                    attemptReconnect = true
                    reconnectAttempts = 0
                    doConnect()
                }
            }
        }

        return null
    }
```

и если всё прошло успешно, вызовет приватный метод `doConnect()`. Он рассчитан на то, что уже получен ненулевой объект [BluetoothDevice](https://developer.android.com/reference/android/bluetooth/BluetoothDevice) и в вызове [connectGatt](https://developer.android.com/reference/android/bluetooth/BluetoothDevice#connectGatt(android.content.Context,%20boolean,%20android.bluetooth.BluetoothGattCallback)) реализует загадочное решение от [Nordic Semiconductor](https://devzone.nordicsemi.com/f/nordic-q-a/33313/android-gatt-133-error): `autoConnect = (device.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN)`

```kotlin
    @SuppressLint("MissingPermission")
    private fun doConnect() : BluetoothGatt? {
        bluetoothDevice?.let { device ->
            reconnectAttempts ++

            return device.connectGatt(
                applicationContext,
                device.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN,
                bleGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        }

        return null
    }
```

Честно говоря, можно было не реализовывать включение и выключение NOTIFICATION/INDICATION Характеристик, но раз уж есть, пусть будет. Суть в том, что штатный метод [setCharacteristicNotification](https://developer.android.com/reference/android/bluetooth/BluetoothGatt#setCharacteristicNotification(android.bluetooth.BluetoothGattCharacteristic,%20boolean)) вообще не включает ничего и не включает. Поэтому надо устанавливать правильное значение в соответствующий Дескриптор

```kotlin
val NOTIFY_DESCRIPTOR_UUID: UUID =
    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb".uppercase())
```

Соответственно, придётся для включения уведомлений записывать в `value` Дескриптора `BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE` и `BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE`.

```kotlin
    @SuppressLint("MissingPermission")
    private fun disableNotifyCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let { gatt ->
            bluetoothGattCharacteristic.getDescriptor(NOTIFY_DESCRIPTOR_UUID)
                ?.let { bluetoothGattDescriptor ->
                    gatt.setCharacteristicNotification(bluetoothGattCharacteristic, false)
                    bluetoothGattDescriptor.value =
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                    writeGattData(GattData(bluetoothGattDescriptor))
                }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifyCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let { gatt ->
            bluetoothGattCharacteristic.getDescriptor(NOTIFY_DESCRIPTOR_UUID)
                ?.let { bluetoothGattDescriptor ->
                    gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true)
                    bluetoothGattDescriptor.value =
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    writeGattData(GattData(bluetoothGattDescriptor))
                }
        }
    }
```

Соответственно, придётся хранить список устройств со включённым УВЕДОМЛЕНИЕМ/ИНДИКАЦИЕЙ в переменной

```kotlin
private val mutableListNotifiedCharacteristic = mutableListOf<BluetoothGattCharacteristic>()
```

и в момент выключения не забыть вернуть в исходное состояние все перекоряченные характеристики. Хотя... честно говоря, не думаю, что этим кто-то будет пользоваться. Этот режим работает очень медленно и при начилии буффера для обмена данными, вообще не нужен. Достаточно уведомлений о прочтении/записи характеристики/дескриптора в [BluetoothGattCallback](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback)

Буффер исходящих сообщений реализован совершенно варварски, на основе [MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/) и прицеплен к [BleGattCallback](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleGattCallback.kt), поскольку удобнее всего обрабатывать события [onCharacteristicWrite](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt,%20android.bluetooth.BluetoothGattCharacteristic,%20int))/[onDescriptorRead] сразу внутри интерфейса, а не тащить их ещё куда-то.

[OutputBuffer](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/buffer/OutputBuffer.kt) Это обычный блокирующий буффер, хранящий объект [MutableListQueue](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/buffer/MutableListQueue.kt), что чистое варварство по всем статьям.

Если не всё понятно, подробнее об Очередях, можно почитать, например в книжке [Структуры данных и алгоритмы](https://www.raywenderlich.com/books/data-structures-algorithms-in-kotlin/v1.0/chapters/5-queues) очень спокойное объяснение для начинашек.

Если список исходящих сообщений в буфере пуст, данные сразу отправляются на запись, если очередь не пуста, значение добавляется в очередь. Никаких решений, скажем, с BackPressure, пока не сделано, но добавить такую возможность довольно просто.

```kotlin
    fun writeGattData(gattData: GattData) {
        buffer.enqueue(gattData)
        if (buffer.count == 1) {
            writeNextGattData(gattData)
        }
    }
```

Соответственно, когда приходит уведомление [onCharacteristicWrite](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt,%20android.bluetooth.BluetoothGattCharacteristic,%20int))/[onDescriptorWrite](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback#onDescriptorWrite(android.bluetooth.BluetoothGatt,%20android.bluetooth.BluetoothGattDescriptor,%20int)) и `status == BluetoothGatt.GATT_SUCCESS`, данные изымаются из очереди и считаются записанными

```kotlin
    private fun dequeueAndWriteNextGattData(gattData: GattData) {
        if (buffer.peek() == gattData) {
            buffer.dequeue()
            buffer.peek()?.let { nextGattData ->
                writeNextGattData(nextGattData)
            }
        }
    }
```

Данные в очереди обёрнуты в самопальный класс [BleGattItem](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/data/BleGattItem.kt):

```kotlin
data class BleGattItem (val uuidService: UUID,
                        val uuidCharacteristic: UUID? = null,
                        val uuidDescriptor: UUID? = null,
                        val value:ByteArray? = null,
) {
    // ...
    constructor(bluetoothGattDescriptor: BluetoothGattDescriptor) :
            this(uuidService = bluetoothGattDescriptor.characteristic.service.uuid,
                uuidCharacteristic = bluetoothGattDescriptor.characteristic.uuid,
                uuidDescriptor = bluetoothGattDescriptor.uuid,
                value = bluetoothGattDescriptor.value,
            )
    // ...
    fun getDescriptor(bluetoothGatt: BluetoothGatt) : BluetoothGattDescriptor? =
        if (uuidCharacteristic == null && uuidDescriptor == null) {
            null
        } else {
            bluetoothGatt.getService(uuidService)?.let { service ->
                service.getCharacteristic(uuidCharacteristic)?.let { characteristic ->
                    characteristic.getDescriptor(uuidDescriptor)
                }
            }
        }
        // ...
```

За счёт дополнительных конструкторов можно инициализировать данные из объектов [BluetoothGattService](https://developer.android.com/reference/android/bluetooth/BluetoothGattService), [BluetoothGattCharacteristic](https://developer.android.com/reference/android/bluetooth/BluetoothGattCharacteristic), [BluetoothGattDescriptor](https://developer.android.com/reference/android/bluetooth/BluetoothGattDescriptor). Соответственно, сделано три функции для получения Сервиса, Характеристики и Дескриптора, что несложно при наличии объекта [BluetoothGatt](https://developer.android.com/reference/android/bluetooth/BluetoothGatt). Объекты [BluetoothGattService](https://developer.android.com/reference/android/bluetooth/BluetoothGattService), [BluetoothGattCharacteristic](https://developer.android.com/reference/android/bluetooth/BluetoothGattCharacteristic), [BluetoothGattDescriptor](https://developer.android.com/reference/android/bluetooth/BluetoothGattDescriptor),как ни странно прекрасно генерируются без всякого «мокания». Подменный объект [BleGattItem](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/data/BleGattItem.kt) сделан не из отладочных соображений, а для того, чтобы не использовать в очереди разнородные объекты и, упаси Вселенная, тип `Any`. Отладка с таким типом — тот ещё ад.

Так, что запись очередного значения из очереди выглядит очень просто:

```kotlin
    @SuppressLint("MissingPermission")
    private fun writeNextCharacteristic(bleGattItem: BleGattItem) : Boolean {
        bluetoothGatt?.let { gatt ->
            bleGattItem.getCharacteristic(gatt)?.let { characteristic ->
                characteristic.value = bleGattItem.value
                return gatt.writeCharacteristic(characteristic)
            }
        }

        return false
    }

    @SuppressLint("MissingPermission")
    private fun writeNextDescriptor(bleGattItem: BleGattItem) : Boolean {
        bluetoothGatt?.let { gatt ->
            bleGattItem.getDescriptor(gatt)?.let { descriptor ->
                descriptor.value = bleGattItem.value
                return gatt.writeDescriptor(descriptor)
            }
        }
        // ...
        return true
    }
```

Остаётся дождаться уведомления [onCharacteristicWrite](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt,%20android.bluetooth.BluetoothGattCharacteristic,%20int))/[onDescriptorWrite](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback#onDescriptorWrite(android.bluetooth.BluetoothGatt,%20android.bluetooth.BluetoothGattDescriptor,%20int)) от наследника [BluetoothGattCallback](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback) и убрать соответствующее значение из очереди.

## Юнит-тестирование [BleGattManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleGattManager.kt)

Пока не реализовано

## Менеджер сопряжения BLE-устройств [BleBondManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleBondManager.kt)

В основе — класс обработки широковещательных событий [BcBondReceiver](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/receivers/BcBondReceiver.kt). Он наследуется от [BroadcastReceiver](https://developer.android.com/reference/android/content/BroadcastReceiver) и перехватывает событие сопряжения устройства и инициализировать повторное подключение. Проблема в том, что событие `BluetoothDevice.ACTION_BOND_STATE_CHANGED` генерируется при любом подключении к устройству. Поэтому, сначала надо перехватить запрос `BluetoothDevice.ACTION_PAIRING_REQUEST`, а потом сравнить адрес устройства в запросе на сопряжение, и при совпадении сформировать событие «Устройство сопряжено».

```kotlin
    override fun onReceive(context: Context?, intent: Intent?) {
        if ( context != null && intent != null ) {
            when (intent.action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bondState: Int = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                    val previousBondState: Int =
                        intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                    val bluetoothDevice: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    // Log.d(TAG, "ACTION_BOND_STATE_CHANGED(${device?.address}): $previousBondState => $bondState")
                    bleBondManager.onSetBondingDevice(bluetoothDevice, previousBondState, bondState)
                }
                else -> {

                }
            }
        }
    }
```

Осталось в классе [BleBondManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleBondManager.kt) зарегистрировать получатель:

```kotlin
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        applicationContext.applicationContext.registerReceiver(bcBondReceiver,
            makeIntentFilter())
    }

    override fun onDestroy(owner: LifecycleOwner) {
        applicationContext.unregisterReceiver(bcBondReceiver)
        super.onDestroy(owner)
    }

    private fun makeIntentFilter() = IntentFilter().let { intentFilter ->
        intentFilter.addAction(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)

        intentFilter
    }
```

вызвать метод `bondRequest(address: String)` и следить за событиями:

```kotlin
    /**
     * BluetoothDevice.BOND_NONE    10
     * BluetoothDevice.BOND_BONDING 11
     * BluetoothDevice.BOND_BONDED  12
     */
    fun onSetBondingDevice(bluetoothDevice: BluetoothDevice?, oldState: Int, newState: Int) {
        bluetoothDevice?.let { device ->
            if (device == requestDevice) {
                Log.d(logTag, "onSetBondingDevice($bluetoothDevice, $oldState, $newState)")
                when(newState) {
                    BluetoothDevice.BOND_BONDING -> { mutableStateFlowBleBondState
                        .tryEmit(BleBondState(BleDevice(bluetoothDevice), State.Bonding)) }
                    BluetoothDevice.BOND_BONDED -> { mutableStateFlowBleBondState
                        .tryEmit(BleBondState(BleDevice(bluetoothDevice), State.Bonded)) }
                    BluetoothDevice.BOND_NONE -> { mutableStateFlowBleBondState
                        .tryEmit(BleBondState(BleDevice(bluetoothDevice), State.Reject)) }
                    else -> { Log.d(logTag, "Unknown State: $newState")}
                }
            }
        }
    }
```

К сожалению, такое сопряжение не будет работать на ряде устройств Samsung из-за [Knox](https://developer.samsung.com/knox). Как это обойти, пока, увы, не знаю.

## Юнит-тестирование [BleBondManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleBondManager.kt)

В классе [BleBondManagerTest](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/test/java/com/grandfatherpikhto/blin/BleBondManagerTest.kt) снова ничего сложного, однако, надо не забыть: этот класс — наследник [DefaultLifecycleObserver](https://developer.android.com/reference/androidx/lifecycle/DefaultLifecycleObserver), а значит, надо «замокать» основные события [DefaultLifecycleObserver.onCreate()](https://developer.android.com/reference/androidx/lifecycle/DefaultLifecycleObserver#onCreate(androidx.lifecycle.LifecycleOwner)) и [DefaultLifecycleObserver.onDestroy()](https://developer.android.com/reference/androidx/lifecycle/DefaultLifecycleObserver#onDestroy(androidx.lifecycle.LifecycleOwner))

Значит, надо «замокать» жизненный цикл. Сделать это очень просто при помощи [Robolectric](http://robolectric.org/androidx_test/).

```kotlin
    private val controllerActivity = Robolectric.buildActivity(AppCompatActivity::class.java)
        .create()
        .start()

    private val appCompatActivity = controllerActivity.get()
```

Проще говоря, создана пустая активность (Основной класс — [AppCompatActivity](https://developer.android.com/reference/androidx/appcompat/app/AppCompatActivity) и сразу переведена в состояния `create()` и `start()`. После этого зовём геттер `get()` и получаем `appCompatActivity`

Теперь, можно добавить `bleBondManager` к жизненному циклу:

```kotlin
    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        appCompatActivity.lifecycle.addObserver(bleBondManager)
    }
```

На всякий случай, оставлен `closeable = MockitoAnnotations.openMocks(this)`, чтобы можно было использовать аннотирование [Mockito](https://site.mockito.org/).

Теперь, есть возможность провести самое «глубинное» тестирование, которого в «чистом» [Mockito](https://site.mockito.org/) или [MockK](https://mockk.io) добиться довольно... громоздко. Т.е., можно отправить интенцию (Намерение [Intent](https://developer.android.com/reference/android/content/Intent)), принять её в [BroadcastReceiver](https://developer.android.com/reference/android/content/BroadcastReceiver) и отследить всю цепочку до подтверждения или отказа от сопряжения устройства:


```kotlin
    private fun putIntentDevice(bluetoothDevice: BluetoothDevice, newBondState: Int = BluetoothDevice.BOND_BONDED) =
        applicationContext.sendBroadcast(Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED).let {
            it.putExtra(BluetoothDevice.EXTRA_DEVICE, bluetoothDevice)
            it.putExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE)
            it.putExtra(BluetoothDevice.EXTRA_BOND_STATE, newBondState)
            it
        })
```

Осталось создать «Тень» отправляемого [BluetoothDevice](https://developer.android.com/reference/android/bluetooth/BluetoothDevice)

```kotlin
    private fun shadowBluetoothDevice(address: String, name: String, bondState: Int = BluetoothDevice.BOND_NONE) : BluetoothDevice =
        bluetoothAdapter.getRemoteDevice(address).let { bluetoothDevice ->
            shadowOf(bluetoothDevice).setBondState(bondState)
            shadowOf(bluetoothDevice).setName(name)
            return bluetoothDevice
        }
```

В тесте важно не забыть штатного роболектриковскго «ждуна». Если этого не сделать, событие просто не успеет произойти и Вы получите совсем не тот результат, которого ожидаете!

Учитывая, что внутри библиотеки обмен данными происходит с помощью упрощённых классов устройства — [BleDevice](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/data/BleDevice.kt) и для передачи оповещения используется специальный дата-класс [BleBondState(val bleDevice:BleDevice), state: BleBondManager.State](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/data/BleBondState.kt), проверка будет выглядеть так:

```kotlin
    @Test
    fun bondDevice() = runTest(dispatcher) {
        // Буквы адреса должны быть в ВЕРХНЕМ регистре
        val address = randomBluetoothAddress
        val bluetoothDevice = shadowBluetoothDevice(address, NAME)
        // Метод [createBond()] вернёт true
        shadowOf(bluetoothDevice).setCreatedBond(true)
        // Вызываем запрос на сопряжение в штатном режиме
        bleBondManager.bondRequest(address)
        assertEquals(BleBondState(BleDevice(bluetoothDevice),
            BleBondManager.State.Request), bleBondManager.bondState)
        // Оговариваем, что устройство теперь относится к списку сопряжённых.
        shadowAdapter.setBondedDevices(mutableSetOf(bluetoothDevice))
        shadowOf(bluetoothDevice).setBondState(BluetoothDevice.BOND_BONDED)
        // Отправляем Намерение с BluetoothDevice и кодом BOND_BONDED
        putIntentDevice(bluetoothDevice)
        // Включить штатного роболектривского «ждуна»!
        ShadowLooper.shadowMainLooper().idle()
        assertEquals(BleBondState(BleDevice(bluetoothDevice), BleBondManager.State.Bonded),
            bleBondManager.bondState)
    }
```

## Наконец, последний класс — [BleManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleManager.kt)

Это просто обёртка для потоков, данных, методов всех трёх основных менеджеров [BleScannManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleScanManager.kt), [BleGattManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleGattManager.kt), [BleBondManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleBondManager.kt)

[BleManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleManager.kt) наследуютеся от [BleManagerInterface](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleManagerInterface.kt). Это нужно, чтобы в app (приложении) можно было создать свой фейковый объект, скажем [FakeBleManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/app/src/main/java/com/grandfatherpikhto/blescan/fake/FakeBleManager.kt) и запустить с его помощью управляемый инструментальный тест. Он крайне тупо состоит из одних геттеров и присваиваний.
А больше здесь особо ничего и не нужно:

```kotlin
class BleManager constructor(private val context: Context,
                             dispatcher: CoroutineDispatcher = Dispatchers.IO)
    : BleManagerInterface {

    private val logTag = this.javaClass.simpleName
    private val scope = CoroutineScope(dispatcher)

    val applicationContext:Context get() = context.applicationContext

    val bleScanManager: BleScanManager = BleScanManager(context, dispatcher)
    val bleGattManager: BleGattManager = BleGattManager(context, bleScanManager, dispatcher)
    val bleBondManager: BleBondManager = BleBondManager(context, dispatcher)

    override val stateFlowScanState get() = bleScanManager.stateFlowScanState
    override val scanState get()     = bleScanManager.scanState

    override val sharedFlowBleScanResult get() = bleScanManager.sharedFlowBleScanResult

    override val scanResults get() = bleScanManager.scanResults.map { BleScanResult(it) }

    // ...

    override
    fun startScan(addresses: List<String>,
                  names: List<String>,
                  services: List<String>,
                  stopOnFind: Boolean,
                  filterRepeatable: Boolean,
                  stopTimeout: Long
    ) : Boolean = bleScanManager.startScan( addresses, names, services,
        stopOnFind, filterRepeatable, stopTimeout )

    override fun stopScan() = bleScanManager.stopScan()

    override fun connect(address: String): BleGatt? {
        bleGattManager.connect(address)?.let {
            return BleGatt(it)
        }

        return null
    }

    override fun disconnect() = bleGattManager.disconnect()

    // ...
 }
```

`Сервис взаимодействия с BLE устройством создан`

## Инструментальное тестирование [BleScanManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/BleScanManager.kt)

А вот здесь всё становится неопрятным и громоздким. Потому, что увы, даже [MockK](https://mockk.io) стрёмно съехал с темы на ошибке [Unable to dlopen libmockkjvmtiagent.so: dlopen failed: library "libmockkjvmtiagent.so" not found](https://github.com/mockk/mockk/issues/819). Хотя, в [Официальной Документации](https://mockk.io/ANDROID.html) утверждается, что всё прекрасно должно работать... не работает, зараза.

Проверить «чистый» [Dexopener](https://github.com/tmurakami/dexopener) пока руки не дошли.

Так или иначе, придётся сооружать громоздкий, но зато управляемый [FakeBleManager](https://github.com/GrandFatherPikhto/BLEScan/blob/master/app/src/main/java/com/grandfatherpikhto/blescan/fake/FakeBleManager.kt), который и будет генерировать нужные события и передавать данные в проект для проверки работы UI. Впрочем, кроме громоздкости в нём ничего сложного нет и чудить можно сколько угодно. Тем более, что в процессе обмена «нативные» объекты, такие, как [ScanResult](https://developer.android.com/reference/android/bluetooth/le/ScanResult), [BluetoothDevice](https://developer.android.com/reference/android/bluetooth/BluetoothDevice), [BluetoothGatt](https://developer.android.com/reference/android/bluetooth/BluetoothGatt), заменены на обёртки данных, такие, как [BleScanResult](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/data/BleScanResult.kt), [BleGatt](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/data/BleGatt.kt), [BleGattItem](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/data/BleGattItem.kt), [BleDevice](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/data/BleDevice.kt). Конечно, это данные для бедных и, если что, придётся лезть в библиотеку, добавлять поля, сурово править код на всех уровнях... но зато в тестировании можно сколько угодно «фейкать», «мокать» и «стабить».

Кроме того, в библиотеку пришлось добавить трёх «Ждунов» — [ConnectingIdling](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/idling/ConnectingIdling.kt), [DisconnectingIdling](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/idling/DisconnectingIdling.kt), [ScanIdling](https://github.com/GrandFatherPikhto/BLEScan/blob/master/blin/src/main/java/com/grandfatherpikhto/blin/idling/ScanIdling.kt). Они нужны для того, чтобы не давать приложению совершать определённые шаги до тех пор пока фейковое сканирование, подключени, отключение не будут завершены. (См. [Ресурсы для работы с Espresso на холостом ходу](https://developer.android.com/training/testing/espresso/idling-resource))

Осталось подменить в основном классе приложения

### UI

#### [MainActivity](https://github.com/GrandFatherPikhto/BLEScan/blob/master/app/src/main/java/com/grandfatherpikhto/blescan/ui/MainActivity.kt)

Здесь происходит запрос на привязывание/отвязывание сервиса, запрос необходимых разрешений, запрос на включение/выключение адаптера Bluetooth. Здесь же происходит навигация по фрагментам: [ScanFragment](https://github.com/GrandFatherPikhto/BLEScan/blob/master/app/src/main/java/com/grandfatherpikhto/blescan/ui/fragments/ScanFragment.kt) и [DeviceFragment](https://github.com/GrandFatherPikhto/BLEScan/blob/master/app/src/main/java/com/grandfatherpikhto/blescan/ui/fragments/DeviceFragment.kt).

Начнём с запроса разрешений на доступ к сканированию, подключению, сопряжению и обмен данными с Bluetooth:

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

На первых порах, некое изумление вызвает необходимость давать разрешение на локацию. Но это ничего, со временем привыкаешь и начинаешь более ли менее понимать логику системы. Запрос на доступ к локации, относится к уровню `dangerous` и требует программного запроса на доступ.

Запросы уровня `dangerous`: [android.permission.ACCESS_FINE_LOCATION](https://developer.android.com/reference/android/Manifest.permission#ACCESS_FINE_LOCATION), [android.permission.ACCESS_COARSE_LOCATION](https://developer.android.com/reference/android/Manifest.permission#ACCESS_COARSE_LOCATION), [android.permission.ACCESS_BACKGROUND_LOCATION](https://developer.android.com/reference/android/Manifest.permission#ACCESS_COARSE_LOCATION) нужно запрашивать напрямую, скажем, из `Активности`. Причём, ланчеры запросов надо создать до формирования самой `Активности`, иначе мы сможем запрашивать разрешения или запрос на включение/выключение Bluetooth только в процессе запуска приложения.

```kotlin
    /**
     * Запрос группы разрешений
     * Ланчер необходимо вынести в глобальные переменные, потому что
     * он должен быть инициализирован ДО запуска Активности.
     * В противном случае, будет ошибка запроса, если мы вздумаем
     * перезапросить разрешения после запуска полного запуска приложения
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
     * Тоже самое: ланчер надо вынести в глобальные переменные,
     * чтобы он инициализировался ДО запуска Активности.
     * Иначе, после старта виджета перезапросить включение Блютуз
     * уже не получится
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
```

Теперь можно сделать пару функций для запросов разрешений и на включение Bluetooth.

```kotlin
    /**
     * Запрос на включение Bluetooth
     */
    private fun requestEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        bluetoothLauncher.launch(enableBtIntent)
    }

    /**
     * Запрос группы разрешений
     */
    private fun requestPermissions(permissions: Array<String>) {
        val launchPermissions:MutableList<String> = mutableListOf<String>()

        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mainActivityModel.andReady(true)
            } else {
                launchPermissions.add(permission)
            }
        }

        if(launchPermissions.isNotEmpty()) {
            permissionsLauncher.launch(launchPermissions.toTypedArray())
        }
    }
```

Например, можно запрашивать разрешения на доступ при запуске приложения, а запрос на включение/выключение адаптера Bluetooth, «повесить» на опцию главного меню.

Привязывание сервисов так же находится здесь, чтобы согласовать жизненный цикл активности и работы сервиса. Строго говоря, этого можно и не делать и перенести привязывание сервиса в класс наследованный от [Application](https://developer.android.com/reference/android/app/Application) [BleScanApp](./app/src/main/java/com/grandfatherpikhto/blescan/BleScanApp.kt), или вовсе отказаться от сервиса, как от ненужного костыля.

Чтобы [BleScanApp](./app/src/main/java/com/grandfatherpikhto/blescan/BleScanApp.kt) создавался, нужно указать его в [blin/AndroidManifext.xml](./app/src/main/AndroidManifest.xml)


```xml
<application>
    <service android:name=".service.BtLeService" android:enabled="true" />
</application>
```

В этой реализации сервис привязан к жизненному циклу [MainActivity](https://github.com/GrandFatherPikhto/BLEScan/blob/master/app/src/main/java/com/grandfatherpikhto/blescan/ui/MainActivity.kt) и отвязывается c отключением от устройства при каждом повороте экрана или переходе приложения в фоновый режим:

```kotlin
    /**
     * Событие жизненного цикла Activity() onPause()
     */
    override fun onPause() {
        super.onPause()
        unbindService(btLeServiceConnector)
    }

    /**
     * Событие жизненного цикла Activity() onResume()
     */
    override fun onResume() {
        super.onResume()
        Intent(this, BtLeService::class.java).also { intent ->
            bindService(intent, btLeServiceConnector, Context.BIND_AUTO_CREATE)
        }
    }
```

#### Навигация фрагментов

В этом примере навигация сделана довольно грубо. В [MainActivity](https://github.com/GrandFatherPikhto/BLEScan/blob/master/app/src/main/java/com/grandfatherpikhto/blescan/ui/MainActivity.kt)
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

Всё, что остаётся — просто менять запись на нужное значение и, соответственно, переключаться между
[ScanFragment](https://github.com/GrandFatherPikhto/BLEScan/blob/master/app/src/main/java/com/grandfatherpikhto/blescan/ui/fragments/ScanFragment.kt) и
[DeviceFragment](https://github.com/GrandFatherPikhto/BLEScan/blob/master/app/src/main/java/com/grandfatherpikhto/blescan/ui/fragments/DeviceFragment.kt). Текущий фрагмент хранится в модели, так что при повороте экрана или уходе приложения в фоновый режим, будет восстанавливаться выбранный фрагмент.

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

```text
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
[findNavController](https://developer.android.com/reference/androidx/navigation/Navigation#findNavController(android.app.Activity,kotlin.Int))

```kotlin
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
```

Работать уже не будет. Так, что его надо заменить на обращение к
[supportFragmentManager](https://developer.android.com/reference/androidx/fragment/app/FragmentActivity#getSupportFragmentManager())
(см. [Navigation](https://developer.android.com/guide/navigation)

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

### [ScanFragment](https://github.com/GrandFatherPikhto/BLEScan/blob/master/app/src/main/java/com/grandfatherpikhto/blescan/ui/fragments/ScanFragment.kt)

«Умолчальный» фрагмент, с которого начинается запуск приложения (Home в nav_graph).

Запускается первым. Контейнер для списка найденных устройств. Использует [RecycleView](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView), в простейшем варианте. Для его работы создан небольшой адаптер [RvBtAdapter](https://github.com/GrandFatherPikhto/BLEScan/blob/master/app/src/main/java/com/grandfatherpikhto/blescan/adapter/RvBtAdapter.kt). Адаптер сделан очень просто, буквально по [официальному руководству](https://developer.android.com/guide/topics/ui/layout/recyclerview). Так, что подробно описывать его здесь не будем.

Может быть, не стоило валить в одну кучу все данные сканирования устройств и подключения в одну модель, но данный пример невелик, поэтому, данные сканирования и подключения находятся в [BtLeModel](./app/src/main/java/com/grandfatherpikhto/blescan/model/BtLeModel.kt)

Устройства хранятся в списке, причём, по каждому новому устройству идёт сигнал обновления всего списка. Это не красиво, но больше вряд ли в списке будет больше 20 устройств, поэтому, в модели сделано так:

```kotlin
        override fun onFindDevice(btLeDevice: BtLeDevice?) {
            super.onFindDevice(btLeDevice)
            _device.postValue(btLeDevice)
            btLeDevice?.let { found ->
                devicesList.add(found)
                _devices.postValue(devicesList)
            }
        }
```

В свою очередь, [ScanFragment](https://github.com/GrandFatherPikhto/BLEScan/blob/master/app/src/main/java/com/grandfatherpikhto/blescan/ui/fragments/ScanFragment.kt) просто отслеживает содержимое списка:

```kotlin
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
            btLeModel.bond.observe(viewLifecycleOwner, { isBond ->
                // btLeScanService = BtLeScanServiceConnector.service
                // btLeScanService?.scanLeDevices(name = AppConst.DEFAULT_NAME)
            })
        }
    }
```

Состояния — сканирование, остановка сканирования, получение списка сопряжённых устройств, так же хранятся в [BtLeModel](./app/src/main/java/com/grandfatherpikhto/blescan/model/BtLeModel.kt).

```kotlin
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
```

Короткий клик по плашке найденного устройства останавливает текущее сканирование и запускает повторное сканирование с фильтром по адресу устройства. Сделано просто так, для проверки работы фильтра.

```kotlin
    private fun initRvAdapter() {
        rvBtAdapter.setOnItemClickListener(object : RvItemClick<BtLeDevice> {
            override fun onItemClick(model: BtLeDevice, view: View) {
                Toast.makeText(
                    requireContext(),
                    "Сканируем адрес ${model.address}",
                    Toast.LENGTH_LONG).show()
                bluetoothInterface.stopScan()
                btLeModel.clean()
                bluetoothInterface.leScanDevices(addresses = model.address, mode = BtLeScanner.Mode.StopOnFind)
            }

            override fun onItemLongClick(model: BtLeDevice, view: View) {
                Toast.makeText(
                    requireContext(),
                    "Подключаемся к ${model.address}",
                    Toast.LENGTH_LONG).show()
                connectToBluetoothDevice(model)
            }
        })

        bindRvAdapter()
    }
```

Длительный клик — попытка подключения к устройству. Это передаётся в главную модель [MainActivityModel](./app/src/main/java/com/grandfatherpikhto/blescan/model/MainActivityModel.kt). Поскольку, она общая для Главной Активности и всех фрагментов. Экземпляр модели Главной Активности вызывается при помощи `private val mainActivityModel:MainActivityModel by activityViewModels()`, а значит, это — синглетон

```kotlin
    private fun connectToBluetoothDevice(model: BtLeDevice) {
        mainActivityModel.changeDevice(model)
        mainActivityModel.changeCurrent(MainActivity.Current.Device)
    }
```

Главная Активность следит за `current` и переключает текующий фрагмент на [DeviceFragment](https://github.com/GrandFatherPikhto/BLEScan/blob/master/app/src/main/java/com/grandfatherpikhto/blescan/ui/fragments/DeviceFragment.kt)

Длительное нажатие на плашку найденного устройства активирует попытку подключения к устройству.

### [DeviceFragment](https://github.com/GrandFatherPikhto/BLEScan/blob/master/app/src/main/java/com/grandfatherpikhto/blescan/ui/fragments/DeviceFragment.kt)

Фактически, это тоже контейнер для [RecycleView](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView) с адаптером [RvGattAdapter](./app/src/main/java/com/grandfatherpikhto/blescan/adapter/RvGattAdapter.kt). После исследования `GATT`, прокручивается простой цикл:

```kotlin
        btLeModel.gatt.observe(viewLifecycleOwner, { gatt ->
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
```

и список заполняется значениями сервисов, характеристик и дескрипторов. Чтобы не усложнять работу списка здесь не реализована псевдодревовидная структура отображения, хотя это не так уж сложно и для этого вовсе не надо реализовывать вложенные списки. Достаточно просто перехватить вызов

```kotlin
    override fun getItemViewType(position: Int): Int {
        // return super.getItemViewType(position)
        return profile[position].first.value
    }
```

и в `bind` привязывать разные плашки, с разным отступом и разным содержимым. Впрочем, Вы это можете реализовать самостоятельно.

## Материалы

1. [Все работы Мартейна Ван Велле](https://medium.com/@martijn.van.welie) Самое толковое и подробное описание работы с Bluetooth BLE, с кучей ссылок на различные источники.
   Подробно о сканировании устройств. Почему-то не отражена проблема сканирования устройств с фильтрами.
2. [Making Android BLE work — part 1 // Martijn van Welie](https://medium.com/@martijn.van.welie/making-android-ble-work-part-1-a736dcd53b02?source=user_profile---------3-------------------------------) Часть 1. Как заставить Android BLE работать - часть 1
3. [Making Android BLE work — part 2 // Martijn van Welie](https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07?source=user_profile---------2-------------------------------) Часть 2. Подключение, отключение, исследование сервисов
4. [Making Android BLE work — part 3 // Martijn van Welie](https://medium.com/@martijn.van.welie/making-android-ble-work-part-3-117d3a8aee23?source=user_profile---------1-------------------------------) Часть 3. чтение/запись характеристик; включение/выключение уведомлений
5. [Making Android BLE work — part 4 // Martijn van Welie](https://medium.com/@martijn.van.welie/making-android-ble-work-part-4-72a0b85cb442?source=user_profile---------0-------------------------------) Часть 4. Сопряжение с устройствами
6. [Перевод статьи Мартейна ван Велле. Часть 1.](https://habr.com/ru/post/536392/)  Сканирование
7. [Перевод статьи Мартейна ван Велле. Часть 2.](https://habr.com/ru/post/537526/)  Подключение/Отключение
8. [Перевод статьи Мартейна ван Велле. Часть 3.](https://habr.com/ru/post/538768/) Чтение/Запись характеристик
9. [Перевод статьи Мартейна ван Велле. Часть 4.](https://habr.com/ru/post/539740/) Сопряжение устройств
10. [BLESSED](https://github.com/weliem/blessed-android) A very compact Bluetooth Low Energy (BLE) library for Android 5 and higher, that makes working with BLE on Android very easy.
11. [BLESSED](https://github.com/weliem/blessed-android) A very compact Bluetooth Low Energy (BLE) library for Android 8 and higher, that makes working with BLE on Android very easy. It is powered by Kotlin's Coroutines and turns asynchronous GATT methods into synchronous methods! It is based on the Blessed Java library and has been rewritten in Kotlin using Coroutines.
12. [(Talk) Bluetooth Low Energy On Android // Stuart Kent](https://www.stkent.com/2017/09/18/ble-on-android.html) (Обсуждение) Bluetooth Low Energy на Android // Стюарт Кент //
13. [Gist by Stuart Kent to Android BLE Talk](https://gist.github.com/stkent/a7f0d6b868e805da326b112d60a9f59b) Огромное количество ссылок на разные ресурсы
14. [The Ultimate Guide to Android Bluetooth Low Energy](https://punchthrough.com/android-ble-guide/) Дельный и короткий гайд по работе со стеком BLE    
15. [Android BLE Library](https://github.com/NordicSemiconductor/Android-BLE-Library) Пожалуй, единственная Android библиотека, которая реально решает множество проблем Android с низким энергопотреблением Bluetooth и действительно нормально работает.
16. [Samsung Bluetooth Knox API](https://docs.samsungknox.com/dev/knox-sdk/bluetooth-support.htm) Работа с BLE на Samsung
17. [Samsung API](https://developer.samsung.com/smarttv/develop/api-references/tizen-web-device-api-references/systeminfo-api/getting-device-capabilities-using-systeminfo-api.html)
18. [Android BLE Issues](https://sweetblue.io/docs/Android-BLE-Issues) This is a short list of issues you will encounter if you try to use the native Android BLE stack directly // Краткий список проблем, с которыми вы столкнетесь, если попытаетесь напрямую использовать собственный стек Android BLE
19. [NordicSemiconductor - BLE Issues](https://github.com/NordicSemiconductor/Android-Ble-library/issues) Список проблем работы с BLE на GitHub
20. [Google: Fix Bluetooth problems on Android](https://support.google.com/android/answer/9769184?hl=en) Список проблем работы с Bluetooth от Google
21. [Android BLE Issues - SweetBlue](https://github.com/iDevicesInc/SweetBlue/wiki/Android-BLE-Issues) Ещё один, немного устаревший список проблем работы со стеком BLE
22. [Android BLE scan with filter issue](https://stackoverflow.com/questions/34065210/android-ble-device-scan-with-filter-is-not-working/34092300) Проблемы сканирования с фильтром. Похоже, до сих пор не исправлены
23. [We’ll prevent applications from starting and stopping scans more than 5 times in 30 second](https://github.com/NordicSemiconductor/Android-Scanner-Compat-Library/issues/18)
24. [Описание Bluetooth](https://ru.wikipedia.org/wiki/Bluetooth) Подробная статья о Bluetooth на Википедии.
25. [Bluetooth specifications](https://www.bluetooth.com/specifications/specs/) Спецификации Bluetooth.
26. [BLE Android official guide](https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview) Официальное руководство по работе с BLE.
27. [Find BLE Devices](https://developer.android.com/guide/topics/connectivity/bluetooth/find-ble-devices) Официальное руководство по работе с BLE. Сканирование.
28. [Connect GATT Server](https://developer.android.com/guide/topics/connectivity/bluetooth/connect-gatt-server) Подключение к серверу GATT.
29. [Transver BLE Data](https://developer.android.com/guide/topics/connectivity/bluetooth/transfer-ble-data) Передача/Приём данных через GATT.
30. [Android connectivity samples](https://github.com/android/connectivity-samples) Официальный набор отдельных проектов Android Studio, которые помогут вам приступить к написанию приложений Connectivity на Android.
31. [Android BLE Library](https://github.com/NordicSemiconductor/Android-BLE-Library) NordicSemiconductor Android BLE Library // Самая надёжная и быстрая библиотека стека BLE
32. [Android BluetoothLeGatt Sample](https://github.com/android/connectivity-samples/tree/master/BluetoothLeGatt) В этом примере показано, как использовать общий профиль атрибутов Bluetooth LE (GATT) для передачи произвольных данных между устройствами.
