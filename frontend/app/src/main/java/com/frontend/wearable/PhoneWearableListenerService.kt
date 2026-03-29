package com.frontend.wearable

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject

class PhoneWearableListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("PhoneWearable", "서비스 onCreate - GMS가 서비스 시작함")
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        android.util.Log.d("PhoneWearable", "메시지 수신: path=$path, from=${messageEvent.sourceNodeId}")
        val json = runCatching {
            JSONObject(String(messageEvent.data))
        }.getOrNull()

        when (path) {
            "/action/start_walk" -> serviceScope.launch {
                WearableActionBus.emit(WearableAction.StartWalk)
            }
            "/action/end_walk" -> serviceScope.launch {
                WearableActionBus.emit(WearableAction.EndWalk)
            }
            "/action/pause_walk" -> serviceScope.launch {
                WearableActionBus.emit(WearableAction.PauseWalk)
            }
            "/action/resume_walk" -> serviceScope.launch {
                WearableActionBus.emit(WearableAction.ResumeWalk)
            }
            "/action/select_course" -> json?.let {
                val courseIndex = it.optInt("courseIndex", 0)
                serviceScope.launch {
                    WearableActionBus.emit(WearableAction.SelectCourse(courseIndex))
                }
            }
            "/action/dismiss_summary" -> serviceScope.launch {
                WearableActionBus.emit(WearableAction.DismissWalkSummary)
            }
            "/response/proposal_accept" -> json?.let {
                val proposalId = it.optString("proposalId", "")
                val myWalkRecordId = it.optLong("myWalkRecordId", 0L)
                if (proposalId.isBlank() || myWalkRecordId == 0L) return@let
                serviceScope.launch {
                    WearableActionBus.emit(WearableAction.ProposalAccept(proposalId, myWalkRecordId))
                }
            }
            "/response/proposal_reject" -> json?.let {
                val proposalId = it.optString("proposalId", "")
                val myWalkRecordId = it.optLong("myWalkRecordId", 0L)
                if (proposalId.isBlank() || myWalkRecordId == 0L) return@let
                serviceScope.launch {
                    WearableActionBus.emit(WearableAction.ProposalReject(proposalId, myWalkRecordId))
                }
            }
            "/response/stamp" -> json?.let {
                val walkId = it.optLong("walkId", 0L)
                val dogId = it.optLong("dogId", 0L)
                val placeId = it.optLong("placeId", 0L)
                if (walkId == 0L || dogId == 0L || placeId == 0L) return@let
                serviceScope.launch {
                    WearableActionBus.emit(WearableAction.Stamp(walkId, dogId, placeId))
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
