#Простейший сканнер BLE
## Материалы
Полный список документации по BLE Android
1.  (https://medium.com/@martijn.van.welie)[Все работы Мартина Ван Велле]
2.  (https://medium.com/@martijn.van.welie/making-android-ble-work-part-1-a736dcd53b02?source=user_profile---------3-------------------------------)[Часть 1. Как заставить Android BLE работать - часть 1] // Making Android BLE work — part 1 // Martijn van Welie
3.  (https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07?source=user_profile---------2-------------------------------)[Часть 2. Подключение, отключение, исследование сервисов]
4.  (https://medium.com/@martijn.van.welie/making-android-ble-work-part-3-117d3a8aee23?source=user_profile---------1-------------------------------)[Часть 3. чтение/запись характеристик; включение/выключение уведомлений]
5.  (https://medium.com/@martijn.van.welie/making-android-ble-work-part-4-72a0b85cb442?source=user_profile---------0-------------------------------)[Часть 4. Сопряжение с устройствами]
6.  (https://habr.com/ru/post/536392/)[Перевод работы Мартина Велле. Часть 1. Сканирование]
7.  (https://habr.com/ru/post/537526/)[Перевод работы Мартина Велле. Часть 2. Подключение/Отключение]
8.  (https://habr.com/ru/post/538768/)[Перевод работы Мартина Велле. Часть 3. Чтение/Запись]
9.  (https://habr.com/ru/post/539740/)[Перевод работы Мартина Велле. Часть 4. Сопряжение устройств]
10. (https://github.com/weliem/blessed-android)[BLESSED is a very compact Bluetooth Low Energy (BLE) library] for Android 5 and higher, that makes working with BLE on Android very easy.
11. https://github.com/weliem/blessed-android-coroutines // BLESSED is a very compact Bluetooth Low Energy (BLE) library for Android 8 and higher, that makes working with BLE on Android very easy. It is powered by Kotlin's Coroutines and turns asynchronous GATT methods into synchronous methods! It is based on the Blessed Java library and has been rewritten in Kotlin using Coroutines.
12. https://www.stkent.com/2017/09/18/ble-on-android.html (Обсуждение) Bluetooth Low Energy на Android // Стюарт Кент // (Talk) Bluetooth Low Energy On Android // Stuart Kent
13. https://gist.github.com/stkent/a7f0d6b868e805da326b112d60a9f59b Gist by Stuart Kent to Android BLE Talk. Огромное количество ссылок на разные ресурсы.
14. https://github.com/NordicSemiconductor/Android-BLE-Library/ Пожалуй, единственная Android библиотека, которая реально решает множество проблем Android с низким энергопотреблением Bluetooth и действительно нормально работает.
15. https://docs.samsungknox.com/dev/knox-sdk/bluetooth-support.htm Samsung Bluetooth Knox API
16. https://developer.samsung.com/smarttv/develop/api-references/tizen-web-device-api-references/systeminfo-api/getting-device-capabilities-using-systeminfo-api.html Samsung API
17. https://sweetblue.io/docs/Android-BLE-Issues // Краткий список проблем, с которыми вы столкнетесь, если попытаетесь напрямую использовать собственный стек Android BLE // This is a short list of issues you will encounter if you try to use the native Android BLE stack directly
18.  https://github.com/NordicSemiconductor/Android-Ble-library/issues // То же.
19. https://github.com/iDevicesInc/SweetBlue/wiki/Android-BLE-Issues // Android BLE Issues
20. https://stackoverflow.com/questions/34065210/android-ble-device-scan-with-filter-is-not-working/34092300 Проблемы сканирования с фильтром
21. https://ru.wikipedia.org/wiki/Bluetooth Описание Bluetooth
22. https://www.bluetooth.com/specifications/specs/ Bluetooth specifications
23. https://support.google.com/android/answer/9769184?hl=en // Fix Bluetooth problems on Android
24. https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview BLE Android official guide
25. https://developer.android.com/guide/topics/connectivity/bluetooth/find-ble-devices Find BLE Devices
26. https://developer.android.com/guide/topics/connectivity/bluetooth/connect-gatt-server Connect GATT Server
27. https://developer.android.com/guide/topics/connectivity/bluetooth/transfer-ble-data Transver BLE Data
<dl>
    <dt><a href="https://medium.com/@martijn.van.welie/making-android-ble-work-part-1-a736dcd53b02">
        Making Android BLE work — part 1 // Martin van Wellie</a></dt>
        <dd>Самое толковое и подробное описание работы с Bluetooth BLE, с кучей ссылок на различные источники.
Подробно о сканировании устройств. Почему-то не отражена проблема сканирования устройств с фильтрами. </dd>
    <dt><a href="https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07">
        Making Android BLE work — part 2 // Martijn van Welie</a></dt>
        <dd></dd>
</dl>