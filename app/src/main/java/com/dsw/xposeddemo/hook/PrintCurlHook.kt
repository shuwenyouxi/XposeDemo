package com.dsw.xposeddemo.hook

import com.dsw.xposeddemo.*
import java.net.URI

/**
 * 打印网络请求curl
 * Created by Shuwen Dai on 2021/2/23
 */
class PrintCurlHook : BaseHook() {

    override var dstPkgNameList = arrayOf(
            "com.lianjia.beike",
//            "com.anjuke.android.app",
//            "com.anjuke.android.newbroker",
    )

    private val keywordPath = "/house/resblock/"  //目标打印path关键词

    override fun enter() {

        hookFun("okhttp3.internal.http.CallServerInterceptor", clsLoader, "intercept", "okhttp3.Interceptor.Chain", object: MethodHookCallback() {
            override fun before(param: MethodHookParam) {
            }

            override fun after(param: MethodHookParam) {
                //request
                val copyRequest = param.args?.get(0)?.call("request")?.call("newBuilder")?.call("build")

                logV(copyRequest.call("url").safeToString())

                if (!(copyRequest.call("url").call("uri") as? URI)?.path.safeToString().contains(keywordPath))
                    return

                val method = copyRequest.call("method").safeToString()
                if (method != "GET") {
                    return
                }
                var contentType = ""
                copyRequest.call("body")?.call("contentType")?.toString()?.also { type ->
                    contentType = "-H Content-Type: '$type'"
                }

                val headers = copyRequest?.call("headers")
                val url = copyRequest.call("url").safeToString()


                val headersBuilder = StringBuilder()
                for (i in 0 until (headers.call("size") as Int)) {
                    headersBuilder
                            .append("-H '" + headers.call("name", i).safeToString())
                            .append(": ")
                            .append(headers.call("value", i).safeToString()  + "' ")
                }



                var curl = "curl -X $method "
                contentType?.takeIf { it.isNotEmpty() }?.also {
                    curl += ("contentType" + " ")
                }
                curl += headersBuilder.toString()
                curl += "'$url'"
                logD(curl)
            }
        })
    }
}
