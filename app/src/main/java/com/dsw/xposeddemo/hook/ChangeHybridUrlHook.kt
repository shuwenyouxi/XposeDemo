package com.dsw.xposeddemo.hook

import android.os.Bundle
import com.dsw.xposeddemo.MethodHookCallback
import com.dsw.xposeddemo.hookFun

/**
 * 修改webview目标页面
 * Created by Shuwen Dai on 2021/2/23
 */
class ChangeHybridUrlHook : BaseHook() {

    override var dstPkgNameList = arrayOf("com.anjuke.android.app")

    override fun enter() {
        hookFun("com.anjuke.android.app.mainmodule.hybrid.HybridActivity", clsLoader, "onCreate", Bundle::class.java, object : MethodHookCallback() {
            override fun after(param: MethodHookParam) {
                hookLoadUrl()
            }

            override fun before(param: MethodHookParam) {
            }
        })
    }

    private fun hookLoadUrl() {
        hookFun("com.tencent.smtt.sdk.WebView", clsLoader, "loadUrl", String::class.java, object: MethodHookCallback() {
            override fun before(param: MethodHookParam) {
                param.args[0] = "http://www.soso.com"
            }

            override fun after(param: MethodHookParam) {
            }
        })
    }
}