package com.dsw.xposeddemo.hook

import com.dsw.xposeddemo.*
import com.dsw.xposeddemo.utils.IOUtils
import de.robv.android.xposed.XposedHelpers
import org.json.JSONObject
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.HashSet

/**
 * 修改网络请求
 * Created by Shuwen Dai on 2021/2/23
 */
class ModifyOkHttpRequestHook : BaseHook() {

    override var dstPkgName = "com.anjuke.android.newbroker"

    private val UTF8 = Charset.forName("UTF-8")

    private val hasHookedInterceptorSet = Collections.synchronizedSet(HashSet<Class<*>>())   //已经被hook的interceptor集合

    private val dstPath = "/mobile-ajk-broker/3.0/pack/intellj-video-audit/audit/add"   //准备hook的接口path

    override fun enter() {
        //找到所有用户自定义interceptor
        XposedHelpers.findAndHookConstructor("okhttp3.OkHttpClient", clsLoader, "okhttp3.OkHttpClient.Builder", object : MethodHookCallback() {
            override fun before(param: MethodHookParam) {
                val builder = param.args?.getOrNull(0) ?: return
                val firstUserInterceptorCls = (builder["interceptors"] as? ArrayList<*>)?.getOrNull(0)?.javaClass
                        ?: return
                logD(firstUserInterceptorCls.name)

                if (hasHookedInterceptorSet.add(firstUserInterceptorCls)) {
                    hookInterceptor(firstUserInterceptorCls.name)   //找到第一个用户的自定义拦截器， 并且hook它
                } else {
                    logD("已存在")
                }
            }

            override fun after(param: MethodHookParam) {

            }
        })
    }

    /**
     * hook网络拦截器
     */
    private fun hookInterceptor(interceptorName: String) {
        hookFun(interceptorName, clsLoader, "intercept", "okhttp3.Interceptor.Chain", object : MethodHookCallback() {
            override fun before(param: MethodHookParam) {
                //request
                var request = param.args?.get(0)?.call("request") ?: return

                if ((request.call("url").call("uri") as? URI)?.path != dstPath)
                    return
                logD("发现目标请求")
                var requestBody = request?.call("body") ?: return
                val buffer = "okio.Buffer".newInstance(clsLoader)
                XposedHelpers.callMethod(requestBody, "writeTo", buffer)
                var requestBodyStr = buffer.call("readUtf8").safeToString()
                val contentType = requestBody.call("contentType")
                val subType = contentType.call("subtype").safeToString()
                logD("subType:$subType")
                when {
                    subType.contains("json") -> {
                        val json = JSONObject(requestBodyStr)
                        json.put("shuwen", "wakaka")
                        json.getJSONObject("videoInfo").put("videoUrl", "https://wosmedia1.anjukestatic.com/QYOnMlKjIQv/brokermediatransform/d145f481-b420-4eb6-a35f-73bad8ac43dc.1594708633804.51544-1638983443.1603703641908.mp4")
                        requestBody = XposedHelpers.callStaticMethod("okhttp3.RequestBody".toClass(clsLoader), "create", contentType, json.toString())
                    }

                    subType.contains("form") -> {
                        logD("form body修改待完成")
                    }
                }
                request = request
                        .call("newBuilder")
                        .call("post", requestBody)
                        .call("build")!!

                val field = "okhttp3.internal.http.RealInterceptorChain".toClass(clsLoader).getDeclaredField("request")
                field.isAccessible = true
                field.set(param.args[0], request)
            }

            override fun after(param: MethodHookParam) {
                //request
                val copyRequest = param.args?.get(0)?.call("request")?.call("newBuilder")?.call("build")
                val headers = copyRequest?.call("headers")
                val requestBody = copyRequest?.call("body")
                //response
                val copyResponse = param.result?.call("newBuilder")?.call("build")
                val copyResponseBody = copyResponse?.call("body")

                if ((copyRequest.call("url").call("uri") as? URI)?.path != dstPath)
                    return

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

                val contentType = copyResponseBody?.call("contentType")
//                if (isPlaintext(contentType)) {
                val bytes = IOUtils.toByteArray((copyResponseBody?.call("byteStream")) as InputStream)
                val body = String(bytes, getCharset(contentType))
                logV("contentType = ${contentType.safeToString()}}")
                logV("responseBody = $body}")
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
