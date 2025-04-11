package com.example.dwnas.database

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

    suspend fun deleteLink(activity: ComponentActivity,link: ListItemLink): String =
        suspendCoroutine { continuation ->
            deleteCurrentLink(activity, link) { links ->
                continuation.resume(links)
            }
        }

    suspend fun deleteManifest(activity: ComponentActivity, manifest: ListItemManifests): String =
        suspendCoroutine { continuation ->
            deleteCurrentManifest(activity, manifest) { manifests ->
                continuation.resume(manifests)
            }
        }

    suspend fun deleteAllManifests(activity: ComponentActivity): String =
        suspendCoroutine { continuation ->
            removeAllManifests(activity) { manifests ->
                continuation.resume(manifests)
            }
        }


    fun deleteAllLinks(context: ComponentActivity, onResult: (String) -> Unit) {
        val db = MainDb.getDb(context).getDao()
        db.deleteAllLinks()
        onResult("+")
    }

    private fun removeAllManifests(context: ComponentActivity, onResult: (String) -> Unit) {
        val db = MainDb.getDb(context).getDao()
        db.deleteAllManifests()
        onResult("+")
    }

    fun addLink(context: ComponentActivity, link: ListItemLink, onResult: (String) -> Unit) {
        val db = MainDb.getDb(context).getDao()
        db.insertLink(link)
        onResult("+")
    }

    fun addManifest(context: ComponentActivity, manifest: ListItemManifests, onResult: (String) -> Unit) {
        val db = MainDb.getDb(context).getDao()
        db.insertManifest(manifest)
        onResult("+")
    }

    private fun deleteCurrentLink(context: ComponentActivity, link: ListItemLink, onResult: (String) -> Unit){
        val db = MainDb.getDb(context).getDao()
        db.deleteLink(link)
        onResult("+")
    }

    private fun deleteCurrentManifest(context: ComponentActivity, manifest: ListItemManifests, onResult: (String) -> Unit){
        val db = MainDb.getDb(context).getDao()
        db.deleteManifest(manifest)
        onResult("+")
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