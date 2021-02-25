package com.dsw.xposeddemo.hook

import com.dsw.xposeddemo.considerFindRealClassLoader
import com.dsw.xposeddemo.hookFun
import com.dsw.xposeddemo.logD
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlin.math.abs

/**
 * Created by Shuwen Dai on 2021/2/23
 */
class BrokerChangeGisHook : IXposedHookLoadPackage {

    private val dstLongitude = 114.032524   //期望经度
    private val dstLatitude = 22.650382     //期望维度

    private var dstLonOffset: Double = 0.0         //期望经度偏移量
    private var dstLatOffset: Double = 0.0          //期望维度偏移量

    private val deviation: Double = 0.005           //允许误差

    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.anjuke.android.newbroker") return  //Hook的app包名过滤
        logD("enter")
        considerFindRealClassLoader(lpparam.classLoader) { realClassLoader ->
            hookFun("com.amap.api.location.AMapLocation", realClassLoader, "getLongitude") {
                val originLon = it.result as Double
                if (originLon == 0.0 || abs(originLon - dstLongitude) < deviation) {
                    return@hookFun
                }
                if (dstLonOffset == 0.0) {
                    dstLonOffset = dstLongitude - originLon
                }
                it.result = originLon + dstLonOffset
                logD("getLongitude ${it.result}")
            }
            hookFun("com.amap.api.location.AMapLocation", realClassLoader, "getLatitude") {
                val originLat = it.result as Double
                if (originLat == 0.0 || abs(originLat - dstLatitude) < deviation) {
                    return@hookFun
                }
                if (dstLatOffset == 0.0) {
                    dstLatOffset = dstLatitude - originLat
                }
//                it.result = originLat + dstLatOffset
                logD("getLatitude ${it.result}")
            }
        }
    }
}