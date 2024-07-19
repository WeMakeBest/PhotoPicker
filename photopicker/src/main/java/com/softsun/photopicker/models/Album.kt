package com.softsun.photopicker.models

data class Album(
    val files: List<MediaItemModel>,
    val name: String,
    val count: String,
    val albumId: Long? = null
)