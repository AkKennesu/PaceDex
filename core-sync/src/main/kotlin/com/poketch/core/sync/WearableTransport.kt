package com.poketch.core.sync

import com.google.android.gms.wearable.DataMap

data class ConnectedNodeInfo(
    val id: String,
    val displayName: String,
    val isNearby: Boolean
)

interface WearableTransport {
    suspend fun putDataMap(path: String, dataMap: DataMap)
    suspend fun sendMessage(path: String, data: ByteArray)
    suspend fun sendMessageToNode(nodeId: String, path: String, data: ByteArray) {
        sendMessage(path, data)
    }
    suspend fun getConnectedNodes(): List<ConnectedNodeInfo>
    suspend fun getLocalNode(): ConnectedNodeInfo? = null
}

