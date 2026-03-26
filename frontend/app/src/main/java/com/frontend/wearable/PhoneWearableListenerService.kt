package com.frontend.wearable

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.EntryPointAccessors
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

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            WearableServiceEntryPoint::class.java,
        )
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
            "/response/proposal_accept" -> json?.let { handleProposalResponse(it, "ACCEPT") }
            "/response/proposal_reject" -> json?.let { handleProposalResponse(it, "REJECT") }
            "/response/stamp" -> json?.let { handleStamp(it) }
        }
    }

    private fun handleProposalResponse(json: JSONObject, action: String) {
        val proposalId = json.optString("proposalId", "")
        val myWalkRecordId = json.optLong("myWalkRecordId", 0L)
        if (proposalId.isBlank() || myWalkRecordId == 0L) return

        serviceScope.launch {
            entryPoint.walkRepository().respondToProposal(proposalId, action, myWalkRecordId)
        }
    }

    private fun handleStamp(json: JSONObject) {
        val walkId = json.optLong("walkId", 0L)
        val dogId = json.optLong("dogId", 0L)
        val placeId = json.optLong("placeId", 0L)
        if (walkId == 0L || dogId == 0L || placeId == 0L) return

        serviceScope.launch {
            entryPoint.placeRepository().stampPlace(walkId, dogId, placeId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
