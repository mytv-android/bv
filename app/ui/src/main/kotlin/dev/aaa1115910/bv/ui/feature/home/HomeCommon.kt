package dev.aaa1115910.bv.ui.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.AvatarShape
import dev.aaa1115910.bv.ui.designsystem.component.BvAvatar
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.VideoCard
import dev.aaa1115910.bv.ui.designsystem.component.toComposeShape
import dev.aaa1115910.bv.entity.carddata.VideoCardData

/** 顶栏头像（40dp，Cookie9），点击打开「我的」。未登录时显示人像图标。 */
@Composable
internal fun HomeAvatar(state: HomeState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val login = state.user.isLogin
    val face = state.user.face.takeIf { login && it.isNotBlank() }
    val name = if (login) state.user.username else ""
    val label = stringResource(R.string.home_avatar)
    Box(
        modifier
            .size(40.dp)
            .clip(AvatarShape.Cookie.toComposeShape())
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        BvAvatar(url = face, size = 40.dp, fallbackText = name)
        if (face == null && name.isBlank()) {
            Icon(
                Icons.Rounded.Person,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** 「继续观看」横向一行：VideoCard(width, progress, showMeta = false)。 */
@Composable
internal fun ContinueRow(
    items: List<ContinueItem>,
    cardWidth: Dp,
    spacing: Dp,
    onOpen: (VideoCardData) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyRow(
        modifier = modifier,
        state = rememberLazyListState(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        itemsIndexed(items) { _, item ->
            VideoCard(
                data = item.card,
                onClick = { onOpen(item.card) },
                width = cardWidth,
                progress = item.progress,
                showMeta = false,
            )
        }
    }
}

/** 分区标签横排（BvChip × 31）。 */
@Composable
internal fun RegionChipRow(
    names: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (selected - 1).coerceAtLeast(0))
    // 外部切换分区（平板快捷分区）时，把选中的标签滚到可见处
    LaunchedEffect(selected) {
        val info = listState.layoutInfo
        val target = info.visibleItemsInfo.firstOrNull { it.index == selected }
        val fullyVisible = target != null &&
            target.offset >= info.viewportStartOffset && target.offset + target.size <= info.viewportEndOffset
        if (!fullyVisible) listState.animateScrollToItem((selected - 1).coerceAtLeast(0))
    }
    LazyRow(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(names) { index, name ->
            BvChip(text = name, selected = index == selected, onClick = { onSelect(index) })
        }
    }
}
