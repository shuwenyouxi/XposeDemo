package com.dsw.xposeddemo.hook

import androidx.appcompat.app.AppCompatActivity
import com.dsw.xposeddemo.*
import dalvik.system.DexClassLoader
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.Field
import java.util.*

/**
 * 通过动态加载okhttp log库dex的方式打印网络请求
 *
 * Created by Shuwen Dai on 2021/2/23
 */
class PrintOkHttpByDynamicLoadDexHook : BaseHook() {

    override var dstPkgNameList = arrayOf(
            "com.lianjia.beike",
//            "com.anjuke.android.app",
//            "com.anjuke.android.newbroker",
    )

    //http log dex路径
    private val dexPath = "/storage/emulated/0/Download/logging-interceptor-3.12.10-jar2dex.dex"

    private var httpLoggingInterceptorCls: Class<*>? = null


    private val out by lazy {
        val file = File(appContext!!.filesDir, "bg.txt")
        FileOutputStream(file, true)
    }


    private val dexClsLoader by lazy {
        // 定义DexClassLoader
        // 第一个参数：是dex压缩文件的路径
        // 第二个参数：是dex解压缩后存放的目录
        // 第三个参数：是C/C++依赖的本地库文件目录,可以为null
        // 第四个参数：是上一级的类加载器

        val dexOutputDir = appContext?.getDir("dex", 0)?.absolutePath ?: null
        DexClassLoader(dexPath, dexOutputDir, null, clsLoader)
    }

    override fun enter() {
        considerLoadDex()
        httpLoggingInterceptorCls ?: run {
            logD("失败")
            return
        }

//        hookFun("okhttp3.logging.HttpLoggingInterceptor\$Logger", clsLoader, "log", String::class.java, object: MethodHookCallback() {
//            override fun before(param: MethodHookParam) {
//            }
//
//            override fun after(param: MethodHookParam) {
//                logD("ssssssss")
//            }
//        })


//        val out = FileOutputStream(file, true)
        XposedHelpers.getStaticObjectField("okhttp3.logging.HttpLoggingInterceptor.Logger".toClass(clsLoader), "DEFAULT")?.also {
            val loggerClsName = it::class.java.name
            hookFun(loggerClsName, clsLoader, "log", String::class.java, object : MethodHookCallback() {
                override fun before(param: MethodHookParam) {
                }

                override fun after(param: MethodHookParam) {
                    val txt = param.args[0].safeToString()
                    if (txt.startsWith("{") && txt.endsWith("}")) {
                        processLog(txt) { partTxt ->
                            logI(partTxt)
                        }
                    } else {
                        processLog(txt) { partTxt ->
                            logD(partTxt)
                        }
                    }

//                    if (txt.startsWith("{") && txt.endsWith("}")) {
//                        logD(txt)
//                        write(txt)
//                    }
                }
            })
        }

        hookFun("okhttp3.OkHttpClient.Builder", clsLoader, "build", object : MethodHookCallback() {
            override fun before(param: MethodHookParam) {
                val builder = param.thisObject ?: return
                (builder["interceptors"] as? ArrayList<*>)?.takeIf { it.isNotEmpty() } ?: return
                val logging = httpLoggingInterceptorCls?.newInstance() ?: return
                val levelBasic = "okhttp3.logging.HttpLoggingInterceptor\$Level".toClass(clsLoader)["HEADERS"]?: return
//                val levelBasic = "okhttp3.logging.HttpLoggingInterceptor\$Level".toClass(clsLoader)["BODY"]?: return
                logging.call("setLevel", levelBasic)
                builder.call("addInterceptor", logging)
                logD("大功告成")
            }

            override fun after(param: MethodHookParam) {
            }
        })

        //找到所有用户自定义interceptor
        XposedHelpers.findAndHookConstructor("okhttp3.OkHttpClient", clsLoader, "okhttp3.OkHttpClient.Builder", object : MethodHookCallback() {
            override fun before(param: MethodHookParam) {
                val builder = param.args?.getOrNull(0) ?: return
                (builder["interceptors"] as? ArrayList<*>)?.takeIf { it.isNotEmpty() } ?: return
                val logging = httpLoggingInterceptorCls?.newInstance() ?: return
                val levelBasic = "okhttp3.logging.HttpLoggingInterceptor\$Level".toClass(clsLoader)["BASIC"]
                        ?: return
                logging.call("setLevel", levelBasic)
                builder.call("addInterceptor", logging)
                logD("大功告成")
            }

            override fun after(param: MethodHookParam) {

            }
        })
    }

