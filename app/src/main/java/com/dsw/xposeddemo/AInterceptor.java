package com.dsw.xposeddemo;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

import static com.dsw.xposeddemo.FunExtKt.logD;

/**
 * Created by Shuwen Dai on 2021/3/2
 */
public class AInterceptor implements Interceptor {
    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Log.e("daishuwen", "wakaka");
        logD("555555555 after: ${list.size}");
        return chain.proceed(chain.request());
    }
}
