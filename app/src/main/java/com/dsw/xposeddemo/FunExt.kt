package com.dsw.xposeddemo

import android.content.Context
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Created by Shuwen Dai on 2021/2/25
 */

const val logcatFilterKeyword = "daishuwen"   //logcat过滤关键词

fun logV(txt: String?) {
    Log.v("Xposed", "$logcatFilterKeyword ${txt ?: ""}")
}

fun logD(txt: String?) {
    Log.d("Xposed", "$logcatFilterKeyword ${txt ?: ""}")
}

fun logI(txt: String?) {
    Log.i("Xposed", "$logcatFilterKeyword ${txt ?: ""}")
}

fun logW(txt: String?) {
    Log.w("Xposed", "$logcatFilterKeyword ${txt ?: ""}")
}

fun logE(t: Throwable) {
    Log.e("Xposed", "$logcatFilterKeyword ${Log.getStackTraceString(t)}")
}

fun logE(txt: String?) {
    Log.e("Xposed", "$logcatFilterKeyword ${txt ?: ""}")
}


@JvmOverloads
fun hookFun(clsName: String, clsLoader: ClassLoader, funName: String, vararg args: Any) {
    XposedHelpers.findAndHookMethod(clsName, clsLoader, funName, *args)
}

@JvmOverloads
fun hookFun(cls: Class<*>, funName: String, vararg args: Any) {
    XposedHelpers.findAndHookMethod(cls, funName, *args)
}


fun fetchMyMethods(cls: Class<*>): List<Method> {
    val list = arrayListOf<Method>()
    cls.methods.forEach {
        if (it.declaringClass == cls) {
            list.add(it)
        }
    }
    return list
}

fun printFiled(field: Field, cls: Class<*>) {
    try {
        val obj = cls.newInstance()
        field.isAccessible = true
        logD("fileName:" + field.name + ":" + field.get(obj))
    } catch (e: Exception) {
        logE(e)
    }
}

fun Any?.safeToString(): String = this?.toString() ?: "null"

operator fun Any.get(name: String): Any? = when (this) {
    is Class<*> -> try {
        getField(this, name)?.apply {
            isAccessible = true
        }?.get(null)
    } catch (e: NoSuchFieldException) {
        null
    }
    else -> try {
        getField(this.javaClass, name)?.apply {
            isAccessible = true
        }?.get(this)
    } catch (e: NoSuchFieldException) {
        null
    }
}

private fun getField(clazz: Class<*>, name: String): Field? = try {
    clazz.getDeclaredField(name)
} catch (e: NoSuchFieldException) {
    clazz.superclass?.let {
        getField(it, name)
    }
}

fun String.newInstance(clsLoader: ClassLoader): Any {
    return this.toClass(clsLoader).newInstance()
}

fun String.toClass(clsLoader: ClassLoader): Class<*> = XposedHelpers.findClass(this, clsLoader)


fun Any?.call(methodName: String, vararg args: Any): Any? {
    return XposedHelpers.callMethod(this, methodName, *args)
}

abstract class MethodHookCallback : XC_MethodHook() {
    abstract fun before(param: MethodHookParam)

    abstract fun after(param: MethodHookParam)

    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        super.beforeHookedMethod(param)
        before(param)
    }

    @Throws(Throwable::class)
    override fun afterHookedMethod(param: MethodHookParam) {
        super.afterHookedMethod(param)
        after(param)
    }
}
