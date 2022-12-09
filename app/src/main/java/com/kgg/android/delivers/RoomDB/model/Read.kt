package com.kgg.android.delivers.RoomDB.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Read(
     @ColumnInfo val pid: String?,

     @PrimaryKey(autoGenerate = true) var id: Int = 0,

     )