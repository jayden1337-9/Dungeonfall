package com.mediavault.app

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale

data class SavedMedia(val name: String, val path: String, val kind: String, val added: Long)

object MediaStore {
    private fun root(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "MediaVault").apply { mkdirs() }

    fun all(context: Context): List<SavedMedia> {
        val file = File(context.filesDir, "media-library.json")
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            (0 until array.length()).mapNotNull { i ->
                val o = array.getJSONObject(i)
                val item = SavedMedia(o.getString("name"), o.getString("path"), o.getString("kind"), o.getLong("added"))
                if (File(item.path).exists()) item else null
            }
        } catch (_: Exception) { emptyList() }
    }

    @Synchronized fun add(context: Context, item: SavedMedia) {
        val items = all(context).filterNot { it.path == item.path } + item
        val array = JSONArray()
        items.forEach { m ->
            array.put(JSONObject().put("name", m.name).put("path", m.path).put("kind", m.kind).put("added", m.added))
        }
        File(context.filesDir, "media-library.json").writeText(array.toString())
    }

    fun folder(context: Context, name: String): File =
        File(root(context), name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "media" }).apply { mkdirs() }

    fun download(context: Context, source: String, referer: String?, onProgress: (Int) -> Unit): SavedMedia {
        require(source.startsWith("https://") || source.startsWith("http://")) { "Only HTTP(S) media URLs are supported" }
        val uri = Uri.parse(source)
        val base = (uri.lastPathSegment ?: "media").substringBefore('?').ifBlank { "media" }
        val dir = folder(context, base.substringBeforeLast('.', base))
        val isHls = source.substringBefore('?').lowercase(Locale.ROOT).endsWith(".m3u8")
        val result = if (isHls) downloadHls(source, dir, referer, onProgress) else {
            val ext = base.substringAfterLast('.', "bin").take(8)
            val out = File(dir, "media.$ext")
            val conn = open(source, referer)
            try {
                val length = conn.contentLengthLong
                conn.inputStream.use { input ->
                    out.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024); var total = 0L
                        while (true) {
                            val n = input.read(buf); if (n < 0) break
                            total += n; require(total <= 2L * 1024 * 1024 * 1024) { "File exceeds 2 GB limit" }
                            output.write(buf, 0, n)
                            if (length > 0) onProgress((total * 100 / length).toInt().coerceIn(0, 99))
                        }
                    }
                }
            } finally { conn.disconnect() }
            out
        }
        onProgress(100)
        val kind = if (isHls) "HLS" else base.substringAfterLast('.', "FILE").uppercase(Locale.ROOT)
        return SavedMedia(base, result.absolutePath, kind, System.currentTimeMillis()).also { add(context, it) }
    }

    private fun downloadHls(url: String, dir: File, referer: String?, progress: (Int) -> Unit): File {
        var playlistUrl = url
        var text = readText(playlistUrl, referer)
        if (text.contains("#EXT-X-STREAM-INF")) {
            val lines = text.lines()
            val variants = mutableListOf<Pair<Long, String>>()
            for (i in lines.indices) if (lines[i].startsWith("#EXT-X-STREAM-INF")) {
                val bw = Regex("BANDWIDTH=(\\d+)").find(lines[i])?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                val next = lines.drop(i + 1).firstOrNull { it.isNotBlank() && !it.startsWith("#") }
                if (next != null) variants += bw to URI(url).resolve(next.trim()).toString()
            }
            playlistUrl = variants.maxByOrNull { it.first }?.second ?: error("No HLS variant found")
            text = readText(playlistUrl, referer)
        }
        require(text.contains("#EXTM3U")) { "Invalid HLS playlist" }
        require(text.contains("#EXT-X-ENDLIST")) { "Live HLS streams cannot be saved as completed files" }
        require(!text.contains("#EXT-X-KEY") || text.contains("#EXT-X-KEY:METHOD=NONE")) { "Encrypted HLS is not supported" }
        require(!text.contains("#EXT-X-MAP") && !text.contains("#EXT-X-BYTERANGE")) { "This HLS playlist uses a format not supported by the downloader" }
        val lines = text.lines()
        val segments = lines.filter { it.isNotBlank() && !it.startsWith("#") }
        require(segments.isNotEmpty()) { "No HLS segments found" }
        val rewritten = mutableListOf<String>()
        var segmentIndex = 0
        for (line in lines) {
            if (line.isNotBlank() && !line.startsWith("#")) {
                segmentIndex++
                val segmentUrl = URI(playlistUrl).resolve(line.trim()).toString()
                val segmentFile = File(dir, "segment_%05d.ts".format(segmentIndex))
                val conn = open(segmentUrl, referer)
                try { conn.inputStream.use { input -> segmentFile.outputStream().use { output -> input.copyTo(output) } } }
                finally { conn.disconnect() }
                rewritten += segmentFile.name
                progress((segmentIndex * 95 / segments.size).coerceIn(1, 95))
            } else rewritten += line
        }
        return File(dir, "index.m3u8").apply { writeText(rewritten.joinToString("\n")) }
    }

    private fun readText(url: String, referer: String?): String {
        val c = open(url, referer)
        return try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }

    private fun open(url: String, referer: String?): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 30000
        c.setRequestProperty("User-Agent", "MediaVault/1.0 Android")
        if (!referer.isNullOrBlank()) c.setRequestProperty("Referer", referer)
        c.instanceFollowRedirects = true
        c.connect()
        if (c.responseCode !in 200..299) throw IllegalStateException("HTTP ${c.responseCode}")
        return c
    }
}
