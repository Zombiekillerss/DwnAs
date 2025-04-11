package com.example.dwnas.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface Dao {
    @Insert(entity=ListItemLink::class)
    fun insertLink(link: ListItemLink)
    @Insert(entity=ListItemManifests::class)
    fun insertManifest(manifest: ListItemManifests)

    @Query("SELECT * FROM Link")
    fun getAllLinks(): Flow<List<ListItemLink>>
    @Query("SELECT * FROM Manifest")
    fun getAllManifests(): Flow<List<ListItemManifests>>

    @Delete
    fun deleteLink(link: ListItemLink)
    @Delete
    fun deleteManifest(link: ListItemManifests)

    @Query("DELETE FROM Link")
    fun deleteAllLinks()
    @Query("DELETE FROM Manifest")
    fun deleteAllManifests()

}