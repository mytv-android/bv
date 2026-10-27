package com.github.mytv.bv.ui.feature.tools

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.adaptive.DeviceLayout
import com.github.mytv.bv.ui.designsystem.adaptive.LocalDeviceProfile
import com.github.mytv.bv.ui.designsystem.component.BvButton
import com.github.mytv.bv.ui.designsystem.component.BvButtonStyle
import com.github.mytv.bv.ui.designsystem.component.BvListItem
import com.github.mytv.bv.ui.designsystem.component.FullPageLoading
import com.github.mytv.bv.ui.designsystem.component.PageMessage
import com.github.mytv.bv.ui.designsystem.focus.BvKey
import com.github.mytv.bv.ui.designsystem.focus.TvColumnPivot
import com.github.mytv.bv.ui.designsystem.focus.bvFocus
import com.github.mytv.bv.ui.designsystem.focus.focusContainerColor
import com.github.mytv.bv.ui.designsystem.focus.focusContentColor
import com.github.mytv.bv.ui.designsystem.focus.onBvKey
import com.github.mytv.bv.ui.designsystem.theme.BvType
import com.github.mytv.bv.util.CodecInfoData
import com.github.mytv.bv.util.CodecMedia
import com.github.mytv.bv.util.CodecMode
import com.github.mytv.bv.util.CodecType
import com.github.mytv.bv.util.CodecUtil
import com.github.mytv.bv.util.SupportedFrameRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private fun Int.toBps(): String = when {
    this >= 1000000 -> "${this / 1000000} Mbps"
    this >= 1000 -> "${this / 1000} Kbps"
    else -> "$this bps"
}

private fun resolutionName(context: Context, height: Int): String = context.getString(
    when (height) {
        360 -> R.string.tools_codec_res_360p
        480 -> R.string.tools_codec_res_480p
        720 -> R.string.tools_codec_res_720p
        1080 -> R.string.tools_codec_res_1080p
        1440 -> R.string.tools_codec_res_1440p
        2160 -> R.string.tools_codec_res_2160p
        4320 -> R.string.tools_codec_res_4320p
        else -> R.string.tools_codec_res_unknown
    }
)

private fun frameRatesText(context: Context, rates: List<SupportedFrameRate>): String =
    rates.joinToString("\n") { rate ->
        resolutionName(context, rate.resolution.second) + ": " +
            if (rate.unsupported) context.getString(R.string.tools_codec_frame_unsupported)
            else "${String.format(Locale.getDefault(), "%.1f", rate.frameRate.upper)}fps"
    }

/** 解码器详情条目（照抄旧电视 MediaCodecDetails 的字段与条件）。 */
private fun codecDetails(context: Context, codec: CodecInfoData): List<Pair<String, String>> = buildList {
    add(
        context.getString(R.string.tools_codec_hs_title) to context.getString(
            when (codec.mode) {
                CodecMode.Hardware -> R.string.tools_codec_hs_hardware
                CodecMode.Software -> R.string.tools_codec_hs_software
            }
        )
    )
    add(context.getString(R.string.tools_codec_instances_title) to codec.maxSupportedInstances.toString())
    val video = codec.media == CodecMedia.Video
    if (video) add(context.getString(R.string.tools_codec_color_formats_title) to codec.colorFormats.joinToString())
    if (codec.media == CodecMedia.Audio) {
        add(
            context.getString(R.string.tools_codec_audio_bitrate_title) to
                "${codec.audioBitrateRange?.first?.toBps()} - ${codec.audioBitrateRange?.last?.toBps()}"
        )
    }
    if (video) {
        add(
            context.getString(R.string.tools_codec_video_max_bitrate_title) to
                (codec.videoBitrateRange?.last?.toBps() ?: context.getString(R.string.tools_codec_res_unknown))
        )
        add(
            context.getString(R.string.tools_codec_video_frame_range_title) to
                "${codec.videoFrame?.first}fps - ${codec.videoFrame?.last}fps"
        )
        add(context.getString(R.string.tools_codec_video_frame_supported_title) to frameRatesText(context, codec.supportedFrameRates))
        add(context.getString(R.string.tools_codec_video_frame_achievable_title) to frameRatesText(context, codec.achievableFrameRates))
    }
}

/**
 * 解码器信息（第 9.11 节模板；逻辑照抄旧电视 MediaCodecScreen）：
 * 列出所有解码器；平板 / 电视为两栏（左列表 + 右详情，电视聚焦即切换、→ 进详情、← 回列表），
 * 手机点击解码器弹出详情。
 */
