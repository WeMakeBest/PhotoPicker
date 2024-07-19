package com.softsun.photopicker.models


data class Album(
    val files: List<MediaItem>,
    val name: String,
    val count: String,
    val albumId: Long? = null
)