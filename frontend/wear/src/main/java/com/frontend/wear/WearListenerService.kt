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
        val json = runCatching { JSONObject(String(messageEvent.data)) }.getOrNull()
        val path = messageEvent.path

        when {
            // 오버레이 알림
            path.endsWith("dog_warning") -> json?.let {
                DogWarningHolder.show(
                    dogName = it.optString("dogName", "강아지"),
                    distance = it.optDouble("distance", 0.0).toInt(),
                )
            }
            path.endsWith("danger_zone") -> json?.let {
                DangerZoneHolder.show(reason = it.optString("reason", "위험 요소"))
            }
            path.endsWith("badge") -> json?.let {
                BadgeHolder.show(
                    badgeId = it.optLong("badgeId", 1L),
                    badgeName = it.optString("badgeName", "배지"),
                )
            }

            // 양방향 인터랙티브 — UI에 오버레이 표시
            path == "/interactive/proposal" -> json?.let {
                ProposalHolder.show(
                    proposalId = it.optString("proposalId", ""),
                    dogName = it.optString("dogName", ""),
                    breed = it.optString("breed", ""),
                    myWalkRecordId = it.optLong("myWalkRecordId", 0L),
                )
            }
            path == "/interactive/footprint" -> json?.let {
                FootprintHolder.show(
                    placeId = it.optLong("placeId", 0L),
                    placeName = it.optString("placeName", ""),
                    walkId = it.optLong("walkId", 0L),
                    dogId = it.optLong("dogId", 0L),
                )
            }

            // 발자국 알림 (일방향 — 도장 완료 확인)
            path.endsWith("footprint") -> json?.let { showFootprintNotification(it) }
        }
    }

    // ── 알림 생성 ────────────────────────────────────────────────────────

    private fun showFootprintNotification(json: JSONObject) {
        val placeName = json.optString("placeName", "이곳")
        notify(
            channelId = CH_FOOTPRINT,
            notifId = 1003,
            title = "🐾 발자국",
            text = "'$placeName'에 발자국을 남겼어요!",
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