    @Synchronized
    private fun write(txt: String) {
        XSharedPreferences("com.dsw.xposeddemo", "daishuwen")
        if (!appContext!!.getSharedPreferences("daishuwen", AppCompatActivity.MODE_MULTI_PROCESS).getBoolean("record", true)) {
//        if (!XSharedPreferences("daishuwen").getBoolean("record", true)) {
            return
        }
        logD("sss 开始")
//        val file = File(appContext!!.filesDir, "bg.txt")
//        val out = FileOutputStream(file, true)
        out.write(txt.toByteArray())
        out.write("\n\n".toByteArray())
//        out.close()
        logD("sss 结束")
    }

    /**
     * 动态加载dex
     */
    private fun considerLoadDex() {
        httpLoggingInterceptorCls?.also {
            logD("已存在， 不用load dex")
            return
        }

        XposedHelpers.findClassIfExists("okhttp3.logging.HttpLoggingInterceptor", clsLoader)?.also {
            httpLoggingInterceptorCls = it
            logD("项目有现成的， 不用load dex")
            return
        }
        try {
            logD("开始动态加载dex")
            addElements()
            httpLoggingInterceptorCls = XposedHelpers.findClassIfExists("okhttp3.logging.HttpLoggingInterceptor", clsLoader)
        } catch (e: Exception) {
            logE(e)
        }
    }


    /**
     * @return Dex 是否合并 成功
     */
    private fun addElements(): Boolean {
        //自己的 classloader 里面的 element数组
        val myDexClassLoaderElements: Array<Any>? = getMyDexClassLoaderElements()
        if (myDexClassLoaderElements == null) {
            logE("AddElements  myDexClassLoaderElements null")
            return false
        } else {
            logE("AddElements  成功 拿到 myDexClassLoaderElements 自己的Elements 长度是   " + myDexClassLoaderElements.size)
        }
        //系统的  classloader 里面的 element数组
        val classLoaderElements: Array<Any>? = getClassLoaderElements()
        //将数组合并
        if (classLoaderElements == null) {
            logE("AddElements  classLoaderElements null")
            return false
        } else {
            logE("AddElements  成功 拿到 classLoaderElements 系统的Elements 长度是   " + classLoaderElements.size)
        }

        //DexElements合并
        val combined = java.lang.reflect.Array.newInstance(classLoaderElements.javaClass.componentType,
                classLoaderElements.size + myDexClassLoaderElements.size) as Array<Any>
        System.arraycopy(classLoaderElements, 0, combined, 0, classLoaderElements.size)
        System.arraycopy(myDexClassLoaderElements, 0, combined, classLoaderElements.size, myDexClassLoaderElements.size)


        //Object[] dexElementsResut = concat(myDexClassLoaderElements, classLoaderElements);
        if (classLoaderElements.size + myDexClassLoaderElements.size != combined.size) {
            logE("合并 elements数组 失败  null")
        }
        //合并成功 重新 加载
        return setDexElements(combined, myDexClassLoaderElements.size + classLoaderElements.size)
    }

