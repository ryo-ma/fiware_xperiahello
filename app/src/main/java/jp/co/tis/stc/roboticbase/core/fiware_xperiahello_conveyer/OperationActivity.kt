package jp.co.tis.stc.roboticbase.core.fiware_xperiahello_conveyer

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_operation.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class OperationActivity : AppCompatActivity(), Mixin {
    private var sharedPref : SharedPreferences? = null
    internal var client: MqttAndroidClient? = null
    private var options: MqttConnectOptions? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operation)
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        setUpView()
        setUpButton()
    }

    private fun setUpView() {
        hideSystemUI()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        setUpMQTT()
    }

    override fun onPause() {
        tearDownMQTT()
        Log.d(TAG, "onPause")
        super.onPause()
    }

    private fun setUpButton() {
        triangleButton.setOnClickListener {
            onClickButton(TRIANGLE_BUTTON)
        }
        squareButton.setOnClickListener {
            onClickButton(SQUARE_BUTTON)
        }
        circleButton.setOnClickListener {
            onClickButton(CIRCLE_BUTTON)
        }
        crossButton.setOnClickListener {
            onClickButton(CROSS_BUTTON)
        }
    }

    private fun onClickButton(key: String) {
        Log.d(TAG, "onClick $key")
        val intent = Intent()
        publish(key)
        intent.putExtra(OPERATION_RESULT_KEY, key)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun setUpMQTT() {
        val context = this
        val schema = sharedPref?.getBoolean(getString(R.string.settings_item_mqtt_use_ssl_key), false)?.let{if (it) "ssl" else "tcp"} ?: "tcp"
        val host = sharedPref?.getString(getString(R.string.settings_item_mqtt_host_key), "") ?: ""
        val port = sharedPref?.getString(getString(R.string.settings_item_mqtt_port_key), "") ?: ""
        val url = "$schema://$host:$port"

        val urlRe = Regex("""^(ssl|tcp)://[\w-.]+:\d+${'$'}""")
        if (!urlRe.matches(url)) {
            AlertDialog.Builder(context)
                    .setTitle("MQTT接続失敗")
                    .setMessage("不正なURL, url = $url")
                    .setPositiveButton("ok"){ _, _ ->
                    }.show()
            return
        }

        if (client == null) {
            client = object : MqttAndroidClient(baseContext, url, MqttClient.generateClientId()) {}
        }

        options = MqttConnectOptions()
        options?.isCleanSession = true
        sharedPref?.getString(getString(R.string.settings_item_mqtt_username_key), "")?.let {
            if (it.isNotEmpty()) {
                options?.userName = it
            }
        }
        sharedPref?.getString(getString(R.string.settings_item_mqtt_password_key), "")?.let {
            if (it.isNotEmpty()) {
                options?.password = it.toCharArray()
            }
        }
        client?.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(iMqttToken: IMqttToken) {
                Log.d(TAG, "connect success, url = $url")
            }

            override fun onFailure(iMqttToken: IMqttToken, throwable: Throwable) {
                Log.e(TAG, "connect failure $throwable")
                AlertDialog.Builder(context)
                        .setTitle("MQTT接続失敗")
                        .setMessage("url = $url, $throwable")
                        .setPositiveButton("ok"){ _, _ ->
                        }.show()
            }
        })
    }

    private fun tearDownMQTT() {
        client?.disconnect()
        client?.unregisterResources()
    }

    private fun publish(move: String) {
        val c = client ?: return
        val baseTopic = sharedPref?.getString(getString(R.string.settings_item_mqtt_base_topic_key), "") ?: ""
        if (c.isConnected) {
            val dt = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
            val msg = "${DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dt)}|button|$move"
            c.publish("$baseTopic/attrs", msg.toByteArray(), 0, false)
            Log.d(TAG, "published $msg")
        } else {
            Log.w(TAG, "not connected")
            AlertDialog.Builder(this)
                    .setTitle("MQTT未接続")
                    .setMessage("MQTTに接続していません")
                    .setPositiveButton("ok"){ _, _ ->
                    }.show()
        }
    }
}
