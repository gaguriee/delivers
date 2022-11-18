package com.kgg.android.delivers.data

import java.io.Serializable

data class ChatRoom(
    val postId: String,
    val users: HashMap<String, Boolean> = HashMap(),
    val messages: HashMap<String, Message> = HashMap()
):Serializable
//