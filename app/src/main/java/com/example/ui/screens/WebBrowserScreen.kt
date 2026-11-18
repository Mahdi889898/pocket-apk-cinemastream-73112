package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.DetectedWebStream
import com.example.data.model.MediaSource
import com.example.data.model.MediaType
import com.example.data.model.PlayerRequest
import com.example.data.model.SourceType
import com.example.data.remote.CuratedCatalog
import com.example.data.remote.MediaDiscoveryService
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGoldContainer
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.StreamCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebBrowserScreen(
    currentUrl: String,
    detectedStreams: List<DetectedWebStream>,
    savedSources: List<MediaSource>,
    onUrlChange: (String) -> Unit,
    onStreamDetected: (DetectedWebStream) -> Unit,
    onClearStreams: () -> Unit,
    onPlayStream: (PlayerRequest) -> Unit,
    onBookmarkSite: (title: String, url: String) -> Unit,
    onSaveStreamFavorite: (DetectedWebStream) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var addressInput by remember(currentUrl) { mutableStateOf(currentUrl) }
    var pageTitle by remember { mutableStateOf("مرورگر فیلم") }
    var progress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var showStreamsSheet by remember { mutableStateOf(false) }

    val webSites = remember(savedSources) {
        val custom = savedSources
            .filter { it.resolvedType() == SourceType.WEBSITE }
            .map { it.title to it.url }
        val defaults = CuratedCatalog.starterWebsites.map { it.title.substringBefore(" —") to it.url }
        (custom + defaults).distinctBy { it.second }
    }

    val webView = remember(context) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
        }
    }

    DisposableEffect(webView) {
        val bridge = object {
            @JavascriptInterface
            fun onVideoUrlFound(url: String, title: String) {
                if (url.startsWith("http://", true) || url.startsWith("https://", true)) {
                    webView.post {
                        onStreamDetected(
                            DetectedWebStream(
                                url = url,
                                formatBadge = MediaDiscoveryService.inferFormatBadge(url),
                                title = title.ifBlank { pageTitle },
                            )
                        )
                    }
                }
            }
        }
        webView.addJavascriptInterface(bridge, "CinemaSniffer")
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress = newProgress
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) pageTitle = title
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                isLoading = true
                if (!url.isNullOrBlank()) addressInput = url
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                isLoading = false
                view?.evaluateJavascript(
                    """
                    (function() {
                      var t = document.title || 'ویدیوی وب';
                      document.querySelectorAll('video, source').forEach(function(el) {
                        var s = el.currentSrc || el.src;
                        if (s && s.indexOf('http') === 0) CinemaSniffer.onVideoUrlFound(s, t);
                      });
                      document.querySelectorAll('meta[property="og:video"], meta[property="og:video:url"]').forEach(function(m) {
                        var c = m.getAttribute('content');
                        if (c && c.indexOf('http') === 0) CinemaSniffer.onVideoUrlFound(c, t);
                      });
                      document.querySelectorAll('a[href]').forEach(function(a) {
                        var h = a.href || '';
                        if (/\\.(mp4|m3u8|mpd|webm|mkv)(\\?|$)/i.test(h)) {
                          CinemaSniffer.onVideoUrlFound(h, (a.innerText || t).trim());
                        }
                      });
                    })();
                    """.trimIndent(),
                    null
                )
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?,
            ): WebResourceResponse? {
                val url = request?.url?.toString().orEmpty()
                if (url.isNotBlank() && isSniffableMediaUrl(url)) {
                    view?.post {
                        onStreamDetected(
                            DetectedWebStream(
                                url = url,
                                formatBadge = MediaDiscoveryService.inferFormatBadge(url),
                                title = pageTitle,
                            )
                        )
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }
        onDispose {
            webView.stopLoading()
            webView.removeJavascriptInterface("CinemaSniffer")
            webView.destroy()
        }
    }

    LaunchedEffect(currentUrl) {
        val normalized = MediaDiscoveryService.normalizeUrl(currentUrl)
        if (webView.url != normalized) {
            onClearStreams()
            webView.loadUrl(normalized)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Address Bar & Browser Controls
            Surface(
                color = ObsidianSurface,
                tonalElevation = 2.dp,
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IconButton(
                            onClick = { if (webView.canGoBack()) webView.goBack() },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "صفحه قبل",
                                tint = TextSecondary,
                            )
                        }

                        OutlinedTextField(
                            value = addressInput,
                            onValueChange = { addressInput = it },
                            placeholder = { Text("آدرس سایت فیلم را وارد کنید...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    val target = MediaDiscoveryService.normalizeUrl(addressInput)
                                    onUrlChange(target)
                                    webView.loadUrl(target)
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ObsidianElevated,
                                unfocusedContainerColor = ObsidianElevated,
                                focusedBorderColor = StreamCyan,
                                unfocusedBorderColor = ObsidianCardBorder,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("browser_url_input"),
                        )

                        IconButton(
                            onClick = {
                                val target = MediaDiscoveryService.normalizeUrl(addressInput)
                                onUrlChange(target)
                                webView.loadUrl(target)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("browser_go_button"),
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "بارگذاری صفحه", tint = CinemaGold)
                        }

                        IconButton(
                            onClick = { onBookmarkSite(pageTitle.take(60), addressInput) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("bookmark_site_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BookmarkAdd,
                                contentDescription = "ذخیره سایت",
                                tint = StreamCyan,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(webSites, key = { it.second }) { (label, siteUrl) ->
                            val selected = addressInput.startsWith(siteUrl)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    addressInput = siteUrl
                                    onUrlChange(siteUrl)
                                    webView.loadUrl(siteUrl)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Language,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                    )
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CinemaGoldContainer,
                                    selectedLabelColor = CinemaGold,
                                    containerColor = ObsidianElevated,
                                    labelColor = TextSecondary,
                                ),
                            )
                        }
                    }
                }
            }

            if (isLoading && progress < 100) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    color = CinemaGold,
                    trackColor = ObsidianSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            AndroidView(
                factory = { webView },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }

        // Floating Detected Video Streams Pill
        AnimatedVisibility(
            visible = detectedStreams.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
        ) {
            Button(
                onClick = { showStreamsSheet = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CinemaGold,
                    contentColor = ObsidianBg,
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("detected_streams_pill"),
            ) {
                Icon(Icons.Rounded.VideoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${detectedStreams.size} ویدیو در صفحه پیدا شد — پخش در پلیر",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }

    if (showStreamsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStreamsSheet = false },
            containerColor = ObsidianSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "ویدیوهای شناسایی‌شده در سایت",
                    style = MaterialTheme.typography.titleLarge,
                    color = CinemaGold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "روی هر مورد بزنید تا با پلیر حرفه‌ای سینما استریم پخش شود:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(detectedStreams, key = { it.url }) { stream ->
                        Surface(
                            color = ObsidianElevated,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ObsidianCardBorder, RoundedCornerShape(14.dp))
                                .clickable {
                                    showStreamsSheet = false
                                    onPlayStream(
                                        PlayerRequest(
                                            title = stream.title,
                                            streamUrl = stream.url,
                                            sourceName = pageTitle.take(40),
                                            mediaType = if (stream.formatBadge == "HLS") {
                                                MediaType.LIVE_CHANNEL
                                            } else {
                                                MediaType.WEB_VIDEO
                                            },
                                        )
                                    )
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    color = CinemaGoldContainer,
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text(
                                        text = stream.formatBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CinemaGold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stream.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = stream.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(
                                    onClick = { onSaveStreamFavorite(stream) },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FavoriteBorder,
                                        contentDescription = "ذخیره در علاقه‌مندی‌ها",
                                        tint = StreamCyan,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(CinemaGold),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "پخش",
                                        tint = ObsidianBg,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun isSniffableMediaUrl(url: String): Boolean {
    val clean = url.lowercase().substringBefore('?')
    return clean.endsWith(".m3u8") ||
        clean.endsWith(".mp4") ||
        clean.endsWith(".mpd") ||
        clean.endsWith(".webm") ||
        clean.endsWith(".mkv")
}
