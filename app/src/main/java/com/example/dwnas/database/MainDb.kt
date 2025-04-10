package com.example.dwnas.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    version = 1,
    entities = [
        ListItemLink::class,
        ListItemManifests::class
    ]
)
abstract class MainDb : RoomDatabase() {
    abstract fun getDao(): Dao

    companion object {
        fun getDb(context: Context): MainDb {
            return Room.databaseBuilder(
                context.applicationContext,
                MainDb::class.java,
                "links.db"
            ).build()
        }
    }
}