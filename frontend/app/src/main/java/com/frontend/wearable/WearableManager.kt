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

    // Wearable API 사용 가능 여부 캐시 (API_UNAVAILABLE 로그 스팸 방지)
    private var available: Boolean? = null

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
            "/response/proposal_accept" -> json?.let {
                scope.launch {
                    val proposalId = it.optString("proposalId", "")
                    val myWalkRecordId = it.optLong("myWalkRecordId", 0L)
                    Log.d("PhoneWearable", "제안 수락: proposalId=$proposalId")
                }
            }
            "/response/proposal_reject" -> json?.let {
                scope.launch {
                    val proposalId = it.optString("proposalId", "")
                    Log.d("PhoneWearable", "제안 거절: proposalId=$proposalId")
                }
            }
            "/response/stamp" -> json?.let {
                Log.d("PhoneWearable", "발자국 스탬프: $it")
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
        available?.let { return it }
        return try {
            nodeClient.connectedNodes.await()
            available = true
            true
        } catch (e: ApiException) {
            if (e.statusCode == 17) { // API_NOT_AVAILABLE
                available = false
                Log.d("WearableManager", "Wearable API 미지원 기기 — 워치 기능 비활성화")
            }
            false
        } catch (_: Exception) {
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
        if (!isAvailable()) return
        try {
            val request = PutDataMapRequest.create("/walk/stats").apply {
                dataMap.putInt("elapsedSeconds", elapsedSeconds)
                dataMap.putDouble("distanceMeters", distanceMeters)
                dataMap.putInt("calories", calories)
                dataMap.putBoolean("isWalking", isWalking)
                dataMap.putBoolean("isPaused", isPaused)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
        } catch (e: Exception) {
            Log.e("WearableManager", "sendWalkStats 실패: ${e.message}")
        }
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
            val data = json.toString().toByteArray()
            nodes.forEach { node ->
                messageClient.sendMessage(node.id, path, data).await()
            }
        } catch (e: Exception) {
            Log.e("WearableManager", "sendMessage($path) 실패: ${e.message}")
        }
    }
}
