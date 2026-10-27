@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.github.mytv.bv.ui.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage

enum class AvatarShape { Cookie, Sunny, Circle }

@Composable
fun AvatarShape.toComposeShape(): Shape = when (this) {
    AvatarShape.Cookie -> MaterialShapes.Cookie9Sided.toShape()
    AvatarShape.Sunny -> MaterialShapes.Sunny.toShape()
    AvatarShape.Circle -> CircleShape
}

/**
 * 头像：默认裁剪为 Cookie9Sided；没有图片时显示名字首字（primaryContainer，字号 = size × 0.4）。
 * shape = Sunny 用于评论头像、等级徽章（背景 secondaryContainer）。
 */
@Composable
fun BvAvatar(
    url: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    fallbackText: String = "",
    shape: AvatarShape = AvatarShape.Cookie,
    containerColor: Color? = null,
    contentColor: Color? = null,
) {
    val sunny = shape == AvatarShape.Sunny
    val container = containerColor
        ?: if (sunny) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    val content = contentColor
        ?: if (sunny) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
    val fontSize = with(LocalDensity.current) { (size * 0.4f).toSp() }
    Box(
        modifier
            .size(size)
            .clip(shape.toComposeShape())
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        if (fallbackText.isNotEmpty()) {
            Text(
                fallbackText.take(1),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = content,
            )
        }
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
