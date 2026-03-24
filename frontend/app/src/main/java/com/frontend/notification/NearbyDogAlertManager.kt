package com.frontend.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.frontend.MainActivity
import com.frontend.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 비선호 강아지 알림 관리자
 * - Android 로컬 알림 발송 담당
 * - Android 8.0+ NotificationChannel 생성
 * - Android 13+ POST_NOTIFICATIONS 권한 확인
 */
@Singleton
class NearbyDogAlertManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID = "nearby_dog_alert"
        const val CHANNEL_NAME = "비선호 강아지 알림"
        const val CHANNEL_DESCRIPTION = "산책 중 비선호 강아지가 근처에 있을 때 알림을 받습니다"

        // 알림 ID 범위 (dogId 기반으로 생성)
        private const val NOTIFICATION_ID_BASE = 10000
    }

    private var channelCreated = false

    /**
     * NotificationChannel 생성 (Android 8.0+)
     * Application.onCreate()에서 호출
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !channelCreated) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            channelCreated = true
        }
    }

    /**
     * POST_NOTIFICATIONS 권한 필요 여부 확인 (Android 13+)
     */
    fun needsNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    /**
     * 알림 권한 보유 여부 확인
     * - Android 13 미만: 항상 true
     * - Android 13+: POST_NOTIFICATIONS 권한 확인
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 비선호 강아지 알림 발송
     * @param dogId 강아지 ID (알림 ID 생성용)
     * @param dogName 강아지 이름
     * @param distanceM 거리 (미터)
     * @return 알림 발송 성공 여부
     */
    fun showNearbyDogAlert(dogId: Long, dogName: String, distanceM: Double): Boolean {
        // 채널 생성 확인
        createNotificationChannel()

        // 권한 확인 (권한 없으면 silent fail)
        if (!hasNotificationPermission()) {
            android.util.Log.w("NearbyDogAlert", "알림 권한 없음 - 알림 발송 건너뜀")
            return false
        }

        return try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                dogId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("비선호 강아지 주의")
                .setContentText("비선호 강아지 ${dogName}이(가) 근처에 있어요.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 300, 200, 300))
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = NOTIFICATION_ID_BASE + (dogId % 1000).toInt()
            notificationManager.notify(notificationId, notification)

            android.util.Log.d("NearbyDogAlert", "알림 발송: dogId=$dogId, name=$dogName, distance=${distanceM.toInt()}m")
            true
        } catch (e: SecurityException) {
            android.util.Log.w("NearbyDogAlert", "알림 발송 실패 (권한 문제): ${e.message}")
            false
        } catch (e: Exception) {
            android.util.Log.e("NearbyDogAlert", "알림 발송 실패: ${e.message}", e)
            false
        }
    }

    /**
     * 특정 강아지 알림 취소
     */
    fun cancelAlert(dogId: Long) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = NOTIFICATION_ID_BASE + (dogId % 1000).toInt()
            notificationManager.cancel(notificationId)
        } catch (e: Exception) {
            android.util.Log.w("NearbyDogAlert", "알림 취소 실패: ${e.message}")
        }
    }

    /**
     * 모든 비선호 강아지 알림 취소 (산책 종료 시)
     */
    fun cancelAllAlerts() {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            // 채널의 모든 알림 취소 (Android 8.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)
                manager?.activeNotifications?.forEach { notification ->
                    if (notification.id in NOTIFICATION_ID_BASE..(NOTIFICATION_ID_BASE + 999)) {
                        notificationManager.cancel(notification.id)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("NearbyDogAlert", "전체 알림 취소 실패: ${e.message}")
        }
    }
}
