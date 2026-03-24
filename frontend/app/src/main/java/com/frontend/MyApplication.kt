package com.frontend

import android.app.Application
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import com.frontend.notification.NearbyDogAlertManager
import com.kakao.vectormap.KakaoMapSdk
import dagger.hilt.android.HiltAndroidApp
import java.security.MessageDigest
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application() {

    @Inject
    lateinit var nearbyDogAlertManager: NearbyDogAlertManager

    override fun onCreate() {
        super.onCreate()
        try {
            KakaoMapSdk.init(this, BuildConfig.KAKAO_MAP_API_KEY)
            Log.d("KakaoMap", "KakaoMapSdk.init() 성공")
        } catch (e: Exception) {
            // 에뮬레이터(x86_64)에서는 카카오맵 네이티브 라이브러리 미지원
            Log.e("KakaoMap", "KakaoMapSdk.init() 실패: ${e.message}", e)
        }

        // NotificationChannel 생성 (Android 8.0+)
        initNotificationChannels()

        if (BuildConfig.DEBUG) {
            printKeyHash()
        }
    }

    /**
     * 알림 채널 초기화
     * - Hilt 주입 전에 호출될 수 있으므로 lazy 처리
     */
    private fun initNotificationChannels() {
        try {
            // Hilt 주입이 완료된 후 채널 생성
            if (::nearbyDogAlertManager.isInitialized) {
                nearbyDogAlertManager.createNotificationChannel()
                Log.d("MyApplication", "NotificationChannel 생성 완료")
            } else {
                // 주입 전이면 직접 생성 (fallback)
                createNearbyDogAlertChannel()
            }
        } catch (e: Exception) {
            Log.w("MyApplication", "NotificationChannel 생성 실패: ${e.message}")
        }
    }

    /**
     * 비선호 강아지 알림 채널 직접 생성 (fallback)
     */
    private fun createNearbyDogAlertChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelId = "nearby_dog_alert"
            val channelName = "비선호 강아지 알림"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel(channelId, channelName, importance).apply {
                description = "산책 중 비선호 강아지가 근처에 있을 때 알림을 받습니다"
                enableVibration(true)
            }
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun printKeyHash() {
        try {
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                info.signingInfo?.apkContentsSigners ?: emptyArray()
            } else {
                @Suppress("DEPRECATION")
                (packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures ?: emptyArray())
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
