package com.kgg.android.delivers

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Story(
    val writer : String,
    val Location : String,
    val photo:String,
    val title:String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    var registerDate : String = ""
) : Parcelable
