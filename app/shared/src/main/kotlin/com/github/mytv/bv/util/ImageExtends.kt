package com.github.mytv.bv.util

fun String.resizedImageUrl(size: ImageSize): String {
    // 已带处理参数（如「…@466h_622w.webp」）的 URL 不再追加，避免二次后缀导致 404
    if (size == ImageSize.Default || contains('@')) return this
    return "$this@${size.sizeString}.webp"
}

enum class ImageSize(val sizeString: String) {
    Default(""),
    Cover("180h_288w_1c"),
    SmallVideoCardCover("400h_640w_1c"),
    SeasonCoverThumbnail("466h_622w")
}