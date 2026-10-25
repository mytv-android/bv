package dev.aaa1115910.bv.entity.carddata

import dev.aaa1115910.bv.util.formatHourMinSec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private class PublicationDateFormatter(val locale: Locale, val zone: TimeZone) {
    val format = SimpleDateFormat("yyyy-MM-dd", locale).apply { timeZone = zone }
}

private val publicationFormatter = ThreadLocal<PublicationDateFormatter>()

private fun publicationDate(seconds: Int): String {
    val locale = Locale.getDefault()
    val zone = TimeZone.getDefault()
    val cached = publicationFormatter.get()
    val formatter = if (cached != null && cached.locale == locale && cached.zone == zone) {
        cached
    } else {
        PublicationDateFormatter(locale, zone).also(publicationFormatter::set)
    }
    return formatter.format.format(Date(seconds * 1000L))
}

data class VideoCardData(
    val avid: Long,
    val title: String,
    val cover: String,
    val upName: String,
    val reason: String = "",
    val play: Int? = null,
    var playString: String = "",
    val danmaku: Int? = null,
    var danmakuString: String = "",
    val time: Long? = null,
    var timeString: String = "",
    val jumpToSeason: Boolean = false,
    val epId: Int? = null,
    val pubTime: Int? = null,
    var pubTimeString: String = "",
    val upMid: Long? = null,
    /** 历史记录条目才有：删除历史时传给 /x/v2/history/delete 的业务类型（archive / pgc） */
    val historyBusiness: String? = null,
    /** 历史记录条目才有：删除历史时传给 /x/v2/history/delete 的目标 id */
    val historyKid: Long? = null,
) {
    init {
        play?.let {
            playString = if (it >= 10000) "${it / 10000}万" else "$it"
        }
        danmaku?.let {
            danmakuString = if (it >= 10000) "${it / 10000}万" else "$it"
        }
        time?.let {
            timeString = if (it > 0) it.formatHourMinSec() else ""
        }
        pubTime?.let {
            pubTimeString = if (it > 0) publicationDate(it) else ""
        }
    }
}
