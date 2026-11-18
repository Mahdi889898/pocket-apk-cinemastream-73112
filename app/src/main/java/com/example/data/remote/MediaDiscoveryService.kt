package com.example.data.remote

import com.example.data.model.CatalogMovie
import com.example.data.model.DetectedWebStream
import com.example.data.model.InternetSearchBundle
import com.example.data.model.M3uChannel
import com.example.data.model.MediaSource
import com.example.data.model.MediaType
import com.example.data.model.SourceType
import com.example.data.model.WebSearchPageHit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class MediaDiscoveryService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Automatically searches across the internet (Archive.org, Wikimedia Video API,
     * DuckDuckGo web search + automated page stream crawler, and user's M3U/website sources)
     * and prepares ready-to-play stream links.
     */
    suspend fun searchInternetForMovie(
        query: String,
        savedSources: List<MediaSource>,
        onProgress: (String) -> Unit = {},
    ): Result<InternetSearchBundle> = withContext(Dispatchers.IO) {
        try {
            val cleanQuery = query.trim()
            if (cleanQuery.isEmpty()) {
                return@withContext Result.success(InternetSearchBundle())
            }

            onProgress("در حال جستجو در سراسر اینترنت، آرشیو و کانال‌های شما...")

            coroutineScope {
                val archiveDeferred = async { searchArchiveAndResolve(cleanQuery) }
                val wikimediaDeferred = async { searchWikimediaVideos(cleanQuery) }
                val playlistsDeferred = async { searchSavedM3uPlaylists(cleanQuery, savedSources) }
                val webSearchDeferred = async { searchWebAndCrawlStreams(cleanQuery, savedSources, onProgress) }

                val archiveMovies = archiveDeferred.await()
                val wikimediaMovies = wikimediaDeferred.await()
                val playlistMatches = playlistsDeferred.await()
                val (crawledStreams, webPages) = webSearchDeferred.await()

                val allPlayable = (playlistMatches + archiveMovies + wikimediaMovies + crawledStreams)
                    .distinctBy { it.streamUrl }

                Result.success(
                    InternetSearchBundle(
                        playableMovies = allPlayable,
                        webPages = webPages,
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun searchArchiveAndResolve(query: String): List<CatalogMovie> = coroutineScope {
        val rawResults = searchArchiveMovies(query).getOrDefault(emptyList())
        rawResults.take(6).map { item ->
            async {
                val identifier = item.id.removePrefix("ia_")
                val resolved = resolveArchiveDirectMp4(identifier)
                if (resolved != null) {
                    item.copy(
                        streamUrl = resolved,
                        formatBadge = inferFormatBadge(resolved),
                    )
                } else {
                    item
                }
            }
        }.awaitAll()
    }

    suspend fun searchArchiveMovies(query: String): Result<List<CatalogMovie>> = withContext(Dispatchers.IO) {
        try {
            val cleanQuery = query.trim().ifEmpty { "classic cinema" }
            val encoded = URLEncoder.encode(
                "($cleanQuery) AND mediatype:(movies) AND format:(MPEG4 OR h.264)",
                "UTF-8"
            )
            val url = "https://archive.org/advancedsearch.php?q=$encoded" +
                "&fl[]=identifier,title,description,year,downloads" +
                "&sort[]=downloads+desc&rows=8&page=1&output=json"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", BROWSER_USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${response.code}"))
                }
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val docs = json.optJSONObject("response")?.optJSONArray("docs")
                    ?: return@withContext Result.success(emptyList())

                val results = mutableListOf<CatalogMovie>()
                for (i in 0 until docs.length()) {
                    val doc = docs.optJSONObject(i) ?: continue
                    val identifier = doc.optString("identifier")
                    val title = doc.optString("title").takeIf { it.isNotBlank() } ?: identifier
                    if (identifier.isBlank()) continue
                    val description = doc.optString("description")
                        .replace(Regex("<[^>]*>"), " ")
                        .trim()
                        .take(120)
                        .ifEmpty { "فیلم استخراج‌شده از آرشیو اینترنت" }
                    val year = doc.optString("year", "")

                    val streamUrl = "https://archive.org/download/$identifier/${identifier}_512kb.mp4"
                    val posterUrl = "https://archive.org/services/img/$identifier"

                    results.add(
                        CatalogMovie(
                            id = "ia_$identifier",
                            title = title,
                            subtitle = description,
                            streamUrl = streamUrl,
                            posterUrl = posterUrl,
                            category = "آرشیو فیلم",
                            sourceName = "Internet Archive",
                            mediaType = MediaType.MOVIE,
                            year = year,
                            formatBadge = "MP4",
                        )
                    )
                }
                Result.success(results)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveArchiveDirectMp4(identifier: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://archive.org/metadata/$identifier/files")
                .header("User-Agent", BROWSER_USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val json = JSONObject(response.body?.string().orEmpty())
                val files = json.optJSONArray("result") ?: return@withContext null
                var bestName: String? = null
                for (i in 0 until files.length()) {
                    val f = files.optJSONObject(i) ?: continue
                    val name = f.optString("name")
                    val format = f.optString("format")
                    if (name.endsWith(".mp4", ignoreCase = true) ||
                        format.contains("MPEG4", ignoreCase = true) ||
                        format.contains("h.264", ignoreCase = true)
                    ) {
                        if (bestName == null || !name.contains("512kb")) {
                            bestName = name
                        }
                    }
                }
                bestName?.let { "https://archive.org/download/$identifier/$it" }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun searchWikimediaVideos(query: String): List<CatalogMovie> {
        return try {
            val encoded = URLEncoder.encode("$query filetype:video", "UTF-8")
            val apiUrl = "https://commons.wikimedia.org/w/api.php?action=query&generator=search" +
                "&gsrnamespace=6&gsrsearch=$encoded&gsrlimit=6&prop=videoinfo&viprop=url|mime|size&format=json"
            val request = Request.Builder()
                .url(apiUrl)
                .header("User-Agent", "CinemaStreamAndroid/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val json = JSONObject(response.body?.string().orEmpty())
                val pages = json.optJSONObject("query")?.optJSONObject("pages") ?: return emptyList()
                val results = mutableListOf<CatalogMovie>()
                val keys = pages.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val page = pages.optJSONObject(key) ?: continue
                    val title = page.optString("title").removePrefix("File:")
                        .substringBeforeLast('.')
                    val videoInfo = page.optJSONArray("videoinfo")?.optJSONObject(0) ?: continue
                    val videoUrl = videoInfo.optString("url")
                    val mime = videoInfo.optString("mime")
                    if (videoUrl.isBlank()) continue
                    if (mime.contains("webm", true) || mime.contains("mp4", true) ||
                        videoUrl.endsWith(".webm", true) || videoUrl.endsWith(".mp4", true)
                    ) {
                        results.add(
                            CatalogMovie(
                                id = "wm_$key",
                                title = title,
                                subtitle = "ویدیوی آماده پخش از Wikimedia Commons",
                                streamUrl = videoUrl,
                                posterUrl = null,
                                category = "ویدیو وب",
                                sourceName = "Wikimedia Commons",
                                mediaType = MediaType.WEB_VIDEO,
                                formatBadge = inferFormatBadge(videoUrl, mime),
                            )
                        )
                    }
                }
                results
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun searchSavedM3uPlaylists(
        query: String,
        savedSources: List<MediaSource>,
    ): List<CatalogMovie> = coroutineScope {
        val playlists = savedSources.filter { it.resolvedType() == SourceType.PLAYLIST_M3U }.take(4)
        playlists.map { src ->
            async {
                val channels = fetchM3uPlaylist(src.url, src.category).getOrDefault(emptyList())
                channels
                    .filter { it.title.contains(query, ignoreCase = true) || it.groupTitle.contains(query, ignoreCase = true) }
                    .take(5)
                    .mapIndexed { idx, ch ->
                        CatalogMovie(
                            id = "m3u_${src.id}_$idx",
                            title = ch.title,
                            subtitle = "یافت‌شده در لیست ${src.title}",
                            streamUrl = ch.streamUrl,
                            posterUrl = ch.logoUrl,
                            category = ch.groupTitle,
                            sourceName = src.title,
                            mediaType = MediaType.LIVE_CHANNEL,
                            formatBadge = inferFormatBadge(ch.streamUrl),
                            isLive = ch.streamUrl.contains(".m3u8", ignoreCase = true),
                        )
                    }
            }
        }.awaitAll().flatten()
    }

    private suspend fun searchWebAndCrawlStreams(
        query: String,
        savedSources: List<MediaSource>,
        onProgress: (String) -> Unit,
    ): Pair<List<CatalogMovie>, List<WebSearchPageHit>> = coroutineScope {
        val pages = mutableListOf<WebSearchPageHit>()

        // 1. Search general web via DuckDuckGo HTML endpoint
        pages += fetchDuckDuckGoHits("$query movie watch video mp4 m3u8")

        // 2. Also search inside user's bookmarked movie websites if any exist
        val userSites = savedSources.filter { it.resolvedType() == SourceType.WEBSITE }.take(2)
        userSites.forEach { site ->
            val domain = site.url.substringAfter("://").substringBefore('/')
            if (domain.isNotBlank()) {
                pages += fetchDuckDuckGoHits("site:$domain $query")
            }
        }

        val uniquePages = pages.distinctBy { it.url }.take(8)
        if (uniquePages.isEmpty()) {
            return@coroutineScope emptyList<CatalogMovie>() to emptyList()
        }

        onProgress("در حال بررسی ${uniquePages.size} سایت و استخراج خودکار لینک پخش...")

        // 3. Crawl discovered pages in parallel (including 1-hop embedded iframes)
        val extractedMovies = uniquePages.take(6).mapIndexed { index, hit ->
            async {
                val streams = extractStreamsFromWebpageDeep(hit.url)
                streams.take(2).mapIndexed { sIdx, stream ->
                    CatalogMovie(
                        id = "web_${index}_${sIdx}_${stream.url.hashCode()}",
                        title = if (stream.title.isNotBlank() && stream.title != hit.domain) stream.title else hit.title,
                        subtitle = "استخراج خودکار از ${hit.domain}",
                        streamUrl = stream.url,
                        posterUrl = null,
                        category = "یافته وب",
                        sourceName = hit.domain,
                        mediaType = if (stream.formatBadge == "HLS") MediaType.LIVE_CHANNEL else MediaType.WEB_VIDEO,
                        formatBadge = stream.formatBadge,
                        isLive = stream.formatBadge == "HLS",
                    )
                }
            }
        }.awaitAll().flatten()

        extractedMovies to uniquePages
    }

    private fun fetchDuckDuckGoHits(searchTerm: String): List<WebSearchPageHit> {
        return try {
            val encoded = URLEncoder.encode(searchTerm, "UTF-8")
            val url = "https://html.duckduckgo.com/html/?q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Accept-Language", "fa-IR,fa;q=0.9,en-US;q=0.8,en;q=0.7")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val html = response.body?.string().orEmpty()
                parseDuckDuckGoHtml(html)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun extractStreamsFromWebpageDeep(pageUrl: String): List<DetectedWebStream> {
        return try {
            val direct = extractStreamsSync(pageUrl)
            if (direct.isNotEmpty()) return direct

            // Check 1-hop embedded iframe players if primary page had none
            val html = fetchHtmlSync(pageUrl) ?: return emptyList()
            val iframeRegex = Regex("""<iframe[^>]+src=["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE)
            val iframeUrls = iframeRegex.findAll(html).map { it.groupValues[1] }.take(2).toList()
            iframeUrls.flatMap { iframeUrl -> extractStreamsSync(iframeUrl) }.distinctBy { it.url }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchHtmlSync(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", BROWSER_USER_AGENT)
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        }
    }

    private fun extractStreamsSync(pageUrl: String): List<DetectedWebStream> {
        val cleanUrl = normalizeUrl(pageUrl)
        if (isDirectStreamUrl(cleanUrl)) {
            return listOf(
                DetectedWebStream(
                    url = cleanUrl,
                    formatBadge = inferFormatBadge(cleanUrl),
                    title = cleanUrl.substringAfterLast('/').substringBefore('?').ifBlank { "استریم مستقیم" },
                )
            )
        }
        val request = Request.Builder()
            .url(cleanUrl)
            .header("User-Agent", BROWSER_USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            val contentType = response.header("Content-Type").orEmpty()
            if (contentType.startsWith("video/") ||
                contentType.contains("mpegurl", ignoreCase = true) ||
                contentType.contains("dash+xml", ignoreCase = true)
            ) {
                return listOf(
                    DetectedWebStream(
                        url = cleanUrl,
                        formatBadge = inferFormatBadge(cleanUrl, contentType),
                        title = "ویدیوی مستقیم",
                    )
                )
            }
            val html = response.body?.string().orEmpty()
            return scanHtmlForStreams(html, cleanUrl)
        }
    }

    suspend fun fetchM3uPlaylist(playlistUrl: String, fallbackGroup: String): Result<List<M3uChannel>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(playlistUrl.trim())
                    .header("User-Agent", "CinemaStreamAndroid/1.0")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IllegalStateException("خطا در دریافت لیست: HTTP ${response.code}"))
                    }
                    val content = response.body?.string().orEmpty()
                    val parsed = M3uParser.parse(content, fallbackGroup)
                    if (parsed.isEmpty()) {
                        Result.failure(IllegalStateException("هیچ کانال یا لینک ویدیوی معتبری در این لیست پیدا نشد."))
                    } else {
                        Result.success(parsed)
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun extractStreamsFromWebpage(pageUrl: String): Result<List<DetectedWebStream>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(extractStreamsFromWebpageDeep(normalizeUrl(pageUrl)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun scanHtmlForStreams(html: String, baseUrl: String): List<DetectedWebStream> {
        val streamUrlRegex = Regex(
            """https?://[^\s"'<>\\)]+?\.(?:m3u8|mp4|mpd|webm|mkv|mov)(?:\?[^\s"'<>\\)]*)?""",
            RegexOption.IGNORE_CASE
        )
        val ogVideoRegex = Regex(
            """<meta[^>]+property=["']og:video(?::url|:secure_url)?["'][^>]+content=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE
        )
        val titleRegex = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val pageTitle = titleRegex.find(html)?.groupValues?.getOrNull(1)
            ?.replace(Regex("<[^>]*>"), "")
            ?.trim()
            ?.take(60)
            ?.ifBlank { null } ?: baseUrl.substringAfter("://").substringBefore('/')

        val urls = linkedSetOf<String>()
        ogVideoRegex.findAll(html).forEach { match ->
            match.groupValues.getOrNull(1)?.let { urls.add(it.replace("&amp;", "&").replace("\\/", "/")) }
        }
        streamUrlRegex.findAll(html).forEach { match ->
            urls.add(match.value.replace("&amp;", "&").replace("\\/", "/"))
        }

        return urls.take(25).mapIndexed { index, url ->
            DetectedWebStream(
                url = url,
                formatBadge = inferFormatBadge(url),
                title = if (urls.size == 1) pageTitle else "$pageTitle (${index + 1})",
            )
        }
    }

    fun parseDuckDuckGoHtml(html: String): List<WebSearchPageHit> {
        val linkRegex = Regex(
            """<a[^>]+class=["'][^"']*result__a[^"']*["'][^>]+href=["']([^"']+)["'][^>]*>(.*?)</a>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val hits = mutableListOf<WebSearchPageHit>()
        linkRegex.findAll(html).forEach { match ->
            val rawHref = match.groupValues[1]
            val rawTitle = match.groupValues[2].replace(Regex("<[^>]*>"), "").trim()
            val resolvedUrl = resolveDuckDuckGoHref(rawHref)
            if (resolvedUrl.startsWith("http://", true) || resolvedUrl.startsWith("https://", true)) {
                val domain = resolvedUrl.substringAfter("://").substringBefore('/').removePrefix("www.")
                if (domain.isNotBlank() && rawTitle.isNotBlank()) {
                    hits.add(
                        WebSearchPageHit(
                            title = rawTitle,
                            url = resolvedUrl,
                            domain = domain,
                            snippet = "برای استخراج خودکار ویدیو باز کنید",
                        )
                    )
                }
            }
        }
        return hits.distinctBy { it.url }.take(10)
    }

    private fun resolveDuckDuckGoHref(rawHref: String): String {
        val cleaned = rawHref.replace("&amp;", "&")
        val uddgMatch = Regex("""[?&]uddg=([^&]+)""").find(cleaned)
        return if (uddgMatch != null) {
            try {
                URLDecoder.decode(uddgMatch.groupValues[1], "UTF-8")
            } catch (_: Exception) {
                cleaned
            }
        } else if (cleaned.startsWith("//")) {
            "https:$cleaned"
        } else {
            cleaned
        }
    }

    companion object {
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

        fun normalizeUrl(raw: String): String {
            val trimmed = raw.trim()
            return if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
                trimmed
            } else {
                "https://$trimmed"
            }
        }

        fun isDirectStreamUrl(url: String): Boolean {
            val lower = url.lowercase().substringBefore('?')
            return lower.endsWith(".m3u8") ||
                lower.endsWith(".mp4") ||
                lower.endsWith(".mpd") ||
                lower.endsWith(".webm") ||
                lower.endsWith(".mkv") ||
                lower.endsWith(".mov")
        }

        fun inferFormatBadge(url: String, contentType: String = ""): String {
            val lower = url.lowercase()
            return when {
                lower.contains(".m3u8") || contentType.contains("mpegurl", true) -> "HLS"
                lower.contains(".mpd") || contentType.contains("dash", true) -> "DASH"
                lower.contains(".webm") -> "WEBM"
                lower.contains(".mkv") -> "MKV"
                else -> "MP4"
            }
        }
    }
}
