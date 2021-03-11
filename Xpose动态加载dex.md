# Xpose动态加载dex

## 功能
* 用Xpose手段给app集成Bugly, HttpLoggingInterceptor等三方库. 


## 步骤(以集成HttpLoggingInterceptor为例)
### 步骤一 下载dex文件到手机
通常我们没办法直接下载到三方库的dex, 我们可以先下载[HttpLoggingInterceptor](https://mvnrepository.com/artifact/com.squareup.okhttp3/logging-interceptor/3.12.10)的jar包, 再用dex-jar工具中的脚本把jar包转成dex文件, 并将其存入手机.

### 步骤二 构建DexClassLoader

```kotlin
    //http log dex路径
    private val dexPath = "/storage/emulated/0/Download/logging-interceptor-3.12.10-jar2dex.dex"

    private val dexClsLoader by lazy {
        // 定义DexClassLoader
        // 第一个参数：是dex压缩文件的路径
        // 第二个参数：是dex解压缩后存放的目录
        // 第三个参数：是C/C++依赖的本地库文件目录,可以为null
        // 第四个参数：是上一级的类加载器

        val dexOutputDir = appContext?.getDir("dex", 0)?.absolutePath ?: null
        DexClassLoader(dexPath, dexOutputDir, null, clsLoader)
    }
```

### 步骤三 拿到两个dexElements数组

通过反射, 获取到`DexClassLoader`中的`pathList`, 再获取到`pathList`中的`dexElements`.
同理, 再拿到系统classLoader的`dexElements`

```java
public class BaseDexClassLoader extends ClassLoader {
    private final DexPathList pathList;
}


final class DexPathList {
    private Element[] dexElements;  //这个就是我们要拿的
}
```

### 步骤四 合并这两个dexElements数组

```kotlin
        //DexElements合并
        val combined = java.lang.reflect.Array.newInstance(classLoaderElements.javaClass.componentType,
                classLoaderElements.size + myDexClassLoaderElements.size) as Array<Any>
        System.arraycopy(classLoaderElements, 0, combined, 0, classLoaderElements.size)
        System.arraycopy(myDexClassLoaderElements, 0, combined, classLoaderElements.size, myDexClassLoaderElements.size)

        //最后再用反射把`combined`替换掉系统`ClassLoader`里的`dexElements`
```

### 步骤五 反射执行HttpLogginInterceptor初始化

```kotlin
hookFun("okhttp3.OkHttpClient.Builder", clsLoader, "build", object: MethodHookCallback() {
    override fun before(param: MethodHookParam) {
        val builder = param.thisObject?:return
        (builder["interceptors"] as? ArrayList<*>)?.takeIf { it.isNotEmpty() }?:return
        val logging = httpLoggingInterceptorCls?.newInstance()?:return
        val levelBasic = "okhttp3.logging.HttpLoggingInterceptor\$Level".toClass(clsLoader)["HEADERS"]?:return
        logging.call("setLevel", levelBasic)
        builder.call("addInterceptor", logging)
        logD("大功告成")
    }

    override fun after(param: MethodHookParam) {
    }
})
```

## 总结
1. [代码](./app/src/main/java/com/dsw/xposeddemo/hook/PrintOkHttpByDynamicLoadDexHook.kt)