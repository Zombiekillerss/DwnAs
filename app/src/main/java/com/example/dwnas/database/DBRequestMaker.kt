package com.example.dwnas.database

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class DBRequestMaker : ViewModel() {
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

    fun addLink(context: ComponentActivity, link: ListItemLink, onResult: (String) -> Unit) {
        val db = MainDb.getDb(context).getDao()
        db.insertLink(link)
        onResult("+")
    }

    fun addManifest(context: ComponentActivity, manifest: ListItemManifests) {
        val db = MainDb.getDb(context).getDao()
        db.insertManifest(manifest)
    }

    private fun getListLinks(context: ComponentActivity, onResult: (List<ListItemLink>?) -> Unit) {
        val db = MainDb.getDb(context).getDao()
        db.getAllLinks().asLiveData().observe(context) {
            onResult(it)
        }
    }

    private fun getListManifests(context: ComponentActivity, onResult: (List<ListItemManifests>?) -> Unit) {
        val db = MainDb.getDb(context).getDao()
        db.getAllManifests().asLiveData().observe(context) {
            onResult(it)
        }
    }
}