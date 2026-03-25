package com.frontend.wear

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject

/**
 * 폰 앱에서 전송하는 DataItem(실시간 산책 통계)과 Message(이벤트 알림)를 수신하는 서비스.
 * - DataItem 경로: /walk/stats
 * - Message 경로: /notification/{dog_warning|danger_zone|footprint|badge}
 */
class WearListenerService : WearableListenerService() {

    companion object {
        private const val CH_DOG_WARNING = "dog_warning"
        private const val CH_DANGER = "danger_zone"
        private const val CH_FOOTPRINT = "footprint"
        private const val CH_BADGE = "badge"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    // ── 산책 통계 DataItem 수신 (1초마다) ────────────────────────────────
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == "/walk/stats"
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                WalkStatsHolder.update(
                    elapsedSeconds = dataMap.getInt("elapsedSeconds"),
                    distanceMeters = dataMap.getDouble("distanceMeters"),
                    calories = dataMap.getInt("calories"),
                    isWalking = dataMap.getBoolean("isWalking"),
                    isPaused = dataMap.getBoolean("isPaused"),
                )
            }
        }
    }

    // ── 이벤트 알림 Message 수신 ─────────────────────────────────────────
    override fun onMessageReceived(messageEvent: MessageEvent) {
        val json = runCatching { JSONObject(String(messageEvent.data)) }.getOrNull() ?: return
        when {
            messageEvent.path.endsWith("dog_warning") -> showDogWarningNotification(json)
            messageEvent.path.endsWith("danger_zone") -> showDangerZoneNotification(json)
            messageEvent.path.endsWith("footprint")   -> showFootprintNotification(json)
            messageEvent.path.endsWith("badge")        -> showBadgeNotification(json)
        }
    }

    // ── 알림 생성 ────────────────────────────────────────────────────────

    private fun showDogWarningNotification(json: JSONObject) {
        val dogName = json.optString("dogName", "강아지")
        val distance = json.optDouble("distance", 0.0).toInt()
        notify(
            channelId = CH_DOG_WARNING,
            notifId = 1001,
            title = "⚠️ 주의",
            text = "$dogName 이(가) ${distance}m 근처에 있어요",
        )
    }

    private fun showDangerZoneNotification(json: JSONObject) {
        val reason = json.optString("reason", "위험 요소")
        notify(
            channelId = CH_DANGER,
            notifId = 1002,
            title = "🚨 위험 구역",
            text = "근처에 '$reason' 신고가 있어요",
        )
    }

    private fun showFootprintNotification(json: JSONObject) {
        val placeName = json.optString("placeName", "이곳")
        notify(
            channelId = CH_FOOTPRINT,
            notifId = 1003,
            title = "🐾 발자국",
            text = "'$placeName'에 발자국을 남겼어요!",
        )
    }

    private fun showBadgeNotification(json: JSONObject) {
        val badgeName = json.optString("badgeName", "배지")
        notify(
            channelId = CH_BADGE,
            notifId = 1004,
            title = "🏅 배지 획득!",
            text = "'$badgeName' 배지를 획득했어요",
        )
    }

    private fun notify(channelId: String, notifId: Int, title: String, text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(notifId, notif)
    }

    // ── 알림 채널 생성 ────────────────────────────────────────────────────
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            CH_DOG_WARNING to "비선호 강아지 알림",
            CH_DANGER      to "위험 구역 알림",
            CH_FOOTPRINT   to "발자국 알림",
            CH_BADGE       to "배지 알림",
        ).forEach { (id, name) ->
            nm.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }
}
