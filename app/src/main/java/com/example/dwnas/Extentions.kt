package com.example.dwnas

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.IOException

fun checkUriPermission(uri: Uri, context: Context): Boolean {
    val persistedUris = context.contentResolver.persistedUriPermissions
    return persistedUris.any { it.uri == uri && it.isWritePermission }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Copied Text", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Текст скопирован", Toast.LENGTH_SHORT).show()
}


fun getRootOfExternalStorage(file: File, context: Context): String {
    val path = file.absolutePath.toString()
    return path.replace("/Android/data/" + context.packageName + "/files", "")
}
fun isFolderExists(absPath: String): Boolean {
    val folder = File(absPath)
    return folder.exists() && folder.isDirectory
}

fun moveFileToMoviesFolder(context: Context, sourceUri: Uri, newFileUri: Uri) {
    try {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            context.contentResolver.openOutputStream(newFileUri)?.use { output ->
                input.copyTo(output)
                Log.d("MoveFile", "Файл перемещен в Movies/")
            }
        }
        context.contentResolver.delete(sourceUri, null, null)
    } catch (e: IOException) {
        Log.e("MoveFile", "Ошибка: ${e.message}")
    }
}
