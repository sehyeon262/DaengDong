package com.frontend.wear

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject

/**
 * 폰 앱에서 전송하는 Message를 수신하는 서비스.
 * - 산책 통계: /walk/stats (MessageClient, 1초마다)
 * - 이벤트 알림: /notification/{dog_warning|danger_zone|footprint|badge}
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

    // ── Message 수신 ─────────────────────────────────────────────────────
    override fun onMessageReceived(messageEvent: MessageEvent) {
        val json = runCatching { JSONObject(String(messageEvent.data)) }.getOrNull()
        val path = messageEvent.path
        android.util.Log.d("WearListener", "onMessageReceived: path=$path")

        when {
            // 코스 목록 수신
            path == "/walk/courses" -> json?.let {
                val array = it.optJSONArray("courses") ?: return@let
                val courses = mutableListOf<WatchCourse>()
                for (i in 0 until array.length()) {
                    val c = array.getJSONObject(i)
                    courses.add(
                        WatchCourse(
                            index = c.optInt("index", i),
                            type = c.optString("type", "FREE"),
                            name = c.optString("name", "코스"),
                            distanceKm = c.optDouble("distanceKm", 0.0),
                            durationMin = c.optInt("durationMin", 0),
                        )
                    )
                }
                CoursesHolder.update(courses)
                android.util.Log.d("WearListener", "코스 수신: ${courses.size}개")
            }
            // 산책 통계 수신 (DataClient 대신 MessageClient 사용)
            path == "/walk/stats" -> json?.let {
                val isWalking = it.optBoolean("isWalking", false)
                val elapsed = it.optInt("elapsedSeconds", 0)
                android.util.Log.d("WearListener", "walk/stats 수신: elapsed=$elapsed, isWalking=$isWalking")
                WalkStatsHolder.update(
                    elapsedSeconds = elapsed,
                    distanceMeters = it.optDouble("distanceMeters", 0.0),
                    calories = it.optInt("calories", 0),
                    isWalking = isWalking,
                    isPaused = it.optBoolean("isPaused", false),
                )
            }
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
            path == "/notification/footprint" -> json?.let { showFootprintNotification(it) }
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
