package com.example.dwnas

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.example.dwnas.utils.DownloadState
import com.example.dwnas.utils.DownloadViewModel
import com.example.dwnas.utils.RequestMaker
import java.io.File


class DownloaderOnLinkActivity : ComponentActivity() {

    private var progress: Int = 0
    private var textResult: String = ""

    private lateinit var downloadViewModel: DownloadViewModel
    private lateinit var requestMakerViewModel: RequestMaker


    private val currentLinks = mutableListOf<String>()
    private var currentUri: Uri? = null
    private var downloadUri: Uri? = null

    private lateinit var etPathToFile: EditText
    private lateinit var etPathToDownload: EditText
    private lateinit var etNameFile: EditText
    private lateinit var etURL: EditText

    private lateinit var tvSegments: TextView

    private lateinit var bDownloadAudio: Button
    private lateinit var bDownloadVideo: Button
    private lateinit var bSetPathDown: Button
    private lateinit var bPickPath: Button

    private val directoryPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                currentUri = it
                showFolderName(it)
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                saveDirectoryUri(it)
            }
        }

    private val downloadDirectoryPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                downloadUri = it
                showFolderName(it, true)
                saveDirectoryUri(it, true)
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.get_link_layout)
        initModels()
        subscribeUpdateData()
        initViews()
        initParameters()

        bDownloadVideo.setOnClickListener {
            if (currentUri != null && etURL.text.toString() != "" && etNameFile.text.toString() != "") {
                if(downloadUri == null)
                    downloadUri = Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                val currentFolder = getAbsolutePath(downloadUri!!)
                downloadViewModel.startDownload(
                    etURL.text.toString(),
                    currentFolder,
                    etNameFile.text.toString(),
                    720
                )
            } else {
                Toast.makeText(this, "Сначала выберите папку", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        bDownloadAudio.setOnClickListener {
            if (currentUri != null && etURL.text.toString() != "" && etNameFile.text.toString() != "") {
                try {
                    if(downloadUri == null)
                        downloadUri = Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                    val currentFolder = getAbsolutePath(downloadUri!!)

                    downloadViewModel.startDownload(
                        etURL.text.toString()+".mp4",
                        currentFolder,
                        etNameFile.text.toString(),
                        720
                    )
                } catch (e:Exception){
                    Log.d("myresult request", e.message.toString())
                }
            } else {
                Toast.makeText(this, "Сначала выберите папку", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        bPickPath.setOnClickListener {
            directoryPicker.launch(null)
        }
        bSetPathDown.setOnClickListener {
            downloadDirectoryPicker.launch(null)
        }
    }

    private fun initParameters() {
        val pref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val pathToFile = pref.getString("saved_directory_uri", null)
        val pathToDown = pref.getString("saved_download_directory_uri", null)
        currentUri = pathToFile?.let { Uri.parse(pathToFile) }
        downloadUri = pathToDown?.let { Uri.parse(pathToDown) }

        if(currentUri != null)
            showFolderName(currentUri!!)
        if(downloadUri != null)
            showFolderName(downloadUri!!, true)
    }

    private fun getAbsolutePath(
        uri: Uri
    ): String {
        val externalStorageFiles = getExternalFilesDirs(null)

        var currentFolder = ""
        for (file: File in externalStorageFiles) {
            val root = getRootOfExternalStorage(file, this)
            var str = uri.path.toString()
            str = str.substring(str.indexOf(':') + 1)
            currentFolder = "$root/$str"
            if (isFolderExists(currentFolder))
                return currentFolder
        }
        return currentFolder
    }

    private fun initModels() {
        downloadViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
        requestMakerViewModel = ViewModelProvider(this)[RequestMaker::class.java]
    }

    private fun initViews() {
        try {
            bSetPathDown = findViewById(R.id.bSetPathDown)
            etNameFile = findViewById(R.id.etNameFile)
            etURL = findViewById(R.id.etURL)
            etPathToFile = findViewById(R.id.etPathToFile)
            etPathToDownload = findViewById(R.id.etPathDownload)
            bPickPath = findViewById(R.id.bSetPathFile)
            bDownloadAudio = findViewById(R.id.bDownloadAudio)
            bDownloadVideo = findViewById(R.id.bDownloadVideo)
            requestMakerViewModel =
                ViewModelProvider(this@DownloaderOnLinkActivity)[RequestMaker::class.java]
            tvSegments = findViewById(R.id.tvSegments)
        } catch (e: Exception) {
            Log.d("myresult request", e.message.toString())
        }
    }

    private fun subscribeUpdateData() {
        downloadViewModel.downloadState.observe(this) { state ->
            when (state) {
                is DownloadState.Progress -> {
                    progress = state.percent.toInt()
                    textResult = "Загружено: ${state.percent.toInt()}%"
                    Log.d("myresult request", textResult)

                }

                is DownloadState.Success -> {
                    val currentFolder = getAbsolutePath(downloadUri!!)
                    textResult = "Файл сохранен: ${state.filePath}"
                    Log.d("myresult request", textResult)
                    Log.d("myresult request", "Пытаюсь перенести файл")
                    val trueFolder = "${getAbsolutePath(currentUri!!)}/${etNameFile.text}.mp4"
                    moveFileToMoviesFolder(this, Uri.parse(currentFolder), Uri.parse(trueFolder))
                    Log.d("myresult request", "файл перенесен")
                }

                is DownloadState.Error -> {
                    textResult = "Ошибка: ${state.message}"
                }

                else -> {}
            }
            Log.d("myresult request", progress.toString())
        }
    }


    private fun showFolderName(uri: Uri, isDownloadDir: Boolean = false) {
        val docId = DocumentsContract.getTreeDocumentId(uri)
        val split = docId.split(":")
        val displayName = when {
            split.size > 1 -> split[1]
            else -> docId
        }
        if(isDownloadDir){
            etPathToDownload.setText(displayName)
        }else {
            etPathToFile.setText(displayName)
        }
    }

    private fun saveDirectoryUri(uri: Uri, isDownloadDir: Boolean = false) {
        val path = uri.toString()
        val editor = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            .edit()
        if(!isDownloadDir) {
            editor.putString("saved_directory_uri", path)
                .apply()
        }else{
            editor.putString("saved_download_directory_uri", path)
                .apply()
        }
    }
}