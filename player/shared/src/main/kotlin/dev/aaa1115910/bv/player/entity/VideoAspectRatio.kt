package dev.aaa1115910.bv.player.entity

import android.content.Context
import dev.aaa1115910.bv.player.shared.R

enum class VideoAspectRatio(private val strRes: Int) {
    Default(R.string.video_aspect_ratio_default),
    FourToThree(R.string.video_aspect_ratio_four_to_three),
    SixteenToNine(R.string.video_aspect_ratio_sixteen_to_nine),
    NineToSixteen(R.string.video_aspect_ratio_nine_to_sixteen),
    EqualWidth(R.string.video_aspect_ratio_equal_width),
    EqualHeight(R.string.video_aspect_ratio_equal_height),
    Stretch(R.string.video_aspect_ratio_stretch);

    fun getDisplayName(context: Context) = context.getString(strRes)

    fun resolveAspectRatio(defaultAspectRatio: Float): Float {
        val fallbackAspectRatio = defaultAspectRatio.takeIf { it > 0f } ?: (16f / 9f)
        return when (this) {
            Default,
            EqualWidth,
            EqualHeight,
            Stretch -> fallbackAspectRatio

            FourToThree -> 4f / 3f
            SixteenToNine -> 16f / 9f
            NineToSixteen -> 9f / 16f
        }
    }
}