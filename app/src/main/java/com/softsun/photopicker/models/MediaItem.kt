package com.softsun.photopicker.models

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.softsun.photopicker.activities.PhotoPicker

data class MediaItem(
    var uri: Uri,
    val type: PhotoPicker.FileType,
    val duration: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(Uri::class.java.classLoader)!!,
        PhotoPicker.FileType.valueOf(parcel.readString()!!),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(uri, flags)
        parcel.writeString(type.name)
        parcel.writeString(duration)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MediaItem> {
        override fun createFromParcel(parcel: Parcel): MediaItem {
            return MediaItem(parcel)
        }

        override fun newArray(size: Int): Array<MediaItem?> {
            return arrayOfNulls(size)
        }
    }
}