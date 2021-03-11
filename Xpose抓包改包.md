# Xpose抓包改包

## 功能
* **抓包**. 打印url, header, requestBody, repsonseBody等
* **修改请求**. 在请求**加签前修改**url, requestBody, header等

## 背景
【需求】模拟黑产, 在经纪人app最后上传实拍视频的时候“偷梁换柱”. 比如前面拍摄流程都是规规矩矩的进行, 最后上传的时候, 换成了自己事先准备好的视频链接. 
【难点】如果直接用Charles之类的软件修改网络请求的话, 会出现加签失败的问题. 


## 常见的加签做法
常见的加签做法是把Query Params(GET请求)或Request Body(POST请求)按照一个加密规则生成签名, 然后把这个签名加到header. 后端服务器收到请求后, 也按照相同的加密规则生成签名. 若后端生成的签名和前端提供的签名不一致, 则说明请求被篡改. 例如

1. 客户端发起

| Request Header | Request Body | 
| --- | --- | 
| sign: aaa | {"uid": "123"} |

Ps: aaa怎么来的? 在c代码把{"uid": "123"}+ salt加密得到.

2. Charles rewrite Request Body

3. 服务端收到

| Request Header | Request Body | 
| --- | --- | 
| sign: aaa | {"uid": "456"} |

4. 服务端

把{"uid": "456"}+salt(和客户端一样, 这个是事先约定好的)加密得到ccc. 发现ccc和header中的aaa不一致, 驳回请求. 

## 改包思路
通常签名会放到OkHttp的Interceptor中执行. 所以我们只要在加签前把请求修改掉就好了。 

回顾OkHttp的构造

```kotlin
    val builder = OkHttpClient.Builder().addInterceptor(...)
    val okHttpClient = builder.build()
```

那么hook builder的`build`方法, 在`build`方法执行前, 拿到用户自定义的第一个interceptor, hook它的`intercept`方法. (这个时间点是在加签前)

```kotlin
hookFun(interceptorName, clsLoader, "intercept", "okhttp3.Interceptor.Chain", object : MethodHookCallback() {
            override fun before(param: MethodHookParam) {
                //修改请求
            }

            override fun after(param: MethodHookParam) {
                //打印请求
            }
}
```

最终打印效果如下

