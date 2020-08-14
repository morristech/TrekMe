package com.peterlaurence.trekme.viewmodel.mapsettings

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.ui.maplist.events.*
import com.peterlaurence.trekme.util.ZipProgressionListener
import com.peterlaurence.trekme.util.makeThumbnail
import com.peterlaurence.trekme.util.stackTraceAsString
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import org.greenrobot.eventbus.EventBus
import java.io.IOException
import java.io.OutputStream

/**
 * The view-model for the [MapSettingsFragment].
 *
 * @author P.Laurence on 14/08/20
 */
class MapSettingsViewModel @ViewModelInject constructor(
        val app: Application
) : ViewModel() {

    private val _zipEvents = MutableLiveData<ZipEvent>()
    val zipEvents: LiveData<ZipEvent> = _zipEvents

    /**
     * Changes the thumbnail of a [Map].
     * Compression of the file defined by the [uri] is done off UI-thread.
     */
    fun setMapImage(map: Map, uri: Uri) = viewModelScope.launch {
        try {
            val thumbnail = withContext(Dispatchers.Default) {
                val outputStream = map.imageOutputStream
                if (outputStream != null) {
                    makeThumbnail(uri, app.contentResolver, map.thumbnailSize, outputStream)
                } else null
            }
            if (thumbnail != null) {
                map.image = thumbnail
                MapLoader.saveMap(map)
                EventBus.getDefault().post(MapImageImportResult(true))
            } else {
                EventBus.getDefault().post(MapImageImportResult(false))
            }
        } catch (e: Exception) {
            Log.e(TAG, e.stackTraceAsString())
            EventBus.getDefault().post(MapImageImportResult(false))
        }
    }

    /**
     * Start zipping a map and write the zip archive to the directory defined by the provided [uri].
     * Internally uses a [Flow] which only emits distinct events. Those events are listened by the
     * main activity, and not the [MapSettingsFragment], because the user might leave this view ;
     * we want to reliably inform the user when this task is finished.
     */
    fun saveMap(map: Map, uri: Uri) = viewModelScope.launch {
        val docFile = DocumentFile.fromTreeUri(app.applicationContext, uri)
        if (docFile != null && docFile.isDirectory) {
            val newFileName: String = map.generateNewNameWithDate() + ".zip"
            val newFile = docFile.createFile("application/zip", newFileName)
            if (newFile != null) {
                val uriZip = newFile.uri
                try {
                    val out: OutputStream = app.contentResolver.openOutputStream(uriZip)
                            ?: return@launch
                    /* The underlying task which writes into the stream is responsible for closing this stream. */
                    zipProgressFlow(map.id, out).distinctUntilChanged().collect {
                        _zipEvents.value = it
                    }
                } catch (e: IOException) {
                    Log.e(TAG, e.stackTraceAsString())
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    private fun zipProgressFlow(mapId: Int, outputStream: OutputStream): Flow<ZipEvent> = callbackFlow {
        val map = MapLoader.getMap(mapId) ?: return@callbackFlow

        val callback = object : ZipProgressionListener {
            private val mapName = map.name

            override fun fileListAcquired() {}

            override fun onProgress(p: Int) {
                offer(ZipProgressEvent(p, mapName, mapId))
            }

            override fun onZipFinished() {
                /* Use sendBlocking instead of offer to be sure not to lose those events */
                sendBlocking(ZipFinishedEvent(mapId))
                sendBlocking(ZipCloseEvent)
                channel.close()
            }

            override fun onZipError() {
                sendBlocking(ZipError)
                cancel()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            map.zip(callback, outputStream)
        }
        awaitClose()
    }
}

private const val TAG = "MapSettingsViewModel.kt"