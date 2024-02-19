package com.kumar.chatapp.ui.data.model

import com.google.gson.Gson


data class ChatMessage(
    val message: String,
    val fromUserId: String
)

fun ChatMessage.toJsonString(): String = Gson().toJson(this).toString()