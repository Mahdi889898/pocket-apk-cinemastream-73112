package com.example.data.remote

import com.example.data.model.CatalogMovie
import com.example.data.model.MediaType

data class StarterWebsite(
    val id: String,
    val title: String,
    val subtitle: String,
    val url: String,
    val badge: String,
)

object CuratedCatalog {
    val featuredMovies: List<CatalogMovie> = listOf(
        CatalogMovie(
            id = "sintel_hd",
            title = "Sintel — سینتل",
            subtitle = "انیمیشن فانتزی و حماسی محصول بنیاد بلندر درباره دختری در جستجوی اژدهای کوچکش.",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            posterUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8f/Sintel_poster.jpg/640px-Sintel_poster.jpg",
            category = "انیمیشن",
            sourceName = "Blender Open Movies",
            mediaType = MediaType.MOVIE,
            durationLabel = "۱۴:۴۸",
            year = "2010",
        ),
        CatalogMovie(
            id = "tears_of_steel",
            title = "Tears of Steel — اشک‌های فولادی",
            subtitle = "فیلم علمی-تخیلی اکشن درباره نبرد گروهی از دانشمندان و جنگجویان برای نجات آینده.",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            posterUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/70/Tears_of_Steel_poster.jpg/640px-Tears_of_Steel_poster.jpg",
            category = "سینمایی",
            sourceName = "Blender Institute",
            mediaType = MediaType.MOVIE,
            durationLabel = "۱۲:۱۴",
            year = "2012",
        ),
        CatalogMovie(
            id = "big_buck_bunny_hls",
            title = "Big Buck Bunny — بانی بزرگ (HLS)",
            subtitle = "انیمیشن کمدی کلاسیک با استریم تطبیقی کیفیت بالا مناسب تست و تماشای فوری.",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            posterUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/640px-Big_buck_bunny_poster_big.jpg",
            category = "انیمیشن",
            sourceName = "Mux HLS Stream",
            mediaType = MediaType.MOVIE,
            durationLabel = "۰۹:۵۶",
            year = "2008",
        ),
        CatalogMovie(
            id = "elephants_dream",
            title = "Elephants Dream — رویای فیل‌ها",
            subtitle = "نخستین فیلم کوتاه متن‌باز جهان با فضاسازی سورئال و موسیقی متن ارکسترال.",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            posterUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e4/Elephants_Dream_s5_proog.jpg/640px-Elephants_Dream_s5_proog.jpg",
            category = "علمی-تخیلی",
            sourceName = "Orange Open Movie",
            mediaType = MediaType.MOVIE,
            durationLabel = "۱۰:۵۴",
            year = "2006",
        ),
        CatalogMovie(
            id = "tears_hls_adaptive",
            title = "Tears of Steel — استریم چندکیفیتی",
            subtitle = "نسخه استریم HLS با قابلیت انتخاب خودکار کیفیت بر اساس سرعت اینترنت شما.",
            streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            posterUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/70/Tears_of_Steel_poster.jpg/640px-Tears_of_Steel_poster.jpg",
            category = "سینمایی",
            sourceName = "Unified Streaming HLS",
            mediaType = MediaType.MOVIE,
            durationLabel = "۱۲:۱۴",
            year = "2012",
        ),
        CatalogMovie(
            id = "bipbop_hls_channel",
            title = "کانال تست استریم چندزبانه (HLS)",
            subtitle = "کانال نمونه با چند ترک صدا و زیرنویس آزمایشی برای بررسی عملکرد پلیر.",
            streamUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
            posterUrl = null,
            category = "کانال زنده",
            sourceName = "Apple HLS Showcase",
            mediaType = MediaType.LIVE_CHANNEL,
            durationLabel = "LIVE",
            year = "HLS",
            isLive = true,
        ),
    )

    val starterWebsites: List<StarterWebsite> = listOf(
        StarterWebsite(
            id = "archive_movies",
            title = "Internet Archive — فیلم‌های رایگان",
            subtitle = "هزاران فیلم سینمایی کلاسیک، مستند و انیمیشن عمومی با پخش مستقیم",
            url = "https://archive.org/details/movies",
            badge = "آرشیو فیلم",
        ),
        StarterWebsite(
            id = "wikimedia_videos",
            title = "Wikimedia Commons — ویدیوها",
            subtitle = "مجموعه ویدیوها و فیلم‌های کوتاه آزاد و باکیفیت",
            url = "https://commons.wikimedia.org/wiki/Category:Featured_videos",
            badge = "ویدیوی آزاد",
        ),
        StarterWebsite(
            id = "blender_films",
            title = "Blender Studio — فیلم‌های متن‌باز",
            subtitle = "فیلم‌ها و انیمیشن‌های باکیفیت استودیوی بلندر",
            url = "https://studio.blender.org/films/",
            badge = "انیمیشن HD",
        ),
        StarterWebsite(
            id = "nasa_plus",
            title = "NASA+ — مستندهای فضایی",
            subtitle = "مستندها و پخش‌های فضایی ناسا بدون اشتراک",
            url = "https://plus.nasa.gov/",
            badge = "مستند",
        ),
    )
}
