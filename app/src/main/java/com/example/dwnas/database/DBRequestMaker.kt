package com.example.dwnas.database

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class DBRequestMaker: ViewModel() {
    fun getListLinks(context: ComponentActivity, onResult:(List<ListItemLink>?)->Unit){
        viewModelScope.launch {
            val db = MainDb.getDb(context).getDao()
            db.getAllLinks().asLiveData().observe(context){
                onResult(it)
            }
        }
    }
    fun getListManifests(context: ComponentActivity, onResult:(List<ListItemManifests>?)->Unit){
        viewModelScope.launch {
            val db = MainDb.getDb(context).getDao()
            db.getAllManifests().asLiveData().observe(context){
                onResult(it)
            }
        }
    }

    fun deleteAllLinks(context: ComponentActivity, onResult:(String)->Unit){
        viewModelScope.launch {
            val db = MainDb.getDb(context).getDao()
            db.deleteAllLinks()
            onResult("+")
        }
    }
    fun deleteAllManifests(context: ComponentActivity, onResult:(String)->Unit){
        viewModelScope.launch {
            val db = MainDb.getDb(context).getDao()
            db.deleteAllManifests()
            onResult("+")
        }
    }
    fun insertLink(context: ComponentActivity, link: ListItemLink){
        viewModelScope.launch {
            val db = MainDb.getDb(context).getDao()
            db.insertLink(link)
        }
    }
    fun insertManifest(context: ComponentActivity, manifest: ListItemManifests){
        viewModelScope.launch {
            val db = MainDb.getDb(context).getDao()
            db.insertManifest(manifest)
        }
    }
}