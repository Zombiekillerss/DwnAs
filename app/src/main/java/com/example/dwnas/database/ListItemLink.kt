package com.example.dwnas.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Link")
data class ListItemLink (
    @PrimaryKey
    val id: Int = 0,
    @ColumnInfo(name="date")
    var link: String
)