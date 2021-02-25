package com.dsw.xposeddemo.hook;


import android.os.Bundle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static com.dsw.xposeddemo.FunExtKt.fetchMyMethods;
import static com.dsw.xposeddemo.FunExtKt.logD;
import static com.dsw.xposeddemo.FunExtKt.printFiled;

/**
 * hook webview的url
 */
public class AjkChangeWebViewHook implements IXposedHookLoadPackage {
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        if (lpparam.packageName != "com.anjuke.android.app") return;  //Hook的app包名过滤
        logD("enter");


        XposedHelpers.findAndHookMethod("com.anjuke.android.app.mainmodule.hybrid.HybridActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                XposedBridge.log("beforeHookedMethod");
            }

            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                XposedBridge.log("afterHookedMethod");
                Class cls = XposedHelpers.findClass("com.anjuke.android.app.mainmodule.hybrid.HybridActivity", lpparam.classLoader);

                //print all field
                for (Field field : cls.getFields()) {
                    printFiled(field, cls);
                }

                //该对象所有方法
                List<Method> methods = fetchMyMethods(param.thisObject.getClass());


                XposedHelpers.findAndHookMethod("com.tencent.smtt.sdk.WebView", lpparam.classLoader, "loadUrl", String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        param.args[0] = "http://www.soso.com";
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                    }
                });
            }
        });
    }


}