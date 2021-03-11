# Xpose虚拟定位

## Xpose简介
* 【官方简介】Xposed是一个可以在不触及任何APK的情况下改变系统和应用程序的行为的模块框架（需要root）。
* 【功能】比如针对手机上已安装的一个app, 可以用xpose来调用方法、hook方法、修改字段等等。
* 【原理】安卓所有的进程的**父类**是`Zygote(孵化)进程`， 而Zygote对应的执行文件是`/system/bin/app_process`。Xpose原理就是他们自己写了一个`app_process`， 丢到`/system/bin/`下来取代原来的`app_process`。改造后的`app_process`会在每次调用方法的时候执行一个自己的方法， 以此达到hook方法的目的。
* 【官方教程】[How Xposed works](https://github.com/rovo89/XposedBridge/wiki/Development-tutorial)

## 修改定位核心代码

```kotlin
    XposedHelpers.findAndHookMethod("com.amap.api.location.AMapLocation", realClassLoader, "getLongitude", object : XC_MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodHookParam) {
            super.beforeHookedMethod(param)
        }

        @Throws(Throwable::class)
        override fun afterHookedMethod(param: MethodHookParam) {
            super.afterHookedMethod(param)
            param.result = (param.result as Double) + 偏移量    //偏移量 = 目标经度 - 实际经度， 只需要计算一次就可以
        }
    })
```

## 遇到问题
上面这个代码， 对于没加固的apk， 完美运行。 可若apk加固过了， 就报错了

```
2021-02-25 15:29:00.733 24238-24238/? E/EdXposed-Bridge: de.robv.android.xposed.XposedHelpers$ClassNotFoundError: java.lang.ClassNotFoundException: com.amap.api.location.AMapLocation
        at de.robv.android.xposed.XposedHelpers.findClass(XposedHelpers.java:71)
        at de.robv.android.xposed.XposedHelpers.findAndHookMethod(XposedHelpers.java:260)
```

## 分析问题

为什么明明有这个类， 却报ClassNotFoundError呢? 了解加固原理后， 得知app加壳后， 更改了classLoader， 因此我们用原来的classLoader会报类找不到的异常。
我们用的是360加固, 查看apk里的`AndroidManifest.xml`发现， application已经被替换成了`com.stub.StubApp`。

```xml
 <application
        android:theme="@ref/0x7f11010e"
        android:label="@ref/0x7f10009b"
        android:icon="@ref/0x7f0e0015"
        android:name="com.stub.StubApp"
        android:allowBackup="false"
        android:hardwareAccelerated="true"
        android:largeHeap="true"
        android:supportsRtl="true"
        android:usesCleartextTraffic="true"
        android:networkSecurityConfig="@ref/0x7f130006"
        android:appComponentFactory="androidx.core.app.CoreComponentFactory"
        android:requestLegacyExternalStorage="true">

```

而这个`com.stub.StubApp`在没有加壳的`classes.dex`文件中。

```java
package com.stub;

public final class StubApp extends Application {
    private static Application b;
      
    private static Application a(Context paramContext) {
        try {
            if (b == null) {
                ClassLoader classLoader = paramContext.getClassLoader();
                if (classLoader != null) {
                Class<?> clazz = classLoader.loadClass(strEntryApplication);
                if (clazz != null)
                    b = (Application)clazz.newInstance(); 
                } 
            } 
        } catch (Exception exception) {}
        return b;
    }

    protected final void attachBaseContext(Context paramContext) {
        //...省略很多代码
        b = a(paramContext);    
        //...省略很多代码
    }
}

```

原来在这里更换了classloader， 那我们拿这个更改后的classLoader， 应该就可以了。

```kotlin

//拓展方法， 拿到壳的classLoader
fun considerFindRealClassLoader(pkgClassLoader: ClassLoader, callback: (realClsLoader: ClassLoader) -> Unit) {
    XposedHelpers.findAndHookMethod("com.stub.StubApp", pkgClassLoader, "attachBaseContext", Context::class.java, object : XC_MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodHookParam) {
            super.beforeHookedMethod(param)
        }

        @Throws(Throwable::class)
        override fun afterHookedMethod(param: MethodHookParam) {
            super.afterHookedMethod(param)
            logD("发现壳啦")
            //获取到的参数args[0]就是360的Context对象，通过这个对象来获取classloader
            val context = it.args[0] as Context
            //获取360的classloader，之后hook加固后的就使用这个classloader
            val classLoader = context.classLoader
            callback.invoke(classLoader)
        }
    })
}

//最终代码如下
considerFindRealClassLoader(lpparam.classLoader) { realClassLoader ->
    XposedHelpers.findAndHookMethod("com.amap.api.location.AMapLocation", realClassLoader, "getLongitude", object : XC_MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodHookParam) {
            super.beforeHookedMethod(param)
        }

        @Throws(Throwable::class)
        override fun afterHookedMethod(param: MethodHookParam) {
            super.afterHookedMethod(param)
            param.result = 114.032524
        }
    })
}

```

## 总结
1. 市面上主流的加固软件， 如360加固， 腾讯乐固， 百度加固等， 他们都是加了一个壳application。 因此按照上述方法都解决hook不了的问题(需要自己结合对应的壳application代码做相应的改动)。
2. 该代码只针对用高德地图定位的app。
3. [代码](./app/src/main/java/com/dsw/xposeddemo/hook/ChangeGisHook.kt)