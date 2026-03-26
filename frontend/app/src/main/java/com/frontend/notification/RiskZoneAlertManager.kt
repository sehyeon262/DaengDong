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
 * 위험장소 근접 알림 관리자
 * - 산책 중 내가 등록한 위험장소에 접근 시 알림 발송
 * - Android 8.0+ NotificationChannel 생성
 * - Android 13+ POST_NOTIFICATIONS 권한 확인
 */
@Singleton
class RiskZoneAlertManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID = "risk_zone_alert"
        const val CHANNEL_NAME = "위험장소 알림"
        const val CHANNEL_DESCRIPTION = "산책 중 내가 등록한 위험장소에 접근할 때 알림을 받습니다"

        // 알림 ID 범위 (riskReportId 기반으로 생성)
        private const val NOTIFICATION_ID_BASE = 30000
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
     * 위험장소 근접 알림 발송
     * @param riskReportId 위험장소 ID (알림 ID 생성용)
     * @param distanceM 거리 (미터)
     * @return 알림 발송 성공 여부
     */
    fun showRiskZoneAlert(riskReportId: Long, distanceM: Double): Boolean {
        // 채널 생성 확인
        createNotificationChannel()

        // 권한 확인 (권한 없으면 silent fail)
        if (!hasNotificationPermission()) {
            android.util.Log.w("RiskZoneAlert", "알림 권한 없음 - 알림 발송 건너뜀")
            return false
        }

        return try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                riskReportId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("위험장소 접근 알림")
                .setContentText("내가 등록한 위험 장소에 접근 중이에요.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 300, 200, 300))
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = NOTIFICATION_ID_BASE + (riskReportId % 1000).toInt()
            notificationManager.notify(notificationId, notification)

            android.util.Log.d("RiskZoneAlert", "알림 발송: riskReportId=$riskReportId, distance=${distanceM.toInt()}m")
            true
        } catch (e: SecurityException) {
            android.util.Log.w("RiskZoneAlert", "알림 발송 실패 (권한 문제): ${e.message}")
            false
        } catch (e: Exception) {
            android.util.Log.e("RiskZoneAlert", "알림 발송 실패: ${e.message}", e)
            false
        }
    }

    /**
     * 특정 위험장소 알림 취소
     */
    fun cancelAlert(riskReportId: Long) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = NOTIFICATION_ID_BASE + (riskReportId % 1000).toInt()
            notificationManager.cancel(notificationId)
        } catch (e: Exception) {
            android.util.Log.w("RiskZoneAlert", "알림 취소 실패: ${e.message}")
        }
    }

    /**
     * 모든 위험장소 알림 취소 (산책 종료 시)
     */
    fun cancelAllAlerts() {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)
                manager?.activeNotifications?.forEach { notification ->
                    if (notification.id in NOTIFICATION_ID_BASE..(NOTIFICATION_ID_BASE + 999)) {
                        notificationManager.cancel(notification.id)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("RiskZoneAlert", "전체 알림 취소 실패: ${e.message}")
        }
    }
}
