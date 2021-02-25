package com.dsw.xposeddemo

import android.content.Context
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Created by Shuwen Dai on 2021/2/25
 */

const val logcatFilterKeyword = "daishuwen"   //logcat过滤关键词

fun logD(txt: String?) {
    XposedBridge.log("$logcatFilterKeyword ${txt ?: ""}")
}

fun logE(t: Throwable) {
    Log.e("Xposed", "$logcatFilterKeyword ${Log.getStackTraceString(t)}")
}

@JvmOverloads
fun hookFun(clsName: String, clsLoader: ClassLoader, funName: String, args: Array<Any>? = null, after: (params: XC_MethodHook.MethodHookParam) -> Unit) {
    when (args) {
        null -> {
            XposedHelpers.findAndHookMethod(clsName, clsLoader, funName, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                }

                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    after.invoke(param)
                }
            })
        }
        else -> {
            XposedHelpers.findAndHookMethod(clsName, clsLoader, funName, *args, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                }

                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    after.invoke(param)
                }
            })
        }
    }
}

/**
 * 加壳的apk需要用这个拿到壳的classLoader
 */
fun considerFindRealClassLoader(pkgClassLoader: ClassLoader, callback: (realClsLoader: ClassLoader) -> Unit) {
//        //360加固
//        XposedHelpers.findAndHookMethod("com.qihoo.util.StubAppxxxxxxxx", pkgClassLoader, "getNewAppInstance", Context::class.java, object : XC_MethodHook() {
//            @Throws(Throwable::class)
//            override fun afterHookedMethod(param: MethodHookParam?) {
//                logD("发现壳啦")
//                super.afterHookedMethod(param)
//            }
//        })

//        //百度加固
//        XposedHelpers.findAndHookMethod("com.baidu.protect.StubApplication", pkgClassLoader, "onCreate", object:  XC_MethodHook() {
//            @Throws(Throwable::class)
//            override fun afterHookedMethod(param: MethodHookParam?) {
//                logD("发现壳啦")
//                super.afterHookedMethod(param)
//            }
//        })

//        //腾讯乐固
//        XposedHelpers.findAndHookMethod("com.tencent.StubShell.TxAppEntry", pkgClassLoader, "attachBaseContext", object : XC_MethodHook() {
//            @Throws(Throwable::class)
//            override fun afterHookedMethod(param: MethodHookParam?) {
//                super.afterHookedMethod(param)
//                logD("发现壳啦")
//            }
//        })

//        //其他加固
//        XposedHelpers.findAndHookMethod("com.shell.SuperApplication", pkgClassLoader, "attachBaseContext", Context::class.java, object : XC_MethodHook() {
//            @Throws(Throwable::class)
//            override fun afterHookedMethod(param: MethodHookParam?) {
//                super.afterHookedMethod(param)
//                logD("发现壳啦")
//            }
//        })

    //hook加固后的包，首先hook attachBaseContext这个方法来获取context对象
    hookFun("com.stub.StubApp", pkgClassLoader, "attachBaseContext", arrayOf(Context::class.java)) {
        logD("发现壳啦")
        //获取到的参数args[0]就是360的Context对象，通过这个对象来获取classloader
        val context = it.args[0] as Context
        //获取360的classloader，之后hook加固后的就使用这个classloader
        val classLoader = context.classLoader
        callback.invoke(classLoader)
    }
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