package com.example.data.remote

import com.example.data.model.M3uChannel

object M3uParser {
    private val logoRegex = Regex("""tvg-logo="([^"]+)"""", RegexOption.IGNORE_CASE)
    private val groupRegex = Regex("""group-title="([^"]+)"""", RegexOption.IGNORE_CASE)

    fun parse(rawContent: String, fallbackGroup: String = "کانال‌ها"): List<M3uChannel> {
        if (rawContent.isBlank()) return emptyList()

        val channels = mutableListOf<M3uChannel>()
        var pendingTitle: String? = null
        var pendingLogo: String? = null
        var pendingGroup: String = fallbackGroup

        val lines = rawContent.lines()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            when {
                line.startsWith("#EXTM3U", ignoreCase = true) -> continue
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    pendingLogo = logoRegex.find(line)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
                    pendingGroup = groupRegex.find(line)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
                        ?: fallbackGroup
                    val commaIndex = line.lastIndexOf(',')
                    pendingTitle = if (commaIndex >= 0 && commaIndex < line.lastIndex) {
                        line.substring(commaIndex + 1).trim()
                    } else {
                        null
                    }
                }
                line.startsWith("#") -> continue
                line.startsWith("http://", ignoreCase = true) ||
                    line.startsWith("https://", ignoreCase = true) ||
                    line.startsWith("rtsp://", ignoreCase = true) -> {
                    val title = pendingTitle?.takeIf { it.isNotBlank() }
                        ?: inferTitleFromUrl(line, channels.size + 1)
                    channels.add(
                        M3uChannel(
                            title = title,
                            streamUrl = line,
                            logoUrl = pendingLogo,
                            groupTitle = pendingGroup,
                        )
                    )
                    pendingTitle = null
                    pendingLogo = null
                    pendingGroup = fallbackGroup
                }
            }
        }
        return channels.distinctBy { it.streamUrl }
    }

    private fun inferTitleFromUrl(url: String, index: Int): String {
        val segment = url.substringAfterLast('/').substringBefore('?')
        return segment.takeIf { it.isNotBlank() && it.length <= 60 } ?: "کانال شماره $index"
    }
}
