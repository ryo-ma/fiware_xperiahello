package jp.co.tis.stc.roboticbase.core.fiware_xperiahello

import android.app.Activity

interface TestHelper {
    fun getPrivateProperty(activity: Activity?, name: String): Any? {
        return activity?.let { a ->
            a::class.java.declaredFields.find { it.name == name }?.let {
                it.isAccessible = true
                it.get(a)
            }
        }
    }

    fun setPrivateProperty(activity: Activity?, name: String, arg: Any) {
        activity?.let { a ->
            a::class.java.declaredFields.find { it.name == name }?.let {
                it.isAccessible = true
                it.set(a, arg)
            }
        }
    }

    fun invokePrivateMethod(activity: Activity?, name: String): Any? {
        return activity?.let { a ->
            a::class.java.declaredMethods.find { it.name == name }?.let {
                it.isAccessible = true
                it.invoke(a)
            }
        }
    }
}