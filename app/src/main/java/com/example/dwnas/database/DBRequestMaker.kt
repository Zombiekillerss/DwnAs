package com.example.dwnas.database

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class DBRequestMaker: ViewModel() {
    fun getListLinks(context: Context): List<ListItemLink>?{
        val result: List<ListItemLink>? = null
        viewModelScope.launch {

        }
        return result
    }
    fun getListManifests(): List<ListItemManifests>?{
        return null
    }

    fun deleteAllLinks(){
    }
    fun deleteAllManifests(){
    }
    fun insertLink(){
    }
    fun insertManifest(){
    }
}