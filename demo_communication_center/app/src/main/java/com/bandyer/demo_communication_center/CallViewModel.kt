package com.bandyer.demo_communication_center

import android.content.Context
import android.net.Uri
import com.bandyer.communication_center.call.Call
import com.bandyer.communication_center.call.CustomEvent
import com.bandyer.communication_center.call.OnCustomEventObserver
import com.bandyer.communication_center.file_share.*
import com.bandyer.communication_center.file_share.utils.dir_observer.DownloadDirObserver
import com.bandyer.demo_communication_center.model.DownloadAvailable
import com.bandyer.demo_communication_center.model.FileUploaded
import com.bandyer.sdk_design.filesharing.FileShareViewModel
import com.bandyer.sdk_design.filesharing.model.TransferData
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.util.*

class CallViewModel: FileShareViewModel() {

    override val itemsData: MutableMap<String, TransferData> = mutableMapOf()

    private val json = Json { isLenient = true; ignoreUnknownKeys = true; coerceInputValues = true }

    private var downloadDirObserver: DownloadDirObserver? = null

    private val onCustomEventObserver = object: OnCustomEventObserver {
        override val eventNames: List<String> = listOf(FILE_UPLOADED)

        override fun onEventReceived(event: CustomEvent) {
            kotlin.runCatching {
                val dataToParse = ((event.data as Array<*>)[0] as JSONObject).toString()
                if(event.eventName == FILE_UPLOADED) {
                    val data = json.decodeFromString<FileUploaded>(dataToParse)
                    val user = data.userAlias ?: data.senderName
                    mAvailableFileDownloads.tryEmit(DownloadAvailable(Uri.parse(data.url), user))
                }
            }
        }
    }

    private var call: Call? = null
    private var fileTransfer: Transfer? = null

    private val mAvailableFileDownloads = MutableSharedFlow<DownloadAvailable>(onBufferOverflow = BufferOverflow.DROP_OLDEST, replay = 1)
    var availableFileDownloads: Flow<TransferData>? = null
        private set

    private val mRemovedFiles = MutableSharedFlow<Uri>(onBufferOverflow = BufferOverflow.DROP_OLDEST, replay = 1)
    val removedFiles = mRemovedFiles.asSharedFlow()

    var fileUploads: Flow<TransferData>? = null
        private set
    var fileDownloads: Flow<TransferData>? = null
        private set

    fun init(context: Context, call: Call) {
        this.call = call

        fileTransfer = Transfer.FileUpload(Credentials.Sandbox(call.hostToken!!))
        fileUploads = fileTransfer!!.uploader.uploads
            .onEach { it.state.apply { if(this is FileTransfer.State.Success) notifyFileUploadSuccess(this.uri, it.info.sender) } }
            .map { it.mapToTransferData(context) }
        fileDownloads = fileTransfer!!.downloader.downloads
            .map { it.mapToTransferData(context) }
        availableFileDownloads = mAvailableFileDownloads
            .asSharedFlow()
            .map { it.mapToTransferData(context) }

        call.addCustomEventObserver(onCustomEventObserver)
        downloadDirObserver = DownloadDirObserver(context, fileTransfer!!.downloader.config.outputDirPath) { uri -> mRemovedFiles.tryEmit(uri) }
    }

    override fun uploadFile(context: Context, id: String, uri: Uri, sender: String) { fileTransfer?.uploader?.add(context, FileInfo.create(id, uri, sender)) }
    override fun cancelFileUpload(uploadId: String) { fileTransfer?.uploader?.cancel(uploadId) }

    override fun downloadFile(context: Context, id: String, uri: Uri, sender: String) { fileTransfer?.downloader?.add(context, FileInfo.create(id, uri, sender)) }
    override fun cancelFileDownload(downloadId: String) { fileTransfer?.downloader?.cancel(downloadId) }

    override fun cancelAllFileUploads() { fileTransfer?.downloader?.cancelAll() }
    override fun cancelAllFileDownloads() { fileTransfer?.downloader?.cancelAll() }

    private fun notifyFileUploadSuccess(uri: Uri, sender: String) {
        val confirm = JSONObject()
            .put("url", uri.toString())
            .put("userAlias", sender)
            .put("senderName", sender)
        call?.sendCustomEvent(CustomEvent(FILE_UPLOADED, confirm))
    }

    private fun FileTransfer.mapToTransferData(context: Context): TransferData {
        var progress = 0L
        var successUri: Uri? = null

        val state = when (val s = state) {
            is FileTransfer.State.Pending -> TransferData.State.Pending
            is FileTransfer.State.OnProgress -> {
                progress = s.progress
                TransferData.State.OnProgress
            }
            is FileTransfer.State.Success -> {
                progress = info.size
                successUri = s.uri
                TransferData.State.Success
            }
            is FileTransfer.State.Error -> TransferData.State.Error
            is FileTransfer.State.Cancelled -> TransferData.State.Cancelled
        }

        val type = if (this is Upload) TransferData.Type.Upload else TransferData.Type.Download

        return TransferData(
            context = context,
            id = info.id,
            uri = info.uri,
            sender = info.sender,
            creationTime = info.creationTime,
            bytesTransferred = progress,
            size = info.size,
            successUri = successUri,
            state = state,
            type = type
        )
    }

    private fun DownloadAvailable.mapToTransferData(context: Context) = TransferData(
        context = context,
        id = UUID.randomUUID().toString(),
        uri = uri,
        sender = user,
        state = TransferData.State.Available,
        type = TransferData.Type.Download
    )

    companion object {
        const val FILE_UPLOADED = "upload:new"
    }
}

