package com.grandfatherpikhto.blescan.ui

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.grandfatherpikhto.blescan.BleScanApp
import com.grandfatherpikhto.blescan.fake.FakeBleManager
import com.grandfatherpikhto.blescan.R
import com.grandfatherpikhto.blescan.helper.withDrawable

import org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    private val argIntent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        .putExtra(MainActivity.FAKE, true)

    @get:Rule
    val mainActivityRule = activityScenarioRule<MainActivity>(argIntent)
    private val applicationContext = InstrumentationRegistry
        .getInstrumentation().targetContext.applicationContext


    private val _bleManager by lazy {
        (applicationContext as BleScanApp).bleManager
    }
    private val bleManager get() = _bleManager!!


    @Before
    fun setUp() {
        mainActivityRule.scenario.onActivity { }
        IdlingRegistry.getInstance().register((bleManager as FakeBleManager)
            .scanIdling)
        IdlingRegistry.getInstance().register((bleManager as FakeBleManager)
            .connectingIdling)
        IdlingRegistry.getInstance().register((bleManager as FakeBleManager)
            .disconnectingIdling)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister((bleManager as FakeBleManager)
            .scanIdling)
        IdlingRegistry.getInstance().unregister((bleManager as FakeBleManager)
            .connectingIdling)
        IdlingRegistry.getInstance().unregister((bleManager as FakeBleManager)
            .disconnectingIdling)
    }

    @Test(timeout = 15000)
    fun connectDevice() {
        Espresso.onView(withId(R.id.cl_scan_fragment))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(withId(R.id.action_scan)).perform(ViewActions.click())
        val scanResult = bleManager.scanResults.filter { it.isConnectable }[0]
        Espresso.onView(withText(scanResult.device.name))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .perform(ViewActions.click())
        Espresso.onView(withId(R.id.cl_device))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(withId(R.id.iv_ble_connected))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(withDrawable(R.drawable.ic_connect_big)))
        Espresso.onView(withId(R.id.action_connect)).perform(ViewActions.click())
        Espresso.onView(withId(R.id.iv_ble_connected))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(withDrawable(R.drawable.ic_disconnect_big)))
    }
}