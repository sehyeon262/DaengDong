package com.frontend

import android.app.Application
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import com.kakao.vectormap.KakaoMapSdk
import dagger.hilt.android.HiltAndroidApp
import java.security.MessageDigest

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            KakaoMapSdk.init(this, BuildConfig.KAKAO_MAP_API_KEY)
        } catch (e: Exception) {
            // 에뮬레이터(x86_64)에서는 카카오맵 네이티브 라이브러리 미지원 - 무시
        }
        if (BuildConfig.DEBUG) {
            printKeyHash()
        }
    }

    private fun printKeyHash() {
        try {
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                info.signingInfo?.apkContentsSigners ?: emptyArray()
            } else {
                @Suppress("DEPRECATION")
                val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                info.signatures ?: emptyArray()
            }
            for (sig in signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(sig.toByteArray())
                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT))
            }
        } catch (e: Exception) {
            Log.e("KeyHash", "error: ${e.message}")
        }
    }
}
