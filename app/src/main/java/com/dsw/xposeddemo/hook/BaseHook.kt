package com.dsw.xposeddemo.hook

import android.app.Application
import android.content.Context
import com.dsw.xposeddemo.considerFindRealClassLoader
import com.dsw.xposeddemo.hookFun
import com.dsw.xposeddemo.logD
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Created by Shuwen Dai on 2021/3/2
 */
open abstract class BaseHook : IXposedHookLoadPackage {

    abstract var dstPkgName: String

    var appContext: Context? = null

    lateinit var clsLoader: ClassLoader

    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        logD("aaaa")
        if (lpparam.packageName != dstPkgName) return  //Hook的app包名过滤
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
        hookFun(Application::class.java, "attach", arrayOf(Context::class.java)) {
            appContext = it.args[0] as? Context
        }
    }

    open abstract fun enter()
}