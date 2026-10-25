package dev.aaa1115910.bv.entity.carddata

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoCardDataTest {
    @Test
    fun `publication date follows timezone changes on the same thread`() {
        val originalZone = TimeZone.getDefault()
        val originalLocale = Locale.getDefault()
        try {
            val seconds = 1_600_027_200 // 2020-09-13 20:00 UTC
            Locale.setDefault(Locale.US)
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            assertEquals("2020-09-13", card(seconds).pubTimeString)
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+08:00"))
            assertEquals("2020-09-14", card(seconds).pubTimeString)
        } finally {
            TimeZone.setDefault(originalZone)
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `publication date follows locale changes on the same thread`() {
        val originalLocale = Locale.getDefault()
        val originalZone = TimeZone.getDefault()
        try {
            val seconds = 1_600_041_600
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            Locale.setDefault(Locale.US)
            card(seconds)
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            val expected = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date(seconds * 1000L))
            assertEquals(expected, card(seconds).pubTimeString)
        } finally {
            Locale.setDefault(originalLocale)
            TimeZone.setDefault(originalZone)
        }
    }

    private fun card(seconds: Int) = VideoCardData(
        avid = 1,
        title = "title",
        cover = "",
        upName = "up",
        pubTime = seconds,
    )
}
