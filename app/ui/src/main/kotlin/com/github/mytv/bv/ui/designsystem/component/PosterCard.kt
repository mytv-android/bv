package com.github.mytv.bv.ui.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.mytv.bv.entity.carddata.SeasonCardData
import com.github.mytv.bv.ui.designsystem.theme.BvType

/** 原型 `poster()`：3:4 海报卡，左上角评分角标。 */
@Composable
fun PosterCard(
    data: SeasonCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    subtitle: String? = data.subTitle,
    onLongClick: (() -> Unit)? = null,
) {
    PosterCard(
        title = data.title,
        cover = data.cover,
        onClick = onClick,
        modifier = modifier,
        width = width,
        rating = data.rating,
        subtitle = subtitle,
        onLongClick = onLongClick,
    )
}

@Composable
fun PosterCard(
    title: String,
    cover: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    rating: String? = null,
    subtitle: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    CardFrame(
        cover = cover,
        aspectRatio = 3f / 4f,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        width = width,
        coverContent = {
            if (!rating.isNullOrBlank()) {
                Text(
                    rating,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    style = BvType.Badge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) {
        Text(
            title,
            style = BvType.CardTitle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = BvType.Meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
