package com.bandyer.demo_communication_center.utils

import com.bandyer.communication_center.file_share.Download
import com.bandyer.communication_center.file_share.Upload
import kotlinx.coroutines.flow.MutableStateFlow

sealed class MutableRegistry<T> : Registry<T> {
    class UploadRegistry: MutableRegistry<Upload>()
    class DownloadRegistry: MutableRegistry<Download>()

    override val records: MutableStateFlow<HashMap<String, T>> = MutableStateFlow(HashMap())

    override fun add(id: String, state: T) = sync {
        if(records.value.containsKey(id)) return@sync
        updateRecords(id, state)
    }

    override fun update(id: String, state: T) = sync {
        if(!records.value.containsKey(id)) return@sync
        updateRecords(id, state)
    }

    override fun remove(id: String) = sync {
        HashMap(records.value).apply {
            remove(id)
            records.value = this
        }
    }

    private fun updateRecords(id: String, state: T) {
        HashMap(records.value).apply {
            this[id] = state
            records.value = this
        }
    }

    private fun sync(code: () -> Unit) = synchronized(this) { code.invoke() }
}