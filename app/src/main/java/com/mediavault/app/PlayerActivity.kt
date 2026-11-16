package com.mediavault.app

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

class PlayerActivity : Activity() {
    private var player: ExoPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = PlayerView(this)
        view.layoutParams = ViewGroup.LayoutParams(-1, -1)
        setContentView(view)
        val path = intent.getStringExtra("path") ?: run { finish(); return }
        val uri = if (path.startsWith("http")) Uri.parse(path) else Uri.fromFile(File(path))
        player = ExoPlayer.Builder(this).build().also {
            view.player = it
            val isHls = path.substringBefore('?').lowercase().endsWith(".m3u8")
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .apply { if (isHls) setMimeType(MimeTypes.APPLICATION_M3U8) }
                .build()
            it.setMediaItem(mediaItem)
            it.prepare()
            it.playWhenReady = true
        }
    }
    override fun onStop() { super.onStop(); player?.pause() }
    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }
}
