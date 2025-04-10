package com.example.dwnas.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File
import java.io.IOException

class MediaFileWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val inputUri = inputData.getString(KEY_INPUT_URI) ?: return Result.failure()
        val outputUri = inputData.getString(KEY_OUTPUT_URI) ?: return Result.failure()
        val outputType = inputData.getString(KEY_OUTPUT_TYPE) ?: "mp4"
        val quality = inputData.getInt(KEY_OUTPUT_QUALITY, 720)

        val tempFile = File.createTempFile("download", ".tmp", applicationContext.cacheDir)

        when (outputType.lowercase()) {
            "mp3" -> downloadAsMp3(Uri.parse(inputUri), tempFile)
            "mp4" -> downloadAsMp4(Uri.parse(inputUri), tempFile, quality)
            else -> throw IllegalArgumentException("Unsupported output type")
        }

        // Копируем временный файл в конечное местоположение
        applicationContext.contentResolver.openOutputStream(Uri.parse(outputUri))?.use { output ->
            tempFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }

        Result.success(
            Data.Builder()
                .putString(KEY_OUTPUT_FILE, outputUri)
                .build()
        )
        // Здесь ваша логика создания файлов
        return Result.success()
    }

    private fun downloadAsMp3(inputUri: Uri, outputFile: File) {
//        val extractor = MediaExtractor().apply {
//            setDataSource(applicationContext, inputUri, null)
//        }

        // Реализация аналогична предыдущему примеру, но только для аудио
        // ...
    }

    private fun downloadAsMp4(inputUri: Uri, outputFile: File, quality: Int) {
//        val muxer = MediaMuxer(outputFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
//        val extractor = MediaExtractor().apply {
//            setDataSource(applicationContext, inputUri, null)
//        }

        val command = """
            -i $inputUri 
            -filter_complex "[v:0]scale=-1:$quality[vid]" 
            -map "[vid]" -map 0:a:0 
            -c:v libx264 -crf 23 -c:a copy 
            $${outputFile.absolutePath}
        """.trimIndent()
    }

    companion object {
        const val KEY_INPUT_URI = "input_uri"
        const val KEY_OUTPUT_URI = "output_uri"
        const val KEY_OUTPUT_TYPE = "output_type"
        const val KEY_OUTPUT_QUALITY = "output_quality"
        const val KEY_OUTPUT_FILE = "output_file"
        const val KEY_ERROR = "error"

        fun makeInputData(
            inputUri: String,
            outputUri: String,
            outputType: String
        ): Data {
            return Data.Builder()
                .putString(KEY_INPUT_URI, inputUri)
                .putString(KEY_OUTPUT_URI, outputUri)
                .putString(KEY_OUTPUT_TYPE, outputType)
                .build()
        }
    }
}

fun scheduleMediaFileCreation(context: Context) {
    val workRequest = OneTimeWorkRequestBuilder<MediaFileWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueue(workRequest)
}