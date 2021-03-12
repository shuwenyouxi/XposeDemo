package com.dsw.xposeddemo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import de.robv.android.xposed.XSharedPreferences

/**
 * Created by Shuwen Dai on 2021/2/24
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btnStartRecord).setOnClickListener {
            getSharedPreferences("daishuwen", MODE_WORLD_READABLE).edit().putBoolean("record", true).apply()
//            XSharedPreferences("daishuwen").edit().putBoolean("record", true).apply()
        }
        findViewById<View>(R.id.btnStopRecord).setOnClickListener {
            getSharedPreferences("daishuwen", MODE_WORLD_READABLE).edit().putBoolean("record", false).apply()
//            XSharedPreferences("daishuwen").edit().putBoolean("record", false).apply()
        }
    }
}