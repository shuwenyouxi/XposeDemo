package com.dsw.xposeddemo.hook

import com.dsw.xposeddemo.*
import com.dsw.xposeddemo.logD
import com.dsw.xposeddemo.utils.IOUtils
import com.dsw.xposeddemo.utils.call
import com.dsw.xposeddemo.utils.get
import com.dsw.xposeddemo.utils.newInstance
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.http.HttpHeaders
import okio.Buffer
import java.io.InputStream
import java.lang.reflect.*
import java.nio.charset.Charset

/**
 * 加签前修改网络请求参数
 * Created by Shuwen Dai on 2021/2/23
 */
class BrokerModifyNetworkRequestHook : BaseHook() {

//    override var dstPkgName = "com.lianjia.beike"
    override var dstPkgName = "com.anjuke.android.newbroker"

    private var okHttpClientCls: Class<*>? = null
    private var okHttpBuilderCls: Class<*>? = null
    private var okHttpInterceptorCls: Class<*>? = null

    private val viewModel = NetworkRequestHookViewModel()

    override fun enter() {
        okHttpClientCls ?: run {
            okHttpClientCls = XposedHelpers.findClass("okhttp3.OkHttpClient", clsLoader)
        }

        okHttpBuilderCls ?: run {
            okHttpBuilderCls = XposedHelpers.findClass("okhttp3.OkHttpClient\$Builder", clsLoader)
        }

        okHttpInterceptorCls ?: run {
            okHttpInterceptorCls = XposedHelpers.findClass("okhttp3.Interceptor", clsLoader)
        }
        val callInterceptor = XposedHelpers.findClass("okhttp3.internal.http.CallServerInterceptor", clsLoader)
        XposedBridge.hookAllMethods(callInterceptor, "intercept", object : XC_MethodHook() {

            override fun afterHookedMethod(param: MethodHookParam) {
                super.afterHookedMethod(param)
                val copyRequest = param.args?.get(0)?.call("request")?.call("newBuilder")?.call("build")
                val headers = copyRequest?.call("headers")
                val requestBody = copyRequest?.call("body")
                logI("================================ Start ================================")
                logD("================================ Request ================================")
                logV("request = ${copyRequest.safeToString()}")
                logV("requestHeaders = ${headers.safeToString()}")
                requestBody?.also {
                    val buffer = "okio.Buffer".newInstance(clsLoader)
                    XposedHelpers.callMethod(requestBody, "writeTo", buffer)
                    logD("================================ Request Body ================================")
                    logV("requestBody = ${buffer.call("readUtf8").safeToString()}")
                }
                logD("================================ Response ================================")
                logV("response = ${param.result.safeToString()}")
                logI("================================ End ================================")
//                logD("responseHeaders = ${param.result.call("headers").safeToString()}")
            }
        })

//        XposedHelpers.findAndHookConstructor(okHttpClientCls, okHttpBuilderCls, object : XC_MethodHook() {
//            override fun beforeHookedMethod(param: MethodHookParam) {
//                super.beforeHookedMethod(param)
//                logD("Hook 到 构造函数  OkHttpClient")
//                addInterceptor(param)
//            }
//        })
    }


    /**
     * 尝试添加拦截器
     */
    @Synchronized
    private fun addInterceptor(param: XC_MethodHook.MethodHookParam) {
        val builderInstance = param.args?.getOrNull(0) ?: param.thisObject
        logD("1")
        builderInstance.javaClass.declaredFields.forEach { field ->
            if (field.type.name == List::class.java.name) {
                logD("2")
                val actualTypeArgument = ((field.genericType as? ParameterizedType)
                        ?.actualTypeArguments?.getOrNull(0) as? Class<*>)
                        ?: return@forEach
                if (actualTypeArgument.name == okHttpInterceptorCls?.name) {
                    logD("3")
                    field.isAccessible = true
                    val list = field.get(builderInstance) as ArrayList<in Any>
                    var dstItem: Any? = null
                    list.forEach {
                        logD("555555555 ${it.javaClass.name}")
                        if (it.javaClass.name == "com.anjuke.android.architecture.net.intercepter.BrokerInterceptor") {
                            dstItem = it
                        }
                    }

                    (dstItem as? Interceptor)?.also { brokerInterceptor ->

                    }
//
                    dstItem?.also {


                        val custom = Proxy.newProxyInstance(it.javaClass.classLoader, it.javaClass.interfaces) { proxy, method, args ->
                            method.invoke(AInterceptor(), args)
                        }
                        list[list.size - 1] = custom

//                        list.add(AInterceptor())
//                        logD("555555555 after: ${list.size}")
                    }


//                    if (isDstList) {
//                        logD("555555555 before: ${list.size}")
//                        Proxy.newProxyInstance(clsLoader, it.javaCl) { proxy, method, args ->
//
//                        }
//                        list.add(AInterceptor())
//                        logD("555555555 after: ${list.size}")
//                    }
                }
            }
        }
    }

