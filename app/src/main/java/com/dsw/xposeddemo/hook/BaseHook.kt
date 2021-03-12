package com.dsw.xposeddemo.hook

import android.app.Application
import android.content.Context
import com.dsw.xposeddemo.MethodHookCallback
import com.dsw.xposeddemo.hookFun
import com.dsw.xposeddemo.logD
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Created by Shuwen Dai on 2021/3/2
 */
abstract class BaseHook : IXposedHookLoadPackage {

    abstract var dstPkgNameList: Array<String>

    var appContext: Context? = null

    lateinit var clsLoader: ClassLoader

    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!dstPkgNameList.contains(lpparam.packageName)) {
            return  //Hook的app包名过滤
        }
        logD("enter")
        considerFindRealClassLoader(lpparam.classLoader) { realClassLoader ->
            clsLoader = realClassLoader
            doBeforeEnter()
            enter()
        }
    }

    /**
     * 正式进入前执行一些通用代码
     */
    private fun doBeforeEnter() {
        if (appContext == null) {
            hookFun(Application::class.java, "attach", Context::class.java, object : MethodHookCallback() {
                override fun before(param: MethodHookParam) {
                }

                override fun after(param: MethodHookParam) {
                    appContext = param.args[0] as? Context
                }
            })
        }
    }

    abstract fun enter()


    /**
     * 加壳的apk需要用这个拿到壳的classLoader
     */
    private fun considerFindRealClassLoader(pkgClassLoader: ClassLoader, callback: (realClsLoader: ClassLoader) -> Unit) {
        //hook加固后的包，首先hook attachBaseContext这个方法来获取context对象
        try {
            hookFun("com.stub.StubApp", pkgClassLoader, "attachBaseContext", Context::class.java, object : MethodHookCallback() {
                override fun after(param: MethodHookParam) {
                    logD("发现壳啦")
                    //获取到的参数args[0]就是360的Context对象，通过这个对象来获取classloader
                    val context = param.args[0] as Context
                    appContext = context
                    //获取360的classloader，之后hook加固后的就使用这个classloader
                    callback.invoke(context.classLoader)
                }

                override fun before(param: MethodHookParam) {
                }
            })
        } catch (e: Throwable) {
            callback.invoke(pkgClassLoader)
        }
    }
}