@Composable
fun MediaCodecScreen(nav: NavController) {
    val context = LocalContext.current
    val layout = LocalDeviceProfile.current.layout
    val tv = layout == DeviceLayout.Tv
    val decoders = remember { mutableStateListOf<CodecInfoData>() }
    var loading by remember { mutableStateOf(true) }
    var current by remember { mutableStateOf<CodecInfoData?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    val firstRequester = remember { FocusRequester() }
    val detailRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        val list = withContext(Dispatchers.IO) {
            runCatching { CodecUtil.parseCodecs().filter { it.type == CodecType.Decoder } }.getOrDefault(emptyList())
        }
        decoders.clear()
        decoders.addAll(list)
        if (layout != DeviceLayout.Phone) current = list.firstOrNull()
        loading = false
        if (tv) {
            withFrameNanos { }
            firstRequester.tryToolFocus()
        }
    }

    ToolPage(title = stringResource(R.string.tools_codec_title), onBack = { nav.popBackStack() }) { insets ->
        when {
            loading -> FullPageLoading()
            decoders.isEmpty() -> PageMessage(stringResource(R.string.tools_codec_empty))
            layout == DeviceLayout.Phone -> CodecList(
                decoders = decoders,
                selected = null,
                contentPadding = insets.padding,
                firstRequester = firstRequester,
                onClick = {
                    current = it
                    showDialog = true
                },
                onFocused = {},
                onRight = { false },
                modifier = Modifier.fillMaxSize(),
            )

            else -> Row(
                Modifier
                    .fillMaxSize()
                    .padding(insets.padding),
                horizontalArrangement = Arrangement.spacedBy(insets.spacing),
            ) {
                CodecList(
                    decoders = decoders,
                    selected = current,
                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp),
                    firstRequester = firstRequester,
                    onClick = {
                        current = it
                        if (tv) detailRequester.tryToolFocus()
                    },
                    onFocused = { if (tv) current = it },
                    onRight = { detailRequester.tryToolFocus() },
                    modifier = Modifier
                        .width(if (tv) 300.dp else 380.dp)
                        .fillMaxHeight(),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (tv) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            RoundedCornerShape(28.dp),
                        ),
                ) {
                    val codec = current
                    if (codec != null) {
                        key(codec.name, codec.mimeType) {
                            TvColumnPivot {
                                Column(
                                    Modifier
                                        .fillMaxSize()
                                        .focusRequester(detailRequester)
                                        .focusGroup()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 26.dp, vertical = 22.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    CodecDetails(context, codec, focusable = tv)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val dialogCodec = current
    if (showDialog && dialogCodec != null && layout == DeviceLayout.Phone) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Column(
                    Modifier
                        .widthIn(min = 280.dp, max = 520.dp)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(
                        Modifier
                            .heightIn(max = 480.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CodecDetails(context, dialogCodec, focusable = false)
                    }
                    BvButton(
                        text = stringResource(R.string.tools_close),
                        onClick = { showDialog = false },
                        modifier = Modifier.align(Alignment.End),
                        style = BvButtonStyle.Tonal,
                    )
                }
            }
        }
    }
}

@Composable
private fun CodecList(
    decoders: List<CodecInfoData>,
    selected: CodecInfoData?,
    contentPadding: PaddingValues,
    firstRequester: FocusRequester,
    onClick: (CodecInfoData) -> Unit,
    onFocused: (CodecInfoData) -> Unit,
    onRight: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    TvColumnPivot {
        LazyColumn(
            modifier
                .focusRestorer()
                .focusGroup(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(
                items = decoders,
                key = { _, codec -> "${codec.media}-${codec.name}-${codec.mimeType}" },
                contentType = { _, _ -> "codec" },
            ) { index, codec ->
                val video = codec.media == CodecMedia.Video
                val isSelected = codec == selected
                BvListItem(
                    title = codec.name,
                    modifier = Modifier
                        .then(if (index == 0) Modifier.focusRequester(firstRequester) else Modifier)
                        .onBvKey { key -> if (key == BvKey.Right) onRight() else false },
                    subtitle = codec.mimeType + " · " + stringResource(
                        when (codec.mode) {
                            CodecMode.Hardware -> R.string.tools_codec_hs_hardware
                            CodecMode.Software -> R.string.tools_codec_hs_software
                        }
                    ),
                    icon = if (video) Icons.Rounded.Videocam else Icons.Rounded.Audiotrack,
                    iconContainerColor = if (video) scheme.primaryContainer else scheme.tertiaryContainer,
                    iconContentColor = if (video) scheme.onPrimaryContainer else scheme.onTertiaryContainer,
                    selected = isSelected,
                    onClick = { onClick(codec) },
                    onFocused = { if (it) onFocused(codec) },
                )
            }
        }
    }
}

/** 详情条目：标题 BodyBold + 内容 Small onSurfaceVariant（多行）；电视上每条可聚焦，便于方向键滚动。 */
@Composable
private fun CodecDetails(context: Context, codec: CodecInfoData, focusable: Boolean) {
    Text(codec.name, style = BvType.PanelTitle, color = MaterialTheme.colorScheme.onSurface)
    Text(codec.mimeType, style = BvType.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val details = remember(codec) { codecDetails(context, codec) }
    details.forEach { (title, text) -> CodecDetailItem(title, text, focusable) }
}

@Composable
private fun CodecDetailItem(title: String, text: String, focusable: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val body: @Composable (Color, Color) -> Unit = { titleColor, textColor ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = BvType.BodyBold, color = titleColor)
            Text(text, style = BvType.Small, color = textColor)
        }
    }
    if (focusable) {
        val interaction = remember { MutableInteractionSource() }
        val focused by interaction.collectIsFocusedAsState()
        val container by focusContainerColor(focused, scheme.surfaceContainerHigh)
        val content by focusContentColor(focused, scheme.onSurface)
        Surface(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .bvFocus(scale = 1.04f, corner = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = container,
            contentColor = content,
            interactionSource = interaction,
        ) { body(content, content.copy(alpha = .8f)) }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = scheme.surfaceContainerHigh,
            contentColor = scheme.onSurface,
        ) { body(scheme.onSurface, scheme.onSurfaceVariant) }
    }
}
