package jp.co.tis.stc.roboticbase.core.fiware_xperiahello

import android.support.v7.app.AppCompatActivity
import com.sonymobile.smartproduct.xperia_hello_sdk_clientapi.ClientAPI

open class ClientAPIImplActivity : AppCompatActivity(), ClientAPI.Callback {
    override fun onInitialized(result: Boolean, reason: Int) {
    }

    override fun onStartInitializePositionCompleted(result: Boolean) {
    }

    override fun onStartAngleOfCompleted(result: Boolean, angle: Int) {
    }

    override fun onStartMotionCompleted(result: Boolean) {
    }

    override fun onStartSpeakCompleted(result: Boolean) {
    }

    override fun onStopSpeakCompleted(result: Boolean) {
    }

    override fun onDetectHumanBody(direction: IntArray?) {
    }

    override fun onStopMotionCompleted(result: Boolean) {
    }

    override fun onPowerStatusChange(powermode: Int) {
    }

    override fun onExceedingLeanLimit(bodystate: Int) {
    }

    override fun onStartNeckColorChangeCompleted(result: Boolean) {
    }

    override fun onDetectMotionDisturbed() {
    }

    override fun onQueryAnglesCompleted(result: Boolean, angle: IntArray?) {
    }

    override fun onStartEyePatternCompleted(result: Boolean) {
    }

    override fun onPushHeadSwitch() {
    }
}