    /**
     * 将自己 创建的 classloader 里面的 内容添加到 原来的 classloader里面
     */
    private fun getMyDexClassLoaderElements(): Array<Any>? {
        try {
            val pathListField: Field = dexClsLoader.javaClass.superclass.getDeclaredField("pathList")
            if (pathListField != null) {
                pathListField.isAccessible = true
                val dexPathList: Any = pathListField.get(dexClsLoader)
                val dexElementsField: Field? = dexPathList.javaClass.getDeclaredField("dexElements")
                if (dexElementsField != null) {
                    dexElementsField.isAccessible = true
                    val dexElements = dexElementsField.get(dexPathList) as Array<Any>
                    if (dexElements != null) {
                        return dexElements
                    } else {
                        logE("AddElements  获取 dexElements == null")
                    }
                    //ArrayUtils.addAll(first, second);
                } else {
                    logE("AddElements  获取 dexElements == null")
                }
            } else {
                logE("AddElements  获取 pathList == null")
            }
        } catch (e: NoSuchFieldException) {
            logE("AddElements  NoSuchFieldException   $e")
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            logE("AddElements  IllegalAccessException   $e")
            e.printStackTrace()
        }
        return null
    }

    /**
     * 获取系统的 classaLoder
     */
    private fun getClassLoaderElements(): Array<Any>? {
        try {
            val pathListField: Field = clsLoader.javaClass.superclass.getDeclaredField("pathList")
            if (pathListField != null) {
                pathListField.isAccessible = true
                val dexPathList = pathListField[clsLoader]
                val dexElementsField = dexPathList.javaClass.getDeclaredField("dexElements")
                if (dexElementsField != null) {
                    dexElementsField.isAccessible = true
                    val dexElements = dexElementsField[dexPathList] as Array<Any>
                    if (dexElements != null) {
                        return dexElements
                    } else {
                        logE("AddElements  获取 dexElements == null")
                    }
                    //ArrayUtils.addAll(first, second);
                } else {
                    logE("AddElements  获取 dexElements == null")
                }
            } else {
                logE("AddElements  获取 pathList == null")
            }
        } catch (e: NoSuchFieldException) {
            logE("AddElements  NoSuchFieldException   $e")
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            logE("AddElements  IllegalAccessException   $e")
            e.printStackTrace()
        }
        return null
    }

    /**
     * 将 Elements 数组 set回原来的 classloader里面
     *
     * @param dexElementsResut
     */
    private fun setDexElements(dexElementsResut: Array<Any>, conunt: Int): Boolean {
        try {
            val pathListField: Field = clsLoader.javaClass.getSuperclass().getDeclaredField("pathList")
            if (pathListField != null) {
                pathListField.isAccessible = true
                val dexPathList = pathListField[clsLoader]
                val dexElementsField = dexPathList.javaClass.getDeclaredField("dexElements")
                if (dexElementsField != null) {
                    dexElementsField.isAccessible = true
                    //先 重新设置一次
                    dexElementsField[dexPathList] = dexElementsResut
                    //重新 get 用
                    val dexElements = dexElementsField[dexPathList] as Array<Any>
                    return if (dexElements.size == conunt && Arrays.hashCode(dexElements) === Arrays.hashCode(dexElementsResut)) {
                        true
                    } else {
                        logE("合成   长度  " + dexElements.size + "传入 数组 长度   " + conunt)
                        logE("   dexElements hashCode " + Arrays.hashCode(dexElements).toString() + "  " + Arrays.hashCode(dexElementsResut))
                        false
                    }
                } else {
                    logE("SetDexElements  获取 dexElements == null")
                }
            } else {
                logE("SetDexElements  获取 pathList == null")
            }
        } catch (e: NoSuchFieldException) {
            logE("SetDexElements  NoSuchFieldException   $e")
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            logE("SetDexElements  IllegalAccessException   $e")
            e.printStackTrace()
        }
        return false
    }

    private fun processLog(fullTxt: String, realLogAction: (part: String)-> Unit) {
        val delta = fullTxt.length / 2000 + 1
        for (i in 0 until delta) {
            realLogAction.invoke(fullTxt.substring(i * 2000, Math.min((i+1)*2000, fullTxt.length)))
        }
    }
}
