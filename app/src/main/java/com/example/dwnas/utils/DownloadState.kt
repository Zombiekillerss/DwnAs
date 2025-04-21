package com.example.dwnas.utils

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Progress(val percent: Float, val eta: Long) : DownloadState()
    data class Success(val filePath: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
