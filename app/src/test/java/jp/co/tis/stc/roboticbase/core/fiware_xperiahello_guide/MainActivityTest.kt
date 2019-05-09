package jp.co.tis.stc.roboticbase.core.fiware_xperiahello_guide

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockito_kotlin.mock
import com.sonymobile.smartproduct.xperia_hello_sdk_clientapi.APIdefinitions
import com.sonymobile.smartproduct.xperia_hello_sdk_clientapi.ClientAPI
import kotlinx.android.synthetic.main.activity_main.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.ScheduledFuture


@RunWith(Enclosed::class)
class MainActivityTest {

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [(Build.VERSION_CODES.O)])
    class LifeCycleTest(private val isMotion: Boolean) : TestHelper {

        @Test
        fun testLyfecycle() {

            val mockedmAPI = mock<ClientAPI> {
            }

            val controller = Robolectric.buildActivity(MainActivity::class.java)
            val activity = controller.create().get()

            assertNotNull(activity.supportActionBar)
            assertTrue(activity.mainButton.hasOnClickListeners())
            assertFalse(getPrivateProperty(activity, "isMove") as Boolean)
            assertFalse(getPrivateProperty(activity, "isMotion") as Boolean)
            assertFalse(getPrivateProperty(activity, "isDemo") as Boolean)
            assertFalse(getPrivateProperty(activity, "isSpeak") as Boolean)
            assertFalse(getPrivateProperty(activity, "willEyeClose") as Boolean)
            assertNull(getPrivateProperty(activity, "scheduler"))
            assertNull(getPrivateProperty(activity, "future"))
            assertNull(getPrivateProperty(activity, "mAPI"))
            assertEquals(0, activity.window.decorView.systemUiVisibility)

            activity.mAPI = mockedmAPI

            controller.start().resume()

            assertNotNull(activity.supportActionBar)
            assertFalse(getPrivateProperty(activity, "isMove") as Boolean)
            assertFalse(getPrivateProperty(activity, "isMotion") as Boolean)
            assertFalse(getPrivateProperty(activity, "isDemo") as Boolean)
            assertFalse(getPrivateProperty(activity, "isSpeak") as Boolean)
            assertFalse(getPrivateProperty(activity, "willEyeClose") as Boolean)
            assertNotNull(getPrivateProperty(activity, "scheduler"))
            assertNotNull(getPrivateProperty(activity, "future"))
            assertNotNull(getPrivateProperty(activity, "mAPI"))
            assertEquals(0, activity.window.decorView.systemUiVisibility)

            verify(mockedmAPI).startMotion("B_ACT-515.smd")

            val future = getPrivateProperty(activity, "future") as ScheduledFuture<*>
            future.cancel(true)

            controller.visible()
            activity.onWindowFocusChanged(true)

            assertEquals(3846, activity.window.decorView.systemUiVisibility)

            setPrivateProperty(activity, "isMotion", isMotion)
            if (isMotion) {
                assertTrue(getPrivateProperty(activity, "isMotion") as Boolean)
                controller.pause()
                verify(mockedmAPI).stopMotion()
                assertTrue(getPrivateProperty(activity, "willEyeClose") as Boolean)

                controller.stop().destroy()
                verify(mockedmAPI).startEyePattern(0)
                verify(mockedmAPI).release()
            } else {
                assertFalse(getPrivateProperty(activity, "isMotion") as Boolean)
                controller.pause()
                verify(mockedmAPI, times(1)).startEyePattern(0)
                verify(mockedmAPI).startNeckColorChange(APIdefinitions.COLOR_OFF)
                assertFalse(getPrivateProperty(activity, "willEyeClose") as Boolean)

                controller.stop().destroy()
                verify(mockedmAPI, times(2)).startEyePattern(0)
                verify(mockedmAPI).release()
            }
        }

        companion object {
            @ParameterizedRobolectricTestRunner.Parameters(name = "isMotion = {0}")
            @JvmStatic
            fun testParams(): List<Array<out Boolean>> {
                return listOf(
                        arrayOf(true),
                        arrayOf(false))
            }
        }
    }

    @RunWith(RobolectricTestRunner::class)
    @Config(sdk = [(Build.VERSION_CODES.O)])
    class SettingsTest : TestHelper {
        private var activity: MainActivity? = null
        private var mockedmAPI: ClientAPI? = null

        @Before
        fun setUp() {
            mockedmAPI = mock {}
            activity = Robolectric.setupActivity(MainActivity::class.java)
            val future = getPrivateProperty(activity!!, "future") as ScheduledFuture<*>
            future.cancel(true)
            activity!!.mAPI = mockedmAPI
        }

        @Test
        fun testStartSettings() {
            val shadow = shadowOf(activity)
            shadow.clickMenuItem(R.id.settings)
            val expected = Intent(activity, SettingsActivity::class.java)
            val actual = shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity
            assertEquals(expected.component, actual.component)
            assertFalse(getPrivateProperty(activity, "isMove") as Boolean)
            assertFalse(getPrivateProperty(activity, "isMotion") as Boolean)
            assertTrue(getPrivateProperty(activity, "isDemo") as Boolean)
            assertFalse(getPrivateProperty(activity, "isSpeak") as Boolean)
            assertFalse(getPrivateProperty(activity, "willEyeClose") as Boolean)

            verify(mockedmAPI)!!.stopSpeak()
        }

        @Test
        fun testCancelSettings() {
            val shadow = shadowOf(activity)
            shadow.clickMenuItem(R.id.cancel)
            val actual = shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity
            assertNull(actual)
            assertFalse(getPrivateProperty(activity, "isMove") as Boolean)
            assertFalse(getPrivateProperty(activity, "isMotion") as Boolean)
            assertFalse(getPrivateProperty(activity, "isDemo") as Boolean)
            assertFalse(getPrivateProperty(activity, "isSpeak") as Boolean)
            assertFalse(getPrivateProperty(activity, "willEyeClose") as Boolean)

            verify(mockedmAPI, never())!!.stopSpeak()
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [(Build.VERSION_CODES.O)])
    class ButtonTest(private val hasSharedPref: Boolean, private val buttonKey: String?) : TestHelper, Mixin {
        private var activity: MainActivity? = null
        private var mockedmAPI: ClientAPI? = null

        @Before
        fun setUp() {
            mockedmAPI = mock {}
            activity = Robolectric.setupActivity(MainActivity::class.java)
            val future = getPrivateProperty(activity!!, "future") as ScheduledFuture<*>
            future.cancel(true)
            activity!!.mAPI = mockedmAPI

            if (hasSharedPref) {
                val context = ApplicationProvider.getApplicationContext<Application>()
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
                val editor = sharedPref.edit()
                editor.putString(activity!!.getString(R.string.settings_item_talk_start_demo_message_key), TALK_START_MESSAGE)
                editor.putString(activity!!.getString(R.string.settings_item_talk_triangle_message_key), TRIANGLE_MESSAGE)
                editor.putString(activity!!.getString(R.string.settings_item_talk_square_message_key), SQUARE_MESSAGE)
                editor.putString(activity!!.getString(R.string.settings_item_talk_circle_message_key), CIRCLE_MESSAGE)
                editor.putString(activity!!.getString(R.string.settings_item_talk_cross_message_key), CROSS_MESSAGE)
                editor.commit()
            }
        }

        @Test
        fun testClickMainButton() {

            activity!!.mainButton.performClick()
            val expected = Intent(activity, OperationActivity::class.java)
            val actual = shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity
            assertEquals(expected.component, actual.component)
            assertFalse(getPrivateProperty(activity, "isMove") as Boolean)
            assertFalse(getPrivateProperty(activity, "isMotion") as Boolean)
            assertTrue(getPrivateProperty(activity, "isDemo") as Boolean)
            assertTrue(getPrivateProperty(activity, "isSpeak") as Boolean)
            assertFalse(getPrivateProperty(activity, "willEyeClose") as Boolean)

            verify(mockedmAPI, times(1))!!.stopSpeak()
            if (hasSharedPref) {
                verify(mockedmAPI, times(1))!!.startSpeak(TALK_START_MESSAGE, AudioManager.STREAM_MUSIC, true)
            } else {
                verify(mockedmAPI, times(1))!!.startSpeak("", AudioManager.STREAM_MUSIC, true)
            }

            shadowOf(activity).receiveResult(
                    Intent(activity, OperationActivity::class.java),
                    Activity.RESULT_OK,
                    buttonKey?.let {Intent().putExtra(OPERATION_RESULT_KEY, it)}
            )
            assertTrue(getPrivateProperty(activity, "isSpeak") as Boolean)
            verify(mockedmAPI, times(2))!!.stopSpeak()
            if (hasSharedPref) {
                when (buttonKey) {
                    TRIANGLE_BUTTON -> {
                        verify(mockedmAPI, times(1))!!.startSpeak(TRIANGLE_MESSAGE, AudioManager.STREAM_MUSIC, true)
                    }
                    SQUARE_BUTTON -> {
                        verify(mockedmAPI, times(1))!!.startSpeak(SQUARE_MESSAGE, AudioManager.STREAM_MUSIC, true)
                    }
                    CIRCLE_BUTTON -> {
                        verify(mockedmAPI, times(1))!!.startSpeak(CIRCLE_MESSAGE, AudioManager.STREAM_MUSIC, true)
                    }
                    CROSS_BUTTON -> {
                        verify(mockedmAPI, times(1))!!.startSpeak(CROSS_MESSAGE, AudioManager.STREAM_MUSIC, true)
                    }
                    else -> {
                        verify(mockedmAPI, times(1))!!.startSpeak("", AudioManager.STREAM_MUSIC, true)
                    }
                }
            } else {
                verify(mockedmAPI, times(2))!!.startSpeak("", AudioManager.STREAM_MUSIC, true)
            }
        }

        companion object {
            const val TALK_START_MESSAGE = "tark start message"
            const val TRIANGLE_MESSAGE = "triangle message"
            const val SQUARE_MESSAGE = "square message"
            const val CIRCLE_MESSAGE = "circle message"
            const val CROSS_MESSAGE = "cross message"

            @ParameterizedRobolectricTestRunner.Parameters(name = "hasSharedPref = {0}, buttonKey = {1}")
            @JvmStatic
            fun testParams(): List<Array<Any?>> {
                val mixIn = object:Mixin{}
                val result = mutableListOf<Array<Any?>>()
                for (hasSharedPref in listOf(true, false)) {
                    for (buttonKey in listOf(
                            mixIn.TRIANGLE_BUTTON,
                            mixIn.SQUARE_BUTTON,
                            mixIn.CIRCLE_BUTTON,
                            mixIn.CROSS_BUTTON,
                            "invalid",
                            null)) {
                        result.add(arrayOf(hasSharedPref, buttonKey))
                    }
                }
                return result
            }
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [(Build.VERSION_CODES.O)])
    class APICallbackTest(private val result: Boolean) : TestHelper {
        private var activity: MainActivity? = null
        private var mockedmAPI: ClientAPI? = null

        @Before
        fun setUp() {
            mockedmAPI = mock {}
            activity = Robolectric.setupActivity(MainActivity::class.java)
            val future = getPrivateProperty(activity!!, "future") as ScheduledFuture<*>
            future.cancel(true)
            activity!!.mAPI = mockedmAPI
        }

        @Test
        fun testOnInitialized() {
            activity!!.onInitialized(result, 0)
            if (!result) {
                verify(mockedmAPI)!!.release()
                verify(mockedmAPI, never())!!.setOnDetectHumanBodyListener()
                verify(mockedmAPI, never())!!.startInitializePosition(1000)
                assertNull(getPrivateProperty(activity, "mAPI"))
            } else {
                verify(mockedmAPI, never())!!.release()
                verify(mockedmAPI)!!.setOnDetectHumanBodyListener()
                verify(mockedmAPI)!!.startInitializePosition(1000)
                assertNotNull(getPrivateProperty(activity, "mAPI"))
            }
        }

        @Test
        fun testOnStartInitializePositionCompleted() {
            activity!!.onStartInitializePositionCompleted(result)
            verify(mockedmAPI)!!.startMotion("B_ACT-515.smd")
        }

        @Test
        fun testOnStartAngleOfCompleted() {
            setPrivateProperty(activity, "isMove", true)
            activity!!.onStartAngleOfCompleted(result, 0)
            assertFalse(getPrivateProperty(activity, "isMove") as Boolean)
        }

        @Test
        fun testOnStartMotionCompleted() {
            setPrivateProperty(activity, "isMotion", true)
            activity!!.onStartMotionCompleted(result)
            assertFalse(getPrivateProperty(activity, "isMotion") as Boolean)
        }

        @Test
        fun testOnStopMotionCompletedWillEyeCloseTrue() {
            setPrivateProperty(activity, "willEyeClose", true)
            activity!!.onStopMotionCompleted(result)
            verify(mockedmAPI)!!.startEyePattern(0)
            verify(mockedmAPI)!!.startNeckColorChange(APIdefinitions.COLOR_OFF)
            assertFalse(getPrivateProperty(activity, "willEyeClose") as Boolean)
        }

        @Test
        fun testOnStopMotionCompletedWillEyeCloseFalse() {
            setPrivateProperty(activity, "willEyeClose", false)
            activity!!.onStopMotionCompleted(result)
            verify(mockedmAPI, never())!!.startEyePattern(0)
            verify(mockedmAPI, never())!!.startNeckColorChange(APIdefinitions.COLOR_OFF)
            assertFalse(getPrivateProperty(activity, "willEyeClose") as Boolean)
        }

        @Test
        fun testOnStartSpeakCompleted() {
            setPrivateProperty(activity, "isSpeak", true)
            activity!!.onStartSpeakCompleted(result)
            assertFalse(getPrivateProperty(activity, "isSpeak") as Boolean)
        }

        @Test
        fun testOnStopSpeakCompleted() {
            setPrivateProperty(activity, "isSpeak", true)
            activity!!.onStopSpeakCompleted(result)
            assertFalse(getPrivateProperty(activity, "isSpeak") as Boolean)
        }

        companion object {
            @ParameterizedRobolectricTestRunner.Parameters(name = "callback result = {0}")
            @JvmStatic
            fun testParams(): List<Array<out Boolean>> {
                return listOf(
                        arrayOf(true),
                        arrayOf(false))
            }
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [(Build.VERSION_CODES.O)])
    class DetectHumanBodyTest(
            private val isMove: Boolean,
            private val isSpeak: Boolean,
            private val greetMessage: String?,
            private val talkIntervalSec: String?,
            private val direction: IntArray?
    ) : TestHelper {
        private var activity: MainActivity? = null
        private var mockedmAPI: ClientAPI? = null

        @Before
        fun setUp() {
            mockedmAPI = mock {}
            activity = Robolectric.setupActivity(MainActivity::class.java)
            val future = getPrivateProperty(activity!!, "future") as ScheduledFuture<*>
            future.cancel(true)
            activity!!.mAPI = mockedmAPI

            setPrivateProperty(activity, "isMove", isMove)
            setPrivateProperty(activity, "isSpeak", isSpeak)
            setPrivateProperty(activity, "isMotion", false)
            setPrivateProperty(activity, "isDemo", false)

            if (greetMessage != null || talkIntervalSec != null) {
                val context = ApplicationProvider.getApplicationContext<Application>()
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
                val editor = sharedPref.edit()
                if (greetMessage != null) {
                    editor.putString(activity!!.getString(R.string.settings_item_talk_greet_message_key), greetMessage)
                }
                if (talkIntervalSec != null) {
                    editor.putString(activity!!.getString(R.string.settings_item_talk_interval_sec_key), talkIntervalSec)
                }
                editor.commit()
            }
        }

        @Test
        fun testOnDetectHumanBody() {
            activity!!.onDetectHumanBody(direction)

            val doNothing = fun () {
                assertEquals(isSpeak, getPrivateProperty(activity, "isSpeak"))
                assertFalse(getPrivateProperty(activity, "isMotion") as Boolean)
                verify(mockedmAPI, never())!!.startSpeak("", AudioManager.STREAM_MUSIC, true)
                verify(mockedmAPI, never())!!.startMotion("B_ACT-101.smd")
                verify(mockedmAPI, never())!!.startAngleOf(APIdefinitions.ANGLE_BODY, 60, 2000)
                verify(mockedmAPI, never())!!.startAngleOf(APIdefinitions.ANGLE_BODY, -60, 2000)
            }

            if (isMove) {
                doNothing()
            } else {
                direction?.let {d ->
                    if (intArrayOf(0, 1, 0, 0, 0, 0, 0, 1) contentEquals d ||
                            intArrayOf(1, 1, 0, 0, 0, 0, 0, 1) contentEquals d) {
                        assertTrue(getPrivateProperty(activity, "isMotion") as Boolean)
                        verify(mockedmAPI)!!.startMotion("B_ACT-101.smd")
                        verify(mockedmAPI, never())!!.startAngleOf(APIdefinitions.ANGLE_BODY, 60, 2000)
                        verify(mockedmAPI, never())!!.startAngleOf(APIdefinitions.ANGLE_BODY, -60, 2000)
                        if (!isSpeak && talkIntervalSec != "600") {
                            verify(mockedmAPI)!!.startSpeak(greetMessage ?: "", AudioManager.STREAM_MUSIC, true)
                            assertTrue(getPrivateProperty(activity, "isSpeak") as Boolean)
                        } else {
                            verify(mockedmAPI, never())!!.startSpeak(greetMessage ?: "", AudioManager.STREAM_MUSIC, true)
                            assertEquals(isSpeak, getPrivateProperty(activity, "isSpeak"))
                        }
                    } else if (intArrayOf(0, 1, 0, 0, 0, 0, 0, 0) contentEquals d ||
                            intArrayOf(0, 1, 1, 0, 0, 0, 0, 0) contentEquals d ||
                            intArrayOf(0, 1, 0, 1, 0, 0, 0, 0) contentEquals d ||
                            intArrayOf(0, 1, 1, 1, 0, 0, 0, 0) contentEquals d) {
                        assertTrue(getPrivateProperty(activity, "isMotion") as Boolean)
                        verify(mockedmAPI)!!.startMotion("B_ACT-101.smd")
                        verify(mockedmAPI)!!.startAngleOf(APIdefinitions.ANGLE_BODY, 60, 2000)
                        verify(mockedmAPI, never())!!.startAngleOf(APIdefinitions.ANGLE_BODY, -60, 2000)
                        if (!isSpeak && talkIntervalSec != "600") {
                            verify(mockedmAPI)!!.startSpeak(greetMessage ?: "", AudioManager.STREAM_MUSIC, true)
                            assertTrue(getPrivateProperty(activity, "isSpeak") as Boolean)
                        } else {
                            verify(mockedmAPI, never())!!.startSpeak(greetMessage ?: "", AudioManager.STREAM_MUSIC, true)
                            assertEquals(isSpeak, getPrivateProperty(activity, "isSpeak"))
                        }
                    } else if (intArrayOf(0, 0, 0, 0, 0, 0, 0, 1) contentEquals d ||
                            intArrayOf(0, 0, 0, 0, 0, 0, 1, 1) contentEquals d ||
                            intArrayOf(0, 0, 0, 0, 0, 1, 0, 1) contentEquals d ||
                            intArrayOf(0, 0, 0, 0, 0, 1, 1, 1) contentEquals d) {
                        assertTrue(getPrivateProperty(activity, "isMotion") as Boolean)
                        verify(mockedmAPI)!!.startMotion("B_ACT-101.smd")
                        verify(mockedmAPI)!!.startAngleOf(APIdefinitions.ANGLE_BODY, -60, 2000)
                        verify(mockedmAPI, never())!!.startAngleOf(APIdefinitions.ANGLE_BODY, 60, 2000)
                        if (!isSpeak && talkIntervalSec != "600") {
                            verify(mockedmAPI)!!.startSpeak(greetMessage ?: "", AudioManager.STREAM_MUSIC, true)
                            assertTrue(getPrivateProperty(activity, "isSpeak") as Boolean)
                        } else {
                            verify(mockedmAPI, never())!!.startSpeak(greetMessage ?: "", AudioManager.STREAM_MUSIC, true)
                            assertEquals(isSpeak, getPrivateProperty(activity, "isSpeak"))
                        }
                    } else {
                        doNothing()
                    }
                } ?: run {
                    doNothing()
                }
            }
        }

        companion object {
            @ParameterizedRobolectricTestRunner.Parameters(name = "isMove = {0}, isSpeak = {1}, greetMessage = {2}, talkIntervalSec = {3}, direction = {4}")
            @JvmStatic
            fun testParams(): List<Array<Any?>> {
                val result  = mutableListOf<Array<Any?>>()
                for (isMove in listOf(true, false)) {
                    for (isSpeak in listOf(true, false)) {
                        for (greetMessage in listOf(null, "", "greet message")) {
                            for (talkIntervalSec in listOf(null, "0", "600")) {
                                for (direction in listOf(
                                        intArrayOf(0, 1, 0, 0, 0, 0, 0, 1),
                                        intArrayOf(1, 1, 0, 0, 0, 0, 0, 1),

                                        intArrayOf(0, 1, 0, 0, 0, 0, 0, 0),
                                        intArrayOf(0, 1, 1, 0, 0, 0, 0, 0),
                                        intArrayOf(0, 1, 0, 1, 0, 0, 0, 0),
                                        intArrayOf(0, 1, 1, 1, 0, 0, 0, 0),

                                        intArrayOf(0, 0, 0, 0, 0, 0, 0, 1),
                                        intArrayOf(0, 0, 0, 0, 0, 0, 1, 1),
                                        intArrayOf(0, 0, 0, 0, 0, 1, 0, 1),
                                        intArrayOf(0, 0, 0, 0, 0, 1, 1, 1),

                                        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
                                        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
                                        null
                                )) {
                                    result.add(arrayOf(isMove, isSpeak, greetMessage, talkIntervalSec, direction))
                                }
                            }
                        }
                    }
                }
                return result
            }
        }
    }
}