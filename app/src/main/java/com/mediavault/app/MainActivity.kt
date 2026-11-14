package com.mediavault.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import android.widget.*
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentHashMap

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val found = ConcurrentHashMap<String, String>()
    private lateinit var browser: WebView
    private lateinit var address: EditText
    private lateinit var detections: LinearLayout
    private lateinit var status: TextView
    private lateinit var content: FrameLayout
    private var currentReferer: String? = null
    private val bg = Color.rgb(15, 18, 25)
    private val panel = Color.rgb(27, 33, 45)
    private val accent = Color.rgb(120, 150, 255)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg; window.navigationBarColor = bg
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding(dp(12), dp(10), dp(12), dp(8)) }
        val title = TextView(this).apply { text = "MEDIAVAULT"; textSize = 22f; setTextColor(Color.WHITE); typeface = android.graphics.Typeface.DEFAULT_BOLD }
        root.addView(title)
        status = TextView(this).apply { text = "Browse the web or paste a media link"; textSize = 12f; setTextColor(0xffaab3c5.toInt()); setPadding(0, dp(4), 0, dp(8)) }
        root.addView(status)
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        address = EditText(this).apply {
            hint = "https://example.com/video"; setSingleLine(true); textSize = 14f
            setTextColor(Color.WHITE); setHintTextColor(0xff8993a8.toInt()); setPadding(dp(10), 0, dp(8), 0)
            setBackgroundColor(panel)
        }
        bar.addView(address, LinearLayout.LayoutParams(0, dp(46), 1f))
        bar.addView(makeButton("GO") { navigate() }, LinearLayout.LayoutParams(dp(58), dp(46)))
        root.addView(bar)
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        browser = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    request?.url?.toString()?.let { address.setText(it) }
                    return false
                }
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    currentReferer = url
                    address.setText(url ?: "")
                }
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val u = request?.url?.toString() ?: return null
                    val type = mediaType(u)
                    if (type != null && u.startsWith("http")) runOnUiThread { addDetection(u, type) }
                    return null
                }
            }
            setDownloadListener { url, _, _, mime, _ -> addDetection(url, mime ?: mediaType(url) ?: "MEDIA") }
        }
        content.addView(browser, FrameLayout.LayoutParams(-1, -1))
        root.addView(TextView(this).apply { text = "DETECTED MEDIA"; textSize = 11f; setTextColor(accent); setPadding(0, dp(9), 0, dp(5)) })
        val scroll = HorizontalScrollView(this)
        detections = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        scroll.addView(detections)
        root.addView(scroll, LinearLayout.LayoutParams(-1, dp(74)))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        nav.addView(makeButton("BROWSER") { showBrowser() }, LinearLayout.LayoutParams(0, dp(44), 1f))
        nav.addView(makeButton("LIBRARY") { showLibrary() }, LinearLayout.LayoutParams(0, dp(44), 1f))
        root.addView(nav)
        setContentView(root)
        if (savedInstanceState == null) browser.loadUrl("https://example.com")
    }

    private fun makeButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 11f; isAllCaps = false; setTextColor(Color.WHITE); setBackgroundColor(panel); setOnClickListener { action() }
    }
    private fun navigate() {
        var url = address.text.toString().trim()
        if (url.isBlank()) return
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
        if (url.endsWith(".m3u8", true) || mediaType(url) != null) addDetection(url, mediaType(url) ?: "MEDIA")
        showBrowser(); browser.loadUrl(url)
    }
    private fun mediaType(url: String): String? {
        val u = url.substringBefore('?').lowercase()
        return when {
            u.endsWith(".m3u8") -> "HLS"
            u.endsWith(".mp4") -> "MP4"
            u.endsWith(".webm") -> "WEBM"
            u.endsWith(".mkv") -> "MKV"
            u.endsWith(".mp3") -> "MP3"
            u.endsWith(".m4a") -> "M4A"
            u.endsWith(".aac") -> "AAC"
            u.endsWith(".ogg") -> "OGG"
            u.endsWith(".wav") -> "WAV"
            u.endsWith(".mov") -> "MOV"
            else -> null
        }
    }
    private fun addDetection(url: String, type: String) {
        if (!url.startsWith("http") || found.putIfAbsent(url, type) != null) return
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(4), dp(8), dp(4)); setBackgroundColor(panel) }
        card.addView(TextView(this).apply { text = type; setTextColor(Color.WHITE); textSize = 12f })
        card.addView(makeButton("SAVE") { save(url) })
        card.addView(makeButton("PLAY") { playRemote(url) })
        detections.addView(card, LinearLayout.LayoutParams(dp(112), dp(70)).apply { marginEnd = dp(6) })
        status.text = "${found.size} media source(s) detected"
    }
    private fun save(url: String) {
        status.text = "Preparing download…"
        worker.execute {
            try {
                val item = MediaStore.download(this, url, currentReferer) { pct ->
                    runOnUiThread { status.text = "Downloading… ${pct}%" }
                }
                runOnUiThread { status.text = "Saved: ${item.name}"; Toast.makeText(this, "Saved to MediaVault library", Toast.LENGTH_LONG).show() }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Download failed: ${e.message ?: "unsupported media"}" }
            }
        }
    }
    private fun playRemote(url: String) {
        startActivity(Intent(this, PlayerActivity::class.java).putExtra("path", url))
    }
    private fun showBrowser() {
        if (browser.parent != null) (browser.parent as ViewGroup).removeView(browser)
        content.removeAllViews(); content.addView(browser, FrameLayout.LayoutParams(-1, -1))
    }
    private fun showLibrary() {
        content.removeAllViews()
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(8), dp(8), dp(8)) }
        val scroll = ScrollView(this); scroll.addView(list)
        val items = MediaStore.all(this).reversed()
        items.forEach { item ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(8), dp(8), dp(8), dp(8)); setBackgroundColor(panel) }
            val label = TextView(this).apply { text = "${item.name}\n${item.kind}"; setTextColor(Color.WHITE); textSize = 13f }
            row.addView(label, LinearLayout.LayoutParams(0, dp(56), 1f))
            row.addView(makeButton("PLAY") { startActivity(Intent(this, PlayerActivity::class.java).putExtra("path", item.path)) })
            list.addView(row, LinearLayout.LayoutParams(-1, dp(64)).apply { bottomMargin = dp(6) })
        }
        if (items.isEmpty()) list.addView(TextView(this).apply { text = "Your saved media will appear here."; setTextColor(Color.LTGRAY) })
        content.addView(scroll)
    }
    override fun onDestroy() { browser.destroy(); worker.shutdown(); super.onDestroy() }
}
