package com.dsw.xposeddemo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dsw.xposeddemo.utils.ClassUtils

/**
 * Created by Shuwen Dai on 2021/2/24
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        val cls = Class.forName("java.lang.CharSequence")
//        val allImplCls = ClassUtils.getAllClassByInterface(cls)
//        Log.d("daishuwen", "size: ${allImplCls.size}")
//        allImplCls.forEach {
//            Log.d("daishuwen", "cls: ${it.name}")
//        }
//        Log.d("daishuwen", "2222")
    }
}