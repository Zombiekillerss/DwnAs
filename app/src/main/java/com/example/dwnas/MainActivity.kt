package com.example.dwnas

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.dwnas.utils.RequestMaker
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        results.entries.forEach { (permission, isGranted) ->
            when {
                isGranted -> {
                    // Разрешение получено
                }
                shouldShowRequestPermissionRationale(permission) -> {
                    // Показать объяснение
                    //showPermissionRationale(permission)
                }
                else -> {
                    // Пользователь отказал навсегда
                    //openAppSettings()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)
        requestRequiredPermissions()
        val bGet = findViewById<Button>(R.id.bGetLink)
        val bGetManifest = findViewById<Button>(R.id.bGetRequest)
        val bGetPlaylist = findViewById<Button>(R.id.bGetPlaylist)
        bGetPlaylist.setOnClickListener {
            val i = Intent(this, AllLinksActivity::class.java)
            startActivity(i)
        }
        bGetManifest.setOnClickListener {
            val i = Intent(this, ManifestActivity::class.java)
            startActivity(i)
        }
        bGet.setOnClickListener {
            val i = Intent(this, DownloaderOnLinkActivity::class.java)
            startActivity(i)
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = buildList {
            add(android.Manifest.permission.READ_MEDIA_VIDEO)
            add(android.Manifest.permission.READ_MEDIA_AUDIO)
            add(android.Manifest.permission.POST_NOTIFICATIONS)
            add(android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            add(android.Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK)
            add(android.Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC)
        }.toTypedArray()

        requestPermissions.launch(permissions)
    }
}
