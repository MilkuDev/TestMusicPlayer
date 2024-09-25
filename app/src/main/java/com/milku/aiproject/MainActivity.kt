package com.milku.aiproject

import android.Manifest
import android.content.ContentUris
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.milku.aiproject.ui.theme.AudioPlayerTheme
import kotlinx.coroutines.runBlocking

///@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val musicList = loadMusicFromExternalStorage()
        val urisList = mutableListOf<Uri>()
        setContent {
            var songIndex by remember { mutableIntStateOf(0) }
            for (element in musicList) {
                urisList.add(element.contentUri)
                Log.d("gay", "track uri: ${element.contentUri}")
                Log.d("gay", "track data: $element")
            }
            if (urisList.isEmpty()) {
                Log.d("gay", "check the app permissions or the music storage can be empty")
            } else {
                mediaPlayer = MediaPlayer.create(this, urisList[songIndex])
                Log.d("gay", "media player created, media uri: ${urisList[songIndex]}")
            }
            AudioPlayerTheme {
                val permissionState =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        rememberMultiplePermissionsState(
                            permissions = listOf(
                                Manifest.permission.READ_MEDIA_AUDIO,
                            )
                        )
                    } else {
                        rememberMultiplePermissionsState(
                            permissions = listOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                        )
                    }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(
                    key1 = lifecycleOwner,
                    effect = {
                        val observer = LifecycleEventObserver { _, event ->
                            Log.d("gay", "event")
                            if(event == Lifecycle.Event.ON_START) {
                                permissionState.launchMultiplePermissionRequest()
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }
                )
                Column {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        permissionState.permissions.forEach { perm ->
                            when(perm.permission) {
                                Manifest.permission.READ_MEDIA_AUDIO -> {
                                    when {
                                        perm.status.isGranted -> {
                                            Log.d("gay", "permission is granted")
                                            Text(
                                                text = "Audio files permission accepted"
                                            )
                                            val music = mutableListOf<String>()
                                            for (element in loadMusicFromExternalStorage()) {
                                                music.add(element.name)
                                            }
                                            for (element in music) {
                                                Log.d("gay", element)
                                            }
                                        }
                                        perm.status.shouldShowRationale -> {
                                            Log.d("gay", "permission info")
                                            Text(
                                                text = "Audio files permission is needed " +
                                                        "to access your music files"
                                            )
                                        }
                                        perm.isSecondaryDenied() -> {
                                            Log.d("gay", "not granted")
                                            Text(
                                                text = "App cant work properly without audio files " +
                                                        "permission, please, give it permission in " +
                                                        "app settings"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        permissionState.permissions.forEach { perm ->
                            when(perm.permission) {
                                Manifest.permission.READ_EXTERNAL_STORAGE -> {
                                    when {
                                        perm.status.isGranted -> {
                                            Text(
                                                text = "Audio files permission accepted"
                                            )
                                            val music = mutableListOf<String>()
                                            for (element in loadMusicFromExternalStorage()) {
                                                music.add(element.name)
                                            }
                                            for (element in music) {
                                                Log.d("gay", element)
                                            }
                                        }
                                        perm.status.shouldShowRationale -> {
                                            Text(
                                                text = "External files permission is needed " +
                                                        "to access your music files"
                                            )
                                            Log.d("gay", "perm info")
                                        }
                                        perm.isSecondaryDenied() -> {
                                            Text(
                                                text = "App cant work properly without external files " +
                                                        "permission, please, give it permission in " +
                                                        "app settings"
                                            )
                                            Log.d("gay", "not granted")
                                        }
                                    }
                                }
                            }

                        }

                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        mediaPlayer?.let {
                            AudioPlayer(
                                it,
                                changeSongIndex = { newSongIndex ->
                                    songIndex = newSongIndex
                                    Log.d("gay", "the current song index is: $newSongIndex")
                                },
                                songUri = urisList[songIndex] // у тебя всегда первый тре?да. ну он и не будет обновлятся.
                            )
                            Log.d("gay", "track info ${mediaPlayer?.trackInfo}")
                        }
                        if (urisList.isEmpty()) {
                            Text(
                                modifier = Modifier
                                    .fillMaxSize(),

                                text = "Music list is empty",
                                color = Color.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
        mediaPlayer?.setOnCompletionListener {
        }
    }
    private fun loadMusicFromExternalStorage(): List<SharedStorageMusic> {
        return runBlocking {
            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.SIZE,
            )
            val music = mutableListOf<SharedStorageMusic>()
            contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
            )?.use {cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(displayNameColumn)
                    val size = cursor.getInt(sizeColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    music.add(SharedStorageMusic(id, name, size, contentUri))
                }
                music.toList()
            } ?: listOf()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
    }
}

//@Composable
//fun AudioPlayer(
//    mediaPlayer: MediaPlayer,
//    songIndex: (Int) -> Unit,
//    songUri: Uri
//) {
//    val context = LocalContext.current
//}

@Composable
fun AudioPlayer(
    mediaPlayer: MediaPlayer,
    songUri: Uri,
    changeSongIndex: (Int) -> Unit
) {
    var newSongIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    val trackName by remember { mutableStateOf("Sample Track") }
    val totalDuration = mediaPlayer.duration.toFloat()
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = trackName,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = formatTime(mediaPlayer.currentPosition) + " / " + formatTime(mediaPlayer.duration),
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = currentProgress,
            onValueChange = { newValue ->
                currentProgress = newValue
                mediaPlayer.seekTo((newValue * totalDuration).toInt())
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = {
                if (newSongIndex <= 0) {
                    newSongIndex = 0
                } else {
                    newSongIndex -= 1
                }
                changeSongIndex(newSongIndex)
                mediaPlayer.stop()
                mediaPlayer.reset()
                mediaPlayer.release()
                try {
                    mediaPlayer.setDataSource(context, songUri)
                    mediaPlayer.prepare()
                    if (isPlaying) {
                        mediaPlayer.reset()
                        mediaPlayer.setDataSource(context, songUri)
                        mediaPlayer.prepare()
                        mediaPlayer.start()
                    } else {
                        mediaPlayer.start()
                    }
                } catch (e: Exception) {
                    Log.e("AudioPlayer", "Error setting data source", e)
                }
                isPlaying = !isPlaying
            }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
            }
            IconButton(onClick = {
                Log.d("song index", "the song index is $newSongIndex")
                if (isPlaying) {
                    mediaPlayer.pause()
                } else {
                    mediaPlayer.start()
                }
                isPlaying = !isPlaying
            }) {
                Icon(
                    if (isPlaying == true) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
            IconButton(onClick = {
                newSongIndex+=1
                changeSongIndex(newSongIndex)
                mediaPlayer.stop()
                mediaPlayer.reset()
                Log.d("audioPlayer state", "the media was stop and reset")
                try {
                    mediaPlayer.setDataSource(context, songUri)
                    mediaPlayer.prepare()
                    Log.d("audioPlayer state", "the media was set and prepare")
                    if (isPlaying) {
                        mediaPlayer.start()
                        Log.d("audioPlayer state", "the media was playing and was started successfully")
                        Log.d("song index", "the song index is $newSongIndex")
                    } else {
                        mediaPlayer.start()
                        Log.d("audioPlayer state", "the media wasn't playing and was started successfully")
                        Log.d("song index", "the song index is $newSongIndex")
                    }
                } catch (e: Exception) {
                    Log.e("AudioPlayer", "Error setting data source", e)
                }
                isPlaying = !isPlaying
            }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next")
            }
        }
    }
}
fun formatTime(ms: Int): String {
    val minutes = (ms / 1000) / 60
    val seconds = (ms / 1000) % 60
    return String.format(locale = java.util.Locale("%02d:%02d"), minutes.toString(), seconds)
}