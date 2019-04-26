package jp.co.tis.stc.roboticbase.core.fiware_xperiahello

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Rect
import android.media.AudioManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.sonymobile.smartproduct.xperia_hello_sdk_clientapi.APIdefinitions
import com.sonymobile.smartproduct.xperia_hello_sdk_clientapi.ClientAPI
import kotlinx.android.synthetic.main.activity_main.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


class MainActivity : ClientAPIImplActivity(), Mixin {
    private var sharedPref : SharedPreferences? = null
    private var animSlideUp : Animation? = null
    private var animSlideDown : Animation? = null
    private val statusBarHeight : Int by lazy {
        val rect = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rect)
        rect.top
    }

    private var isMove = false
    private var isMotion = false
    private var isDemo = false
    private var isSpeak = false
    private var willEyeClose = false
    private var current = LocalDateTime.now()

    private var scheduler: ScheduledExecutorService? = null
    private var future: ScheduledFuture<*>? = null
    internal var mAPI: ClientAPI? = null

    // Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_main)
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        setSupportActionBar(toolbar)
        setUpView()
        setUpPermission()
        setUpButton()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        mAPI?.startMotion("B_ACT-515.smd")
        isDemo = false
        willEyeClose = false
        newTimerTask()
    }

    override fun onPause() {
        Log.d(TAG, "onPause, isMotion=$isMotion")
        future?.cancel(true)
        if (isMotion) {
            mAPI?.stopMotion()
            willEyeClose = true
        } else {
            mAPI?.startEyePattern(0)
            mAPI?.startNeckColorChange(APIdefinitions.COLOR_OFF)
        }
        super.onPause()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        mAPI?.startEyePattern(0)
        mAPI?.release()
        super.onDestroy()
    }

    // initialize
    private fun setUpView() {
        animSlideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        animSlideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility: Int ->
            if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == View.VISIBLE) {
                toolbar?.startAnimation(animSlideDown)
                supportActionBar?.show()
            } else {
                toolbar?.startAnimation(animSlideUp)
                supportActionBar?.hide()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        toolbar.translationY = statusBarHeight.toFloat()
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menuInflater.inflate(R.menu.settings, menu)
        return true
    }

    private fun setUpPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        } else {
            initializeAPI()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initializeAPI()
                } else {
                    finish()
                }
            }
        }
    }

    private fun initializeAPI() {
        Log.d(TAG, "initialize mAPI")
        mAPI = ClientAPI(this, this, CERTIFICATE_NAME)
        if (mAPI == null) {
            Log.e(TAG, "ERROR: fail to initialize SDK service (Null Instance)")
            finish()
        }
    }

    // transit to SettingsActivity
    override fun onOptionsItemSelected(item: MenuItem) : Boolean {
        when (item.itemId) {
            R.id.settings -> {
                isDemo = true
                mAPI?.stopSpeak()
                val intent = Intent(application, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // transit to OperationActivity
    private fun setUpButton() {
        mainButton.setOnClickListener {
            Log.d(TAG, "mainButton.onClick")
            val startDemoMessage = sharedPref?.getString(getString(R.string.settings_item_talk_start_demo_message_key), "") ?: ""
            isDemo = true
            mAPI?.stopSpeak()
            isSpeak = true
            mAPI?.startSpeak(startDemoMessage, AudioManager.STREAM_MUSIC, true)
            val intent = Intent(application, OperationActivity::class.java)
            startActivityForResult(intent, OPERATION_BACK_CODE)
        }
    }

    // return from OperationActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == OPERATION_BACK_CODE && resultCode == RESULT_OK) {
            Log.d(TAG, "operation result = ${data?.getStringExtra(OPERATION_RESULT_KEY)}")

            val triangleMessage = sharedPref?.getString(getString(R.string.settings_item_talk_triangle_message_key), "") ?: ""
            val squareMessage = sharedPref?.getString(getString(R.string.settings_item_talk_square_message_key), "") ?: ""
            val circleMessage = sharedPref?.getString(getString(R.string.settings_item_talk_circle_message_key), "") ?: ""
            val crossMessage = sharedPref?.getString(getString(R.string.settings_item_talk_cross_message_key), "") ?: ""

            val message = when (data?.getStringExtra(OPERATION_RESULT_KEY)) {
                TRIANGLE_BUTTON -> triangleMessage
                SQUARE_BUTTON -> squareMessage
                CIRCLE_BUTTON -> circleMessage
                CROSS_BUTTON -> crossMessage
                else -> ""
            }
            Log.d(TAG, "msg=$message")
            mAPI?.stopSpeak()
            isSpeak = true
            mAPI?.startSpeak(message, AudioManager.STREAM_MUSIC, true)
        }
    }

    // idol motion timer task
    private fun newTimerTask()  {
        val idolIntervalMsec = sharedPref?.getString(getString(R.string.settings_item_motion_idol_interval_msec_key), "5000")?.toLong() ?: 5000

        scheduler = Executors.newSingleThreadScheduledExecutor()
        val task = Runnable {
            if (isMove || isMotion || isDemo) return@Runnable
            val r = (Math.random() * 16).roundToInt()
            if (r <= 13) {
                val file = String.format("BA_Bore_%02d.smd", r + 1)
                Log.d(TAG, "idol motion -> $file")
                isMotion = true
                mAPI?.startMotion(file)
            }
        }
        future = scheduler?.scheduleWithFixedDelay(task, 1000, idolIntervalMsec, TimeUnit.MILLISECONDS)
    }

    // mAPI callback
    override fun onInitialized(result: Boolean, reason: Int) {
        Log.d(TAG, "onInitialized($result)")
        if (!result) {
            mAPI?.release()
            mAPI = null

            Log.e(TAG, "ERROR: fail to initialize SDK service (fail init: $reason)")
            finish()
            return
        }
        mAPI?.setOnDetectHumanBodyListener()
        mAPI?.startInitializePosition(1000)
    }

    override fun onStartInitializePositionCompleted(result: Boolean) {
        mAPI?.startMotion("B_ACT-515.smd")
    }

    override fun onStartAngleOfCompleted(result: Boolean, angle: Int) {
        isMove = false
    }

    override fun onStartMotionCompleted(result: Boolean) {
        isMotion = false
    }
    override fun onStopMotionCompleted(result: Boolean) {
        Log.d(TAG, "onStopMotionCompleted willEyeClose=$willEyeClose")
        if (willEyeClose) {
            mAPI?.startEyePattern(0)
            mAPI?.startNeckColorChange(APIdefinitions.COLOR_OFF)
            willEyeClose = false
        }
    }

    override fun onStartSpeakCompleted(result: Boolean) {
        isSpeak = false
    }

    override fun onStopSpeakCompleted(result: Boolean) {
        isSpeak = false
    }

    override fun onDetectHumanBody(direction: IntArray?) {
        if (isMove || isMotion || isDemo) return
        val d = direction ?: return
        Log.d(TAG, "onDetectHumanBody ${d.joinToString()}}")
        val b = d.withIndex().map{(Math.pow(2.0, it.index.toDouble()) * it.value).toInt()}.reduce{l, r -> l + r}

        val turnAngle = sharedPref?.getString(getString(R.string.settings_item_motion_turn_angle_key), "60")?.toInt() ?: 60
        val turnDurationMsec = sharedPref?.getString(getString(R.string.settings_item_motion_turn_duration_msec_key), "2000")?.toInt() ?: 2000
        val greetMessage = sharedPref?.getString(getString(R.string.settings_item_talk_greet_message_key), "") ?: ""
        val talkIntervalSec = sharedPref?.getString(getString(R.string.settings_item_talk_interval_sec_key), "0")?.toInt() ?: 0

        fun greet() {
            val now = LocalDateTime.now()
            if (ChronoUnit.SECONDS.between(current, now) >= talkIntervalSec) {
                if (!isSpeak) {
                    Log.d(TAG, "say '$greetMessage'")
                    isSpeak = true
                    mAPI?.startSpeak(greetMessage, AudioManager.STREAM_MUSIC, true)
                }
                current = now
            }
        }

        when (b) {
            130, 131 -> {
                Log.d(TAG, "detect front")
                isMotion = true
                greet()
                mAPI?.startMotion("B_ACT-101.smd")
            }
            2, 6, 10, 14 -> {
                Log.d(TAG, "detect right")
                isMotion = true
                greet()
                mAPI?.startMotion("B_ACT-101.smd")
                mAPI?.startAngleOf(APIdefinitions.ANGLE_BODY, 1 * turnAngle, turnDurationMsec)
            }
            128, 160, 192, 224 -> {
                Log.d(TAG, "detect left")
                isMotion = true
                greet()
                mAPI?.startMotion("B_ACT-101.smd")
                mAPI?.startAngleOf(APIdefinitions.ANGLE_BODY, -1 * turnAngle, turnDurationMsec)
            }
        }
    }
}