```
2021-03-09 11:38:29.242 29940-11947/com.anjuke.android.newbroker I/Xposed: daishuwen ================================ Start ================================
2021-03-09 11:38:29.242 29940-11947/com.anjuke.android.newbroker D/Xposed: daishuwen ================================ Request ================================
2021-03-09 11:38:29.242 29940-11947/com.anjuke.android.newbroker V/Xposed: daishuwen request = Request{method=POST, url=https://api.anjuke.com/mobile-ajk-broker/3.0/wos/token?app=a-broker&_pid=29940&brokerId=1389002&fingerDeviceId=d270187be2274c98b45048fb64505b89&macid=4ad8e981a19c81c5&i=4ad8e981a19c81c5&m=Android-IN2010&uuid=9e70642a-7f95-4c08-8a7a-e02a67606832&sesrid=bc39d5ac-f823-4369-91a9-c8d5c1ee56b3&o=OnePlus8-user+11+++release-keys&token=08593cb4762b0376daf5b3bef5afb68310d270187b&qtime=20210309113829&cv=9.21&v=11&from=mobile&pm=debug&_chat_id=0, tags={}}
2021-03-09 11:38:29.242 29940-11947/com.anjuke.android.newbroker V/Xposed: daishuwen requestHeaders = sig: 321cb7c1931f135d090ccb7d96c891d2
    body_md5: 31dbe576b28bc00332efe59be300ae1b
    Accept: application/json
    nsign: 1000deacd9efb7d47864e32170108400a093015d0011002d23a48d9d
    AuthToken: 08593cb4762b0376daf5b3bef5afb68310d270187b
    key: ee20a1640951687026333adf2083d9b1
    get_md5: eec1404d553a4620ec2a7ad7389b184f015d0000
    nsign_uuid: 23a48d9d-d491-400b-9f0e-62ef5e57614b
    User-Agent: :d270187be2274c98b45048fb64505b89
    Content-Type: application/json; charset=utf-8
    Content-Length: 45
    Host: api.anjuke.com
    Connection: Keep-Alive
    Accept-Encoding: gzip
2021-03-09 11:38:29.242 29940-11947/com.anjuke.android.newbroker D/Xposed: daishuwen ================================ Request Body ================================
2021-03-09 11:38:29.242 29940-11947/com.anjuke.android.newbroker V/Xposed: daishuwen requestBody = {"brokerId":"1389002","type":"zhiNengShiKan"}
2021-03-09 11:38:29.242 29940-11947/com.anjuke.android.newbroker D/Xposed: daishuwen ================================ Response ================================
2021-03-09 11:38:29.242 29940-11947/com.anjuke.android.newbroker V/Xposed: daishuwen response = Response{protocol=h2, code=200, message=, url=https://api.anjuke.com/mobile-ajk-broker/3.0/wos/token?app=a-broker&_pid=29940&brokerId=1389002&fingerDeviceId=d270187be2274c98b45048fb64505b89&macid=4ad8e981a19c81c5&i=4ad8e981a19c81c5&m=Android-IN2010&uuid=9e70642a-7f95-4c08-8a7a-e02a67606832&sesrid=bc39d5ac-f823-4369-91a9-c8d5c1ee56b3&o=OnePlus8-user+11+++release-keys&token=08593cb4762b0376daf5b3bef5afb68310d270187b&qtime=20210309113829&cv=9.21&v=11&from=mobile&pm=debug&_chat_id=0}
2021-03-09 11:38:29.247 29940-11947/com.anjuke.android.newbroker V/Xposed: daishuwen contentType = application/json; charset=utf-8}
2021-03-09 11:38:29.247 29940-11947/com.anjuke.android.newbroker V/Xposed: daishuwen responseBody = {"status":"ok","data":{"token":"cmZpNFVlYnV5dzlGTEFCZ2lCVld4WVBsei9jPTplPTE2MTUyNjE3MDgmZj0xNjE1MjYxMTA4NDcxNy5tcDQmcj0yNTA3Mjc3MTU=","expired":"1615261708","file_id":"16152611084717.mp4","path":"\/16152611084717.mp4","bucket":"intelljauditvideo","app_id":"mqkJiHgFmsu","wos_url":"https:\/\/wosajk1.anjukestatic.com","type":"zhiNengShiKan"}}}
2021-03-09 11:38:29.247 29940-11947/com.anjuke.android.newbroker I/Xposed: daishuwen ================================ End ================================

```

## 遇到的问题&解决问题
1. 试过在Xpose工程中集成Okhttp, 然后写一个Request Body, 再把Xpose工程中写的Request Body替换掉app里的Request Body. 结果不可行. 因为虽然两个RequestBody的类是相同的, 但是他们两个是由不同的ClassLoader加载出来的, 互相不认识对方. 除了JDK里的Class, 其他的所有Class都不能共通. 最后实现是通过反射的方式去执行.


2. 打印Response Body的时候, 尝试直接调用`okhttp3.ResponseBody#string`, 这样确实能够正常打印Response Body, 但会导致app拿不到response. 原因是该方法执行完成后会close buffer.

```java
package okhttp3;
public abstract class ResponseBody implements Closeable {
    public final String string() throws IOException {
        BufferedSource source = this.source();

        String var3;
        try {
            Charset charset = Util.bomAwareCharset(source, this.charset());
            var3 = source.readString(charset);
        } finally {
            Util.closeQuietly(source);
        }
        return var3;
    }
}
```

最后是参考了`HttpLoggingInterceptor`的做法， 把buffer读成bype数组, 再用type数组构建一个新的Response Body, 替换掉原来的.

```kotlin
    logD("responseBody: ${String(bytes, getCharset(contentType))}")
    val responseBody = XposedHelpers.callStaticMethod("okhttp3.ResponseBody".toClass(clsLoader), "create", contentType, bytes)
```

## 总结
1. 找到OkhttpClient创建的位置, 找到第一个拦截器, 并且hook它的intercept方法, 在这里面执行修改请求和打印请求的操作. 
2. Xpose Module中操作app里的对象(除了JDK内置对象), 只能通过反射的方式去操作, 没有其它方式. [Xpose作者的回复](https://github.com/rovo89/XposedBridge/issues/125#issuecomment-250959873)
3. 该代码只针对用Okhttp的app.
4. [代码](./app/src/main/java/com/dsw/xposeddemo/hook/ModifyOkHttpRequestHook.kt)