    inner class NetworkRequestHookViewModel {
        private val UTF8 = Charset.forName("UTF-8")

        private val httpHeadersCls by lazy {
            XposedHelpers.findClass("okhttp3.internal.http.HttpHeaders", clsLoader)
        }

        /**
         * 打印response
         */
        fun printResponse(response: Any) {
            logD("555")
            val builder = response.call("newBuilder")
            val clone = builder!!.call("build")!!
            var responseBody = clone["body"]
            try {
                logD("<-- " + clone["code"] + ' ' + clone["message"] + ' ' + clone["request"]?.get("url"))
                val headers = clone["headers"]
                var i = 0
                val count = (headers?.call("size") as? Int) ?: 0
                while (i < count) {
                    logD("\t" + XposedHelpers.callMethod(headers, "name", i) + ": " + XposedHelpers.callMethod(headers, "value", i))
                    i++
                }
                logD(" ")
                logD("666")
                if (XposedHelpers.callStaticMethod(httpHeadersCls, "hasBody", clone) as Boolean) {
//                    if (responseBody == null)
//                        return
//                    if (isPlaintext(responseBody.call("contentType"))) {
//                        val bytes: ByteArray = IOUtils.toByteArray(XposedHelpers.callMethod(responseBody, "byteStream") as InputStream)
//                        val contentType = responseBody.call("contentType")
//                        val body = String(bytes, getCharset(contentType))
//                        logD("\tbody:$body")
//                        return
//                    } else {
//                        logD("\tbody: maybe [binary body], omitted!")
//                    }
                }
            } catch (e: Exception) {
                logE(e)
            } finally {
                logD("<-- END HTTP")
            }
        }

        /**
         * 打印response
         */
        fun printResponse1(clone: Response) {
            logD("555")
//        val builder = response.newBuilder()
//        val clone = builder.build()
            var responseBody = clone.body()
            try {
                logD("<-- " + clone.code() + ' ' + clone.message() + ' ' + clone.request().url())
                val headers = clone.headers()
                var i = 0
                val count = headers.size()
                while (i < count) {
                    logD("\t" + headers.name(i) + ": " + headers.value(i))
                    i++
                }
                logD(" ")
                if (HttpHeaders.hasBody(clone)) {
                    if (responseBody == null)
                        return
                    if (isPlaintext(responseBody.contentType())) {
                        val bytes: ByteArray = IOUtils.toByteArray(responseBody.byteStream())
                        val contentType = responseBody.contentType()
                        val body = String(bytes, getCharset(contentType))
                        logD("\tbody:$body")
                        return
                    } else {
                        logD("\tbody: maybe [binary body], omitted!")
                    }
                }
            } catch (e: Exception) {
                logE(e)
            } finally {
                logD("<-- END HTTP")
            }
        }

        private fun isPlaintext(mediaType: Any?): Boolean {
            if (mediaType == null) return false
            if (mediaType.call("type").safeToString() == "text") {
                return true
            }
            var subtype = mediaType.call("subtype") as? String
            if (subtype != null) {
                subtype = subtype.toLowerCase()
                if (subtype.contains("x-www-form-urlencoded") || subtype.contains("json") || subtype.contains("xml") || subtype.contains("html")) //
                    return true
            }
            return false
        }

        private fun getCharset(contentType: Any?): Charset {
            return contentType?.let { XposedHelpers.callMethod(contentType, "charset", UTF8) as Charset }
                    ?: UTF8
        }
    }
}
