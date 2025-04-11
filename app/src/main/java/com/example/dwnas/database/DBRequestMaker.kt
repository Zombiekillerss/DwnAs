package com.example.dwnas.database

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class DBRequestMaker : ViewModel() {
    fun getListLinks(context: ComponentActivity, onResult: (List<ListItemLink>?) -> Unit) {
        val db = MainDb.getDb(context).getDao()
        db.getAllLinks().asLiveData().observe(context) {
            onResult(it)
        }
    }

    suspend fun getManifests(activity: ComponentActivity): List<ListItemManifests> =
        suspendCoroutine { continuation ->
            getListManifests(activity) { manifests ->
                continuation.resume(manifests ?: emptyList())
            }
        }

    suspend fun getLinks(activity: ComponentActivity): List<ListItemLink> =
        suspendCoroutine { continuation ->
            getListLinks(activity) { links ->
                continuation.resume(links ?: emptyList())
            }
        }

    fun getListManifests(context: ComponentActivity, onResult: (List<ListItemManifests>?) -> Unit) {
        val db = MainDb.getDb(context).getDao()
        db.getAllManifests().asLiveData().observe(context) {
            onResult(it)
        }
    }

    fun deleteAllLinks(context: ComponentActivity, onResult: (String) -> Unit) {
        val db = MainDb.getDb(context).getDao()
        db.deleteAllLinks()
        onResult("+")
    }

    fun deleteAllManifests(context: ComponentActivity, onResult: (String) -> Unit) {
        val db = MainDb.getDb(context).getDao()
        db.deleteAllManifests()
        onResult("+")
    }

    fun insertLink(context: ComponentActivity, link: ListItemLink) {
        val db = MainDb.getDb(context).getDao()
        db.insertLink(link)
    }

    fun insertManifest(context: ComponentActivity, manifest: ListItemManifests) {
        val db = MainDb.getDb(context).getDao()
        db.insertManifest(manifest)
    }
}