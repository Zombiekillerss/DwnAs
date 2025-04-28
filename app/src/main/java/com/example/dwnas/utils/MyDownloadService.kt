package com.example.dwnas.utils
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.dwnas.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest


class MyDownloadService : Service() {
    private val channelId = "download_channel"
    private val binder = LocalBinder()
    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state

    inner class LocalBinder : Binder() {
        fun getService(): MyDownloadService = this@MyDownloadService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification("Загрузка начата..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("myresult request", "старт команда")

        val url = intent?.getStringExtra("url") ?: return START_NOT_STICKY
        val outputPath = intent.getStringExtra("output path") ?: filesDir.path
        val fileName = intent.getStringExtra("file name") ?: "video.mp4"
        val maxQuality = intent.getIntExtra("max quality", 720)
        startForeground(1, createNotification("Загрузка начата..."))
        CoroutineScope(Dispatchers.IO).launch {
            downloadVideo(url, outputPath, fileName, maxQuality)
            stopSelf()
        }

        return START_STICKY
    }

    private suspend fun downloadVideo(url: String, outputPath: String, fileName: String, maxQuality: Int) {
        Log.d("myresult request", "Загрузка видео")
        val text = outputPath.substring(outputPath.indexOf(":") + 1)

        val file = File("$text/$fileName.mp4")

        var request = YoutubeDLRequest(url).apply {
            addOption("-o", "$text/$fileName.mp4")
            addOption("-f", "best[height<=$maxQuality]")
            addOption("--merge-output-format", "mp4")
            addOption("--no-playlist")
            if(file.exists())
                addOption("--continue")
        }

        try {
            val test = YoutubeDL.getInfo(url)

            Log.d("myresult request", test.format.toString())
        }catch (e:Exception){
            Log.d("myresult request", "err123" + e.message.toString())
        }

        withContext(Dispatchers.IO) {
            var flag = true
            while(flag) {
                flag = false
                Log.d("myresult request", "$outputPath/$fileName")
                try {
                    YoutubeDL.getInstance().execute(request) { progress, seconds, message ->
                        updateNotification("data to dwn: [$message ::: $seconds ::: $progress]")
                    }
                } catch (e: Exception) {
                    flag = true
                    val message = e.message.toString()
                    Log.d("myresult request", message)
                    if(message.contains("Did not get any data blocks"))
                        flag = false
                    else
                        request = request.apply { addOption("--continue") }
                }
            }
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Downloader")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        Log.d("myresult request", "Обновление")
        Log.d("myresult request", text)
        val notification = createNotification(text)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, notification)
        val str = text.substring(text.indexOf('[') - 1, text.lastIndexOf(']'))
        val list = str.split(" ::: ")

        _state.update { DownloadState.Progress(list.last().toDouble(), list[list.size - 2].toLong()) }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Загрузки",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Уведомления о загрузке видео" }

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}