package com.kgg.android.delivers.RoomDB

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kgg.android.delivers.RoomDB.dao.readDao
import com.kgg.android.delivers.RoomDB.model.Read

@Database(entities = [Read::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun readDao() : readDao
}