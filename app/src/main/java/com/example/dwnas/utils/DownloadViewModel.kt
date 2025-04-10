package com.example.dwnas.utils

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData


@SuppressLint("StaticFieldLeak")
class DownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val _downloadState = MutableLiveData<DownloadState>()
    val downloadState: LiveData<DownloadState> = _downloadState

    private var boundService: MyDownloadService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            boundService = (service as MyDownloadService.LocalBinder).getService()
            boundService?.state?.asLiveData()?.observeForever {
                _downloadState.postValue(it)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
        }
    }

    fun startDownload(url: String, outputPath: String, fileName: String, maxQuality: Int) {
        val intent = Intent(getApplication(), MyDownloadService::class.java).apply {
            putExtra("url", url)
            putExtra("output path", outputPath)
            putExtra("file name", fileName)
            putExtra("max quality", maxQuality)
        }

        getApplication<Application>().startForegroundService(intent)
        try {
            getApplication<Application>().bindService(
                intent,
                connection,
                Context.BIND_AUTO_CREATE
            )
        } catch (e: Exception) {
            Log.d("myresult request", e.message.toString())

        }

    }

    override fun onCleared() {
        super.onCleared()
        boundService?.let {
            getApplication<Application>().unbindService(connection)
        }
    }
}