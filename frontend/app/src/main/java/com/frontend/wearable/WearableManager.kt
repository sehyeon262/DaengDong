package com.frontend.wearable

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 폰→워치 통신을 담당하는 래퍼 클래스.
 * - DataClient: 산책 통계 (1초마다)
 * - MessageClient: 이벤트 알림 (비선호 강아지, 배지, 제안, 발자국)
 */
@Singleton
class WearableManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val nodeClient: NodeClient = Wearable.getNodeClient(context)

    // Wearable API 미지원 기기 캐시 (API_UNAVAILABLE인 경우에만 캐시)
    private var apiUnavailable: Boolean = false

    // 포그라운드 메시지 수신 리스너 (WearableListenerService 백업)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val foregroundListener = MessageClient.OnMessageReceivedListener { event ->
        handleMessage(event)
    }

    private fun handleMessage(event: MessageEvent) {
        val path = event.path
        Log.d("PhoneWearable", "포그라운드 메시지 수신: path=$path, from=${event.sourceNodeId}")
        val json = runCatching { JSONObject(String(event.data)) }.getOrNull()
        when (path) {
            "/action/start_walk" -> scope.launch { WearableActionBus.emit(WearableAction.StartWalk) }
            "/action/end_walk"   -> scope.launch { WearableActionBus.emit(WearableAction.EndWalk) }
            "/action/pause_walk" -> scope.launch { WearableActionBus.emit(WearableAction.PauseWalk) }
            "/action/resume_walk"-> scope.launch { WearableActionBus.emit(WearableAction.ResumeWalk) }
            "/action/select_course" -> json?.let {
                val courseIndex = it.optInt("courseIndex", 0)
                scope.launch { WearableActionBus.emit(WearableAction.SelectCourse(courseIndex)) }
            }
            "/response/proposal_accept" -> json?.let {
                scope.launch {
                    WearableActionBus.emit(
                        WearableAction.ProposalAccept(
                            proposalId = it.optString("proposalId", ""),
                            myWalkRecordId = it.optLong("myWalkRecordId", 0L),
                        )
                    )
                }
            }
            "/response/proposal_reject" -> json?.let {
                scope.launch {
                    WearableActionBus.emit(
                        WearableAction.ProposalReject(
                            proposalId = it.optString("proposalId", ""),
                            myWalkRecordId = it.optLong("myWalkRecordId", 0L),
                        )
                    )
                }
            }
            "/action/dismiss_summary" -> scope.launch { WearableActionBus.emit(WearableAction.DismissWalkSummary) }
            "/response/stamp" -> json?.let {
                scope.launch {
                    WearableActionBus.emit(
                        WearableAction.Stamp(
                            walkId = it.optLong("walkId", 0L),
                            dogId = it.optLong("dogId", 0L),
                            placeId = it.optLong("placeId", 0L),
                        )
                    )
                }
            }
        }
    }

    /** 앱 포그라운드 진입 시 호출 — 직접 메시지 수신 등록 */
    fun registerForegroundListener() {
        try {
            messageClient.addListener(foregroundListener)
            Log.d("PhoneWearable", "포그라운드 MessageClient 리스너 등록 완료")
        } catch (e: Exception) {
            Log.e("PhoneWearable", "리스너 등록 실패: ${e.message}")
        }
    }

    /** 앱 종료/백그라운드 시 호출 */
    fun unregisterForegroundListener() {
        try {
            messageClient.removeListener(foregroundListener)
        } catch (_: Exception) {}
    }

    private suspend fun isAvailable(): Boolean {
        if (apiUnavailable) {
            Log.d("WearableManager", "isAvailable (캐시) = false (API 미지원 기기)")
            return false
        }
        return try {
            val nodes = nodeClient.connectedNodes.await()
            Log.d("WearableManager", "connectedNodes = ${nodes.map { "${it.id}(${it.displayName})" }}")
            true
        } catch (e: ApiException) {
            if (e.statusCode == 17) { // API_NOT_AVAILABLE
                apiUnavailable = true
                Log.d("WearableManager", "Wearable API 미지원 기기 — 워치 기능 비활성화")
            } else {
                Log.e("WearableManager", "ApiException statusCode=${e.statusCode}: ${e.message}")
            }
            false
        } catch (e: Exception) {
            Log.e("WearableManager", "isAvailable 예외: ${e.message}")
            false
        }
    }

    /**
     * 산책 통계를 DataClient로 워치에 전송 (1초마다 호출)
     */
    suspend fun sendWalkStats(
        elapsedSeconds: Int,
        distanceMeters: Double,
        calories: Int,
        isWalking: Boolean,
        isPaused: Boolean,
    ) {
        if (!isAvailable()) {
            Log.w("WearableManager", "sendWalkStats 스킵: Wearable 사용 불가")
            return
        }
        val json = JSONObject().apply {
            put("elapsedSeconds", elapsedSeconds)
            put("distanceMeters", distanceMeters)
            put("calories", calories)
            put("isWalking", isWalking)
            put("isPaused", isPaused)
        }
        sendMessageToAllNodes("/walk/stats", json)
        Log.d("WearableManager", "sendWalkStats 전송 완료: elapsed=$elapsedSeconds, isWalking=$isWalking")
    }

    /**
     * 일방향 알림 전송 (비선호 강아지, 배지 등)
     * 경로: /notification/{type}
     */
    suspend fun sendNotification(type: String, json: JSONObject) {
        if (!isAvailable()) return
        sendMessageToAllNodes("/notification/$type", json)
    }

    /**
     * 양방향 인터랙티브 메시지 전송 (산책 제안, 발자국 등)
     * 경로: /interactive/{type}
     */
    suspend fun sendInteractiveMessage(type: String, json: JSONObject) {
        if (!isAvailable()) return
        sendMessageToAllNodes("/interactive/$type", json)
    }

    /**
     * 코스 목록을 워치로 전송 (추천 경로 로드 완료 시 호출)
     */
    suspend fun sendCourseData(courses: List<CourseInfo>) {
        if (!isAvailable()) return
        val json = JSONObject()
        val array = org.json.JSONArray()
        courses.forEach { c ->
            array.put(JSONObject().apply {
                put("index", c.index)
                put("type", c.type)
                put("name", c.name)
                put("distanceKm", c.distanceKm)
                put("durationMin", c.durationMin)
            })
        }
        json.put("courses", array)
        sendMessageToAllNodes("/walk/courses", json)
        Log.d("WearableManager", "sendCourseData: ${courses.size}개 코스 전송")
    }

    data class CourseInfo(
        val index: Int,
        val type: String,
        val name: String,
        val distanceKm: Double,
        val durationMin: Int,
    )

    /**
     * 산책 종료 시 DataItem 삭제
     */
    suspend fun clearWalkStats() {
        if (!isAvailable()) return
        try {
            val uri = PutDataMapRequest.create("/walk/stats").uri
            dataClient.deleteDataItems(uri).await()
        } catch (e: Exception) {
            Log.e("WearableManager", "clearWalkStats 실패: ${e.message}")
        }
    }

    private suspend fun sendMessageToAllNodes(path: String, json: JSONObject) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                Log.w("WearableManager", "sendMessage($path) 스킵: 연결된 노드 없음")
                return
            }
            val data = json.toString().toByteArray()
            nodes.forEach { node ->
                messageClient.sendMessage(node.id, path, data).await()
                Log.d("WearableManager", "sendMessage 완료: path=$path → ${node.id}(${node.displayName})")
            }
        } catch (e: Exception) {
            Log.e("WearableManager", "sendMessage($path) 실패: ${e.message}")
        }
    }
}
