package com.virtual.tensorhub

import android.app.Application
import android.util.Log
import com.amap.api.maps.MapsInitializer
import com.amap.api.location.AMapLocationClient

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // ✅ 手动注入 Key，防止读取不到 meta-data
            AMapLocationClient.setApiKey("bc2c93e35cac9d5d3007f54d76a155fd")
            MapsInitializer.updatePrivacyShow(this, true, true)
            MapsInitializer.updatePrivacyAgree(this, true)
            Log.d("AMapInit", "✅ 高德地图 SDK 初始化成功")
        } catch (e: Exception) {
            Log.e("AMapInit", "❌ 高德地图 SDK 初始化失败: ${e.message}")
        }
    }
}
