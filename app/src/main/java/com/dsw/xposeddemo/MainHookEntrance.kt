package com.dsw.xposeddemo

import com.dsw.xposeddemo.hook.*
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Created by Shuwen Dai on 2021/2/23
 */
class MainHookEntrance : IXposedHookLoadPackage {
    private val hookList = mutableListOf<IXposedHookLoadPackage>().apply {
//        add(ChangeHybridUrlHook())
//        add(ChangeGisHook())
//        add(PrintOkHttpHook())
//        add(ModifyOkHttpRequestHook())
        add(PrintCurlHook())
//        add(PrintOkHttpByDynamicLoadDexHook())
    }

    @Throws(Throwable::class)
    override fun handleLoadPackage(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
        hookList.forEach {
            it.handleLoadPackage(loadPackageParam)
        }
    }
}