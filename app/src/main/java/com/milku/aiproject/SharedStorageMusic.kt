package com.milku.aiproject

import android.content.ContentUris
import android.health.connect.datatypes.units.Length
import android.net.Uri
import android.util.Log
import java.net.URI

data class SharedStorageMusic(
    val id: Long,
    val name: String,
    val size: Int,
    val contentUri: Uri
)