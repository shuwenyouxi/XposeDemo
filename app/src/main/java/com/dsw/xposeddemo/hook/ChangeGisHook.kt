package com.dsw.xposeddemo.hook

import com.dsw.xposeddemo.MethodHookCallback
import com.dsw.xposeddemo.hookFun
import com.dsw.xposeddemo.logD
import kotlin.math.abs

/**
 * 修改定位
 * Created by Shuwen Dai on 2021/2/23
 */
class ChangeGisHook : BaseHook() {

    override var dstPkgNameList = arrayOf("com.anjuke.android.newbroker")

    private val dstLongitude = 114.032524   //期望经度
    private val dstLatitude = 22.650382     //期望维度

    private var dstLonOffset: Double = 0.0         //期望经度偏移量
    private var dstLatOffset: Double = 0.0          //期望维度偏移量

    private val deviation: Double = 0.005           //允许误差

    override fun enter() {
        hookFun("com.amap.api.location.AMapLocation", clsLoader, "getLongitude", object : MethodHookCallback() {
            override fun before(param: MethodHookParam) {
            }

            override fun after(param: MethodHookParam) {
                val originLon = param.result as Double
                if (originLon == 0.0 || abs(originLon - dstLongitude) < deviation) {
                    return
                }
                if (dstLonOffset == 0.0) {
                    dstLonOffset = dstLongitude - originLon
                }
                param.result = originLon + dstLonOffset
                logD("getLongitude ${param.result}")
            }
        })

        hookFun("com.amap.api.location.AMapLocation", clsLoader, "getLatitude", object : MethodHookCallback() {
            override fun before(param: MethodHookParam) {
            }

            override fun after(param: MethodHookParam) {
                val originLat = param.result as Double
                if (originLat == 0.0 || abs(originLat - dstLatitude) < deviation) {
                    return
                }
                if (dstLatOffset == 0.0) {
                    dstLatOffset = dstLatitude - originLat
                }
                param.result = originLat + dstLatOffset
                logD("getLatitude ${param.result}")
            }
        })
    }
}