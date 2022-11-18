package com.kgg.android.delivers.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Story(
    var writer : String? = null,
    var location : String? = null,
    var photo:String? = null,
    var description:String? = null,
    var category: String? = null,
    var latitude: Double? = 0.0,
    var longitude: Double? = 0.0,
    var registerDate : String? = null,
    var postId:String? = null
) : Parcelable
