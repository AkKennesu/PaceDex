package com.poketch.core.sync

import com.google.android.gms.wearable.DataMap
import java.io.IOException

class FakeWearableTransport(
    var isConnected: Boolean = true
) : WearableTransport {

    var peerListener: (suspend (path: String, dataMap: DataMap) -> Unit)? = null
    var peerMessageListener: (suspend (path: String, data: ByteArray) -> Unit)? = null

    var reverseListener: (suspend (path: String, dataMap: DataMap) -> Unit)? = null
    var reverseMessageListener: (suspend (path: String, data: ByteArray) -> Unit)? = null

    override suspend fun putDataMap(path: String, dataMap: DataMap) {
        if (!isConnected) throw IOException("FakeWearableTransport: Disconnected")
        peerListener?.invoke(path, dataMap)
    }

    override suspend fun sendMessage(path: String, data: ByteArray) {
        if (!isConnected) throw IOException("FakeWearableTransport: Disconnected")
        peerMessageListener?.invoke(path, data)
    }

    override suspend fun getConnectedNodes(): List<ConnectedNodeInfo> {
        return if (isConnected) {
            listOf(ConnectedNodeInfo("peer-node-1", "Paired Test Node", isNearby = true))
        } else {
            emptyList()
        }
    }

    val reverseEndpoint: WearableTransport = object : WearableTransport {
        override suspend fun putDataMap(path: String, dataMap: DataMap) {
            if (!isConnected) throw IOException("FakeWearableTransport: Disconnected")
            reverseListener?.invoke(path, dataMap)
        }

        override suspend fun sendMessage(path: String, data: ByteArray) {
            if (!isConnected) throw IOException("FakeWearableTransport: Disconnected")
            reverseMessageListener?.invoke(path, data)
        }

        override suspend fun getConnectedNodes(): List<ConnectedNodeInfo> {
            return if (isConnected) {
                listOf(ConnectedNodeInfo("reverse-peer-1", "Reverse Paired Test Node", isNearby = true))
            } else {
                emptyList()
            }
        }
    }
}
