package com.example.dwnas.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Link")
data class ListItemLink (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name="name")
    val name: String,
    @ColumnInfo(name="link")
    var link: String
)