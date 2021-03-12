package com.dsw.xposeddemo.hook

import com.dsw.xposeddemo.*
import com.dsw.xposeddemo.utils.IOUtils
import de.robv.android.xposed.XposedHelpers
import java.io.InputStream
import java.nio.charset.Charset

/**
 * 打印网络请求
 * Created by Shuwen Dai on 2021/2/23
 */
class PrintOkHttpHook : BaseHook() {

    override var dstPkgNameList = arrayOf(
            "com.lianjia.beike",
//            "com.anjuke.android.app",
//            "com.anjuke.android.newbroker",
    )

    private val UTF8 = Charset.forName("UTF-8")

    override fun enter() {

        hookFun("okhttp3.internal.http.CallServerInterceptor", clsLoader, "intercept", "okhttp3.Interceptor.Chain", object: MethodHookCallback() {
            override fun before(param: MethodHookParam) {
            }

            override fun after(param: MethodHookParam) {
                //request
                val rqBuilder = param.args?.get(0)?.call("request")?.call("newBuilder")
                rqBuilder.call("removeHeader", "Accept-Encoding")
                val copyRequest = rqBuilder?.call("build")
//                val copyRequest = param.args?.get(0)?.call("request")?.call("newBuilder")?.call("build")
                val headers = copyRequest?.call("headers")
                val requestBody = copyRequest?.call("body")
                //response
                val copyResponse = param.result?.call("newBuilder")?.call("build")
                val copyResponseBody = copyResponse?.call("body")

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

//                copyResponseBody?.call("string")?.also {
//                    logV("responseBody = $it}")
//                }

                val contentType = copyResponseBody?.call("contentType")
//                if (isPlaintext(contentType)) {
                val bytes = IOUtils.toByteArray((copyResponseBody?.call("byteStream")) as InputStream)
                val body = String(bytes, getCharset(contentType))
                logV("contentType = ${contentType.safeToString()}")
                logV("responseBody = $body")
                val responseBody = XposedHelpers.callStaticMethod("okhttp3.ResponseBody".toClass(clsLoader), "create", contentType, bytes)
                param.result = param.result?.call("newBuilder")?.call("body", responseBody)?.call("build")
//                } else {
//                    logV("\tbody: maybe [binary body], omitted!");
//                }



                logI("================================ End ================================")
            }
        })
    }

    private fun isPlaintext(mediaType: Any?): Boolean {
        if (mediaType == null) return false
        if ((mediaType.call("type") as? String) == "text") {
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
