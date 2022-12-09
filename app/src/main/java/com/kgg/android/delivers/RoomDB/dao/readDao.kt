package com.kgg.android.delivers.RoomDB.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kgg.android.delivers.RoomDB.model.Read

@Dao
interface readDao {
    @Query("SELECT * FROM Read")
    fun getAll(): List<Read>

    @Query("SELECT pid FROM Read")
    fun getAllPid(): List<String>


    @Insert
    fun insertRead(read: Read)

    @Query("DELETE FROM read")
    fun deleteAll()
}