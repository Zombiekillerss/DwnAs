package com.example.dwnas.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Manifest")
data class ListItemManifests (
    @PrimaryKey
    val id: Int = 0,
    @ColumnInfo(name="idLink")
    val idLink: Int,
    @ColumnInfo(name="manifest")
    val manifest: String,
    @ColumnInfo(name="name")
    val name: String
)