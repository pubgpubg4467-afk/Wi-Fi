package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AudioStreamType
import com.example.model.BluetoothScenario
import com.example.model.DeviceType
import com.example.service.BluetoothHelper
import com.example.service.WifiHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NetPulse", appName)
    }

    @Test
    fun `wifi frequency to channel calculations`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = WifiHelper(context)

        assertEquals(1, helper.frequencyToChannel(2412))
        assertEquals(6, helper.frequencyToChannel(2437))
        assertEquals(11, helper.frequencyToChannel(2462))
        assertEquals(36, helper.frequencyToChannel(5180))

        assertEquals("2.4 GHz", helper.calculateFrequencyBand(2412))
        assertEquals("5 GHz", helper.calculateFrequencyBand(5180))
    }

    @Test
    fun `friis distance calculation returns positive realistic value`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = WifiHelper(context)
        val distance = helper.calculateEstimatedDistance(5180, -42)
        assertTrue(distance in 0.1..20.0)
    }

    @Test
    fun `bluetooth audio routing matrix and device management`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val btHelper = BluetoothHelper(context)

        val paired = btHelper.pairedDevices.value
        assertTrue(paired.isNotEmpty())

        val firstDev = paired.first()
        // Connect / Disconnect test
        btHelper.disconnectDevice(firstDev.address)
        assertFalse(btHelper.pairedDevices.value.first { it.address == firstDev.address }.isConnected)

        btHelper.connectDevice(firstDev.address)
        assertTrue(btHelper.pairedDevices.value.first { it.address == firstDev.address }.isConnected)

        // Routing test
        btHelper.routeStream(AudioStreamType.MEDIA, firstDev.address)
        assertEquals(firstDev.address, btHelper.routingMatrix.value.mediaDeviceAddress)

        // Scenario test: Driving
        btHelper.applyScenario(BluetoothScenario.DRIVING)
        assertEquals(BluetoothScenario.DRIVING, btHelper.activeScenario.value)
    }
}
