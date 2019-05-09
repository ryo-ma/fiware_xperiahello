package jp.co.tis.stc.roboticbase.core.fiware_xperiahello_guide

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockito_kotlin.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.*


@RunWith(Enclosed::class)
class OperationActivityTest {

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [(Build.VERSION_CODES.O)])
    class LifeCycleTest(
            private val useSSL: Boolean?,
            private val host: String?,
            private val port: String?,
            private val username: String?,
            private val password: String?) {
        private var controller: ActivityController<OperationActivity>? = null
        private var activity: OperationActivity? = null
        private var mockedClient: MqttAndroidClient? = null

        private inline fun <reified T : Any> argumentCaptor() = ArgumentCaptor.forClass(T::class.java)

        @Before
        fun setUp() {
            mockedClient = mock {}
            controller = Robolectric.buildActivity(OperationActivity::class.java)
            activity = controller?.get()

            if (useSSL != null || host != null || port != null || username != null || password != null) {
                val context = ApplicationProvider.getApplicationContext<Application>()
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
                val editor = sharedPref.edit()
                if (useSSL != null) {
                    editor.putBoolean(activity!!.getString(R.string.settings_item_mqtt_use_ssl_key), useSSL)
                }
                if (host != null) {
                    editor.putString(activity!!.getString(R.string.settings_item_mqtt_host_key), host)
                }
                if (port != null) {
                    editor.putString(activity!!.getString(R.string.settings_item_mqtt_port_key), port)
                }
                if (username != null) {
                    editor.putString(activity!!.getString(R.string.settings_item_mqtt_username_key), username)
                }
                if (password != null) {
                    editor.putString(activity!!.getString(R.string.settings_item_mqtt_password_key), password)
                }
                editor.commit()
            }
        }

        @Test
        fun testLyfeCycle() {
            assertEquals(0, activity!!.window.decorView.systemUiVisibility)

            controller?.create()

            assertNull(activity!!.supportActionBar)
            assertTrue(activity!!.triangleButton.hasOnClickListeners())
            assertTrue(activity!!.squareButton.hasOnClickListeners())
            assertTrue(activity!!.circleButton.hasOnClickListeners())
            assertTrue(activity!!.crossButton.hasOnClickListeners())
            assertNull(activity!!.client)
            assertEquals(3846, activity!!.window.decorView.systemUiVisibility)

            val schema = useSSL?.let {if (it) "ssl" else "tcp"} ?: "tcp"
            val url = "$schema://${host ?: ""}:${port ?: ""}"
            if (host == VALID_HOST && port == VALID_PORT) {
                val optionsCaptor = argumentCaptor<MqttConnectOptions>()
                val userContextCaptor = argumentCaptor<Any>()
                val callbackCaptor = argumentCaptor<IMqttActionListener>()

                activity!!.client = mockedClient
                controller?.start()?.resume()?.visible()
                val lastAlertDialog = ShadowAlertDialog.getLatestAlertDialog()
                assertNull(lastAlertDialog)
                verify(mockedClient)?.connect(optionsCaptor.capture(), userContextCaptor.capture(), callbackCaptor.capture())
                assertTrue(optionsCaptor.value.isCleanSession)
                if (username == VALID_USERNAME) {
                    assertEquals(username, optionsCaptor.value.userName)
                } else {
                    assertNull(optionsCaptor.value.userName)
                }
                if (password == VALID_PASSWORD) {
                    assertEquals(password, optionsCaptor.value.password.joinToString(""))
                } else {
                    assertNull(optionsCaptor.value.password)
                }

                val e = Exception("test")
                val token = object : IMqttToken {
                    override fun getActionCallback(): IMqttActionListener {
                        return object:IMqttActionListener{
                            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {}
                            override fun onSuccess(asyncActionToken: IMqttToken?) {}
                        }
                    }
                    override fun getClient(): IMqttAsyncClient {
                        return MqttAsyncClient("", "")
                    }
                    override fun getException(): MqttException {
                        return MqttException(Exception())
                    }
                    override fun getGrantedQos(): IntArray {
                        return intArrayOf()
                    }
                    override fun getMessageId(): Int {
                        return 0
                    }
                    override fun getResponse(): MqttWireMessage {
                        return MqttWireMessage.createWireMessage(byteArrayOf())
                    }
                    override fun getSessionPresent(): Boolean {
                        return true
                    }
                    override fun getTopics(): Array<String> {
                        return arrayOf("")
                    }
                    override fun getUserContext(): Any {
                        return ""
                    }
                    override fun isComplete(): Boolean {
                        return true
                    }
                    override fun setActionCallback(listener: IMqttActionListener?) {}
                    override fun setUserContext(userContext: Any?) {}
                    override fun waitForCompletion() {}
                    override fun waitForCompletion(timeout: Long) {}
                }
                callbackCaptor.value.onFailure(token, e)
                val failureAlertDialog = ShadowAlertDialog.getLatestAlertDialog()
                assertNotNull(failureAlertDialog)
                assertEquals("MQTT接続失敗", shadowOf(failureAlertDialog).title)
                assertEquals("url = $url, $e", shadowOf(failureAlertDialog).message)
                val okButton = failureAlertDialog.getButton(Dialog.BUTTON_POSITIVE)
                okButton.performClick()

                controller?.pause()?.destroy()
                verify(mockedClient)?.disconnect()
                verify(mockedClient)?.unregisterResources()
            } else {
                controller?.start()?.resume()?.visible()
                val lastAlertDialog = ShadowAlertDialog.getLatestAlertDialog()
                assertNotNull(lastAlertDialog)
                assertEquals("MQTT接続失敗", shadowOf(lastAlertDialog).title)
                assertEquals("不正なURL, url = $url", shadowOf(lastAlertDialog).message)
                val okButton = lastAlertDialog.getButton(Dialog.BUTTON_POSITIVE)
                okButton.performClick()
                verify(mockedClient, never())?.connect(any(MqttConnectOptions::class.java), any(Object::class.java), any(IMqttActionListener::class.java))

                controller?.pause()?.destroy()
                verify(mockedClient, never())?.disconnect()
                verify(mockedClient, never())?.unregisterResources()
            }
        }

        companion object {
            const val VALID_HOST = "mqtt.example.com"
            const val VALID_PORT = "8883"
            const val VALID_USERNAME = "username"
            const val VALID_PASSWORD = "password"

            @ParameterizedRobolectricTestRunner.Parameters(name = "useSSL = {0}, host = {1}, port = {2}, username = {3}, password = {4}")
            @JvmStatic
            fun testParams(): List<Array<Any?>> {
                val result = mutableListOf<Array<Any?>>()
                for (useSSL in listOf(null, true, false)) {
                    for (host in listOf(null, "", VALID_HOST)) {
                        for (port in listOf(null, "", "invalid", VALID_PORT)) {
                            for (username in listOf(null, "", VALID_USERNAME)) {
                                for (password in listOf(null, "", VALID_PASSWORD)) {
                                    result.add(arrayOf(useSSL, host, port, username, password))
                                }
                            }
                        }
                    }
                }
                return result
            }
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [(Build.VERSION_CODES.O)])
    class ButtonTest(
            private val hasClient: Boolean,
            private val isConnected: Boolean,
            private val buttonKey: String) : Mixin {
        private var activity: OperationActivity? = null
        private var mockedClient: MqttAndroidClient? = null

        private inline fun <reified T : Any> argumentCaptor() = ArgumentCaptor.forClass(T::class.java)

        @Before
        fun setUp() {
            mockedClient = mock {
                on { isConnected } doReturn isConnected
            }
            activity = Robolectric.setupActivity(OperationActivity::class.java)
            if (hasClient) {
                activity!!.client = mockedClient
            } else {
                activity!!.client = null
            }

            val context = ApplicationProvider.getApplicationContext<Application>()
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = sharedPref.edit()
            editor.putString(activity!!.getString(R.string.settings_item_mqtt_base_topic_key), BASE_TOPIC)
            editor.commit()
        }

        @Test
        fun testClickButton() {
            when (buttonKey) {
                TRIANGLE_BUTTON -> {
                    activity!!.triangleButton.performClick()
                }
                SQUARE_BUTTON -> {
                    activity!!.squareButton.performClick()
                }
                CIRCLE_BUTTON -> {
                    activity!!.circleButton.performClick()
                }
                CROSS_BUTTON -> {
                    activity!!.crossButton.performClick()
                }
            }
            assertEquals(Activity.RESULT_OK, shadowOf(activity)!!.resultCode)
            val expected = Intent()
            expected.putExtra(OPERATION_RESULT_KEY, buttonKey)
            assertEquals(expected.component, shadowOf(activity)!!.resultIntent.component)
            assertTrue(activity!!.isFinishing)
            if (hasClient) {
                if (isConnected) {
                    val topicCaptor = argumentCaptor<String>()
                    val payloadCpator = argumentCaptor<ByteArray>()
                    val qosCaptor = argumentCaptor<Int>()
                    val retainedCaptor = argumentCaptor<Boolean>()

                    verify(mockedClient)?.isConnected
                    verify(mockedClient)?.publish(topicCaptor.capture(), payloadCpator.capture(), qosCaptor.capture(), retainedCaptor.capture())
                    assertEquals("$BASE_TOPIC/attrs", topicCaptor.value)
                    val pattern = """^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?\+09:00\|button\|$buttonKey$"""
                    assertTrue(String(payloadCpator.value).matches(Regex(pattern)))
                    assertEquals(0, qosCaptor.value)
                    assertFalse(retainedCaptor.value)
                } else {
                    val lastAlertDialog = ShadowAlertDialog.getLatestAlertDialog()
                    assertNotNull(lastAlertDialog)
                    assertEquals("MQTT未接続", shadowOf(lastAlertDialog).title)
                    assertEquals("MQTTに接続していません", shadowOf(lastAlertDialog).message)
                    val okButton = lastAlertDialog.getButton(Dialog.BUTTON_POSITIVE)
                    okButton.performClick()
                    verify(mockedClient, never())?.connect(any(MqttConnectOptions::class.java), any(Object::class.java), any(IMqttActionListener::class.java))

                    verify(mockedClient)?.isConnected
                    verify(mockedClient, never())!!.publish(anyString(), any(byteArrayOf()::class.java), anyInt(), anyBoolean())
                }
            } else {
                verify(mockedClient, never())!!.isConnected
                verify(mockedClient, never())!!.publish(anyString(), any(byteArrayOf()::class.java), anyInt(), anyBoolean())
            }
        }

        companion object {
            const val BASE_TOPIC = "/topic"

            @ParameterizedRobolectricTestRunner.Parameters(name = "hasClient = {0}, isConnected = {1}, buttonKey = {2}")
            @JvmStatic
            fun testParams(): List<Array<Any>> {
                val mixIn = object:Mixin{}
                val result = mutableListOf<Array<Any>>()
                for (hasClient in arrayOf(true, false)) {
                    for (isConnected in arrayOf(true, false)) {
                        for (buttonKey in arrayOf(
                                mixIn.TRIANGLE_BUTTON,
                                mixIn.SQUARE_BUTTON,
                                mixIn.CIRCLE_BUTTON,
                                mixIn.CROSS_BUTTON)) {
                            result.add(arrayOf(hasClient, isConnected, buttonKey))
                        }
                    }
                }
                return result
            }
        }
    }
}