package com.poketch.core.sync

import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataMapRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayServicesWearableTransport @Inject constructor(
    private val dataClient: DataClient,
    private val messageClient: MessageClient,
    private val nodeClient: NodeClient
) : WearableTransport {

    override suspend fun putDataMap(path: String, dataMap: DataMap) {
        val request = PutDataMapRequest.create(path).apply {
            this.dataMap.putAll(dataMap)
        }
        dataClient.putDataItem(request.asPutDataRequest().setUrgent()).await()
    }

    override suspend fun sendMessage(path: String, data: ByteArray) {
        val nodes = nodeClient.connectedNodes.await()
        for (node in nodes) {
            messageClient.sendMessage(node.id, path, data).await()
        }
    }

    override suspend fun sendMessageToNode(nodeId: String, path: String, data: ByteArray) {
        messageClient.sendMessage(nodeId, path, data).await()
    }

    override suspend fun getConnectedNodes(): List<ConnectedNodeInfo> {
        return runCatching {
            nodeClient.connectedNodes.await().map {
                ConnectedNodeInfo(
                    id = it.id,
                    displayName = it.displayName,
                    isNearby = it.isNearby
                )
            }
        }.getOrElse { emptyList() }
    }

    override suspend fun getLocalNode(): ConnectedNodeInfo? {
        return runCatching {
            val node = nodeClient.localNode.await()
            ConnectedNodeInfo(
                id = node.id,
                displayName = node.displayName,
                isNearby = node.isNearby
            )
        }.getOrNull()
    }
}
