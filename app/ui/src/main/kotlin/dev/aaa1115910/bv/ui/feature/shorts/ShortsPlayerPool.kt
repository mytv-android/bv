package dev.aaa1115910.bv.ui.feature.shorts

import android.content.Context
import android.view.TextureView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.entity.ShortsItem
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.toAudioLoudness
import dev.aaa1115910.bv.player.impl.exo.ExoMediaPlayer
import dev.aaa1115910.bv.repository.PlayUrlResolver
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.viewmodel.ShortsViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import dev.aaa1115910.bv.R as SharedR

/** 槽位状态：Idle 未加载 → Loading 解析地址 → Preparing 已 prepare → Ready 首次缓冲完成；Error 失败 */
internal enum class SlotState { Idle, Loading, Preparing, Ready, Error }

/**
 * 播放器池里的一个实例（第 10.4 节）。界面只读这里的 Compose 状态；所有写入都在主线程。
 */
@Stable
internal class ShortsPlayerSlot(val index: Int) {
    internal var player: ExoMediaPlayer? = null
    internal var listener: Player.Listener? = null
    internal var loadJob: Job? = null
    internal var retried = false
    internal var played = false

    /** 已绑定画面的 TextureView（后绑定的在最后，生效的是最后一个） */
    internal val views = mutableListOf<TextureView>()
    internal var boundView: TextureView? = null

    /** 当前装载的视频；null 表示空闲 */
    var item by mutableStateOf<ShortsItem?>(null)
        internal set

    /** 每次分配给新视频或回收时加一，界面据此重新绑定画面 */
    var assignId by mutableIntStateOf(0)
        internal set

    var state by mutableStateOf(SlotState.Idle)
        internal set
    var firstFrameRendered by mutableStateOf(false)
        internal set
    var isPlaying by mutableStateOf(false)
        internal set
    var buffering by mutableStateOf(false)
        internal set
    var positionMs by mutableLongStateOf(0L)
        internal set
    var durationMs by mutableLongStateOf(0L)
        internal set
    var videoWidth by mutableIntStateOf(0)
        internal set
    var videoHeight by mutableIntStateOf(0)
        internal set

    internal fun attach(view: TextureView) {
        views.remove(view)
        views.add(view)
        bindSurface()
    }

    internal fun detach(view: TextureView) {
        if (views.remove(view)) bindSurface()
    }

    /** 把最后绑定的 TextureView 设为播放器的画面 */
    internal fun bindSurface() {
        val exo = player?.mPlayer
        if (exo == null) {
            boundView = null
            return
        }
        val target = views.lastOrNull()
        if (target === boundView) return
        boundView?.let { exo.clearVideoTextureView(it) }
        if (target != null) {
            // 暂停时换画面不一定会重新出帧，先显示封面
            if (!exo.isPlaying) firstFrameRendered = false
            exo.setVideoTextureView(target)
        }
        boundView = target
    }

    internal fun resetPlayback(item: ShortsItem?) {
        state = SlotState.Idle
        firstFrameRendered = false
        isPlaying = false
        buffering = false
        positionMs = 0L
        durationMs = (item?.duration ?: 0) * 1000L
        videoWidth = item?.width ?: 0
        videoHeight = item?.height ?: 0
        retried = false
        played = false
    }
}

/**
 * 刷刷播放器池（第 10.4 节）：3 个 [ExoMediaPlayer] 实例。
 *
 * - 第 i 页用一个实例播放；i+1 prepare 后静音暂停；i−1 保留暂停；超出范围的实例 stop 后复用。
 * - 移动网络（[bind] 的 maxQuality 返回非 null）：清晰度上限 720P，只预准备下一条，且等当前条缓冲好再预准备。
 * - 页面不可见：全部暂停并上报进度（[onHidden]），停止后释放当前条之外的实例（[releaseInactive]）。
 * - 刷刷默认关闭弹幕（池里不创建弹幕播放器）。
 *
 * 由 [ShortsPlayerPoolHolder]（与 ShortsViewModel 同一个 ViewModelStore）持有，导航离开再回来时当前条还在。
 */
@Stable
internal class ShortsPlayerPool(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    companion object {
        private val logger = KotlinLogging.logger { }
        const val SLOT_COUNT = 3
        private const val TICK_MS = 100L
        private const val HEARTBEAT_MS = 15_000L
    }

    val slots: List<ShortsPlayerSlot> = List(SLOT_COUNT) { ShortsPlayerSlot(it) }

    /** 当前播放的视频 */
    var currentAid by mutableStateOf<Long?>(null)
        private set

    /** 用户点击暂停（换条后重置） */
    var userPaused by mutableStateOf(false)
        private set

    /** 长按 2 倍速中 */
    var boosting by mutableStateOf(false)
        private set

    val currentSlot: ShortsPlayerSlot? get() = currentAid?.let { slotFor(it) }

    private var viewModel: ShortsViewModel? = null
    private var maxQuality: () -> Resolution? = { null }
    private var incognito = false
    private var visible = false
    private var released = false
    private var lastIndex = 0
    private var lastItems: List<ShortsItem> = emptyList()
    private var sinceHeartbeatMs = 0L

    private val options by lazy {
        val apiType = Prefs.apiType
        VideoPlayerOptions(
            userAgent = when (apiType) {
                ApiType.Web -> context.getString(SharedR.string.video_player_user_agent_http)
                ApiType.App -> context.getString(SharedR.string.video_player_user_agent_client)
            },
            referer = when (apiType) {
                ApiType.Web -> context.getString(SharedR.string.video_player_referer)
                ApiType.App -> null
            },
            enableFfmpegAudioRenderer = Prefs.enableFfmpegAudioRenderer,
            enableSkipSilence = Prefs.playerSkipSilence,
            audioBalanceLevel = Prefs.playerAudioBalanceLevel
        )
    }

    private var tickJob: Job? = null

    /**
     * @param maxQuality 每次加载时调用；返回非 null 表示按流量计费（移动网络），清晰度不超过该值
     */
    fun bind(vm: ShortsViewModel, maxQuality: () -> Resolution?) {
        viewModel = vm
        this.maxQuality = maxQuality
        incognito = runCatching { Prefs.incognitoMode }.getOrDefault(false)
    }

    fun slotFor(aid: Long): ShortsPlayerSlot? = slots.firstOrNull { it.item?.aid == aid }

    /**
     * 页面停稳（150ms 后）调用：第 [index] 条成为当前条，并按需分配、预准备相邻的实例
     */
    fun update(index: Int, items: List<ShortsItem>) {
        if (released) return
        lastIndex = index
        lastItems = items
        if (!visible) return
        val current = items.getOrNull(index) ?: return
        if (currentAid != current.aid) {
            currentSlot?.let { old ->
                reportProgress(old)
                old.player?.mPlayer?.setPlaybackSpeed(1f)
            }
            currentAid = current.aid
            userPaused = false
            boosting = false
            sinceHeartbeatMs = 0L
        }

        val metered = maxQuality() != null
        val next = items.getOrNull(index + 1)
        val prev = items.getOrNull(index - 1)
        val prevLoaded = prev != null && (slotFor(prev.aid)?.state ?: SlotState.Idle) != SlotState.Idle
        val wanted = buildList {
            add(current)
            if (next != null) add(next)
            if (prev != null && (!metered || prevLoaded)) add(prev)
        }
        val wantedAids = wanted.mapTo(HashSet()) { it.aid }
        val free = slots.filter { slot ->
            val aid = slot.item?.aid
            aid == null || aid !in wantedAids
        }.toMutableList()
        wanted.forEach { item ->
            if (slotFor(item.aid) == null) free.removeFirstOrNull()?.let { assign(it, item) }
        }
        free.forEach { if (it.item != null) recycle(it, releasePlayer = false) }
        slots.forEach { applyRole(it) }

        val playing = slotFor(current.aid)
        playing?.let { ensureLoaded(it) }
        val neighboursAllowed = !metered || playing?.state == SlotState.Ready
        if (neighboursAllowed) {
            wanted.drop(1).forEach { item -> slotFor(item.aid)?.let { ensureLoaded(it) } }
        }
    }

    /** 单击：暂停 / 继续；失败状态下重试 */
    fun togglePlay(aid: Long) {
        if (aid != currentAid) return
        val slot = currentSlot ?: return
        if (slot.state == SlotState.Error) {
            retry(aid)
            return
        }
        userPaused = !userPaused
        if (userPaused) reportProgress(slot)
        applyRole(slot)
    }

    fun retry(aid: Long) {
        val slot = slotFor(aid) ?: return
        slot.loadJob?.cancel()
        slot.retried = false
        slot.state = SlotState.Idle
        ensureLoaded(slot)
    }

    /** 长按 2 倍速 */
    fun setBoost(on: Boolean) {
        if (boosting == on) return
        boosting = on
        currentSlot?.let { applyRole(it) }
    }

    fun seekTo(aid: Long, positionMs: Long) {
        if (aid != currentAid) return
        val slot = currentSlot ?: return
        val target = positionMs.coerceAtLeast(0L)
        slot.player?.seekTo(target)
        slot.positionMs = target
    }

    // ---------------- 可见性 ----------------

    fun onVisible() {
        if (released) return
        visible = true
        // 页面可见才启动 100ms 进度轮询；隐藏时不再空转
        if (tickJob == null) {
            tickJob = scope.launch { tick() }
        }
        if (lastItems.isNotEmpty()) update(lastIndex, lastItems) else slots.forEach { applyRole(it) }
    }

    fun onHidden() {
        if (!visible) return
        visible = false
        tickJob?.cancel()
        tickJob = null
        currentSlot?.let { reportProgress(it) }
        slots.forEach { it.player?.mPlayer?.playWhenReady = false }
    }

    /** 释放当前条之外的实例 */
    fun releaseInactive() {
        if (released) return
        val current = currentAid
        slots.forEach { slot ->
            val aid = slot.item?.aid
            if (aid == null || aid != current) recycle(slot, releasePlayer = true)
        }
    }

    fun release() {
        if (released) return
        currentSlot?.let { reportProgress(it) }
        visible = false
        tickJob?.cancel()
        tickJob = null
        released = true
        slots.forEach { recycle(it, releasePlayer = true) }
    }

    // ---------------- 内部 ----------------

    private suspend fun tick() {
        while (true) {
            delay(TICK_MS)
            if (!visible || released) continue
            val slot = currentSlot ?: continue
            val exo = slot.player?.mPlayer ?: continue
            slot.positionMs = exo.currentPosition
            exo.duration.takeIf { it > 0 }?.let { slot.durationMs = it }
            if (exo.isPlaying) {
                sinceHeartbeatMs += TICK_MS
                if (sinceHeartbeatMs >= HEARTBEAT_MS) {
                    sinceHeartbeatMs = 0L
                    reportProgress(slot)
                }
            }
        }
    }

    private fun assign(slot: ShortsPlayerSlot, item: ShortsItem) {
        recycle(slot, releasePlayer = false)
        slot.item = item
        slot.resetPlayback(item)
    }

    private fun recycle(slot: ShortsPlayerSlot, releasePlayer: Boolean) {
        slot.loadJob?.cancel()
        slot.loadJob = null
        slot.player?.mPlayer?.let { exo ->
            exo.playWhenReady = false
            exo.clearVideoSurface()
            if (!releasePlayer) {
                exo.stop()
                exo.clearMediaItems()
            }
        }
        if (releasePlayer) {
            slot.player?.release()
            slot.player = null
            slot.listener = null
        }
        slot.boundView = null
        slot.views.clear()
        slot.item = null
        slot.assignId++
        slot.resetPlayback(null)
    }

    private fun ensureLoaded(slot: ShortsPlayerSlot) {
        val item = slot.item ?: return
        if (slot.state != SlotState.Idle) return
        val vm = viewModel ?: return
        if (item.cid <= 0) {
            slot.state = SlotState.Error
            return
        }
        slot.state = SlotState.Loading
        val cap = maxQuality()
        val assignId = slot.assignId
        slot.loadJob = scope.launch {
            val result = try {
                Result.success(vm.resolvePlayUrl(item, cap))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Result.failure(e)
            }
            if (released || slot.assignId != assignId) return@launch
            result.onSuccess { url ->
                val player = slot.player ?: createPlayer(slot)
                if (url.width > 0 && url.height > 0) {
                    slot.videoWidth = url.width
                    slot.videoHeight = url.height
                }
                // 每条视频有自己的响度元数据；没有元数据时处理器旁路
                player.setAudioLoudness(url.playData.volume?.toAudioLoudness())
                player.playUrl(url.videoUrl, url.audioUrl)
                player.prepare()
                slot.state = SlotState.Preparing
                applyRole(slot)
            }.onFailure {
                logger.fWarn { "Resolve shorts play url failed: [av=${item.aid}] ${it.stackTraceToString()}" }
                slot.state = SlotState.Error
            }
        }
    }

    private fun createPlayer(slot: ShortsPlayerSlot): ExoMediaPlayer {
        logger.fInfo { "Create shorts player #${slot.index}" }
        val player = ExoMediaPlayer(context, options)
        val listener = listenerFor(slot)
        player.mPlayer?.let { exo ->
            exo.repeatMode = Player.REPEAT_MODE_ONE
            exo.playWhenReady = false
            exo.addListener(listener)
        }
        slot.player = player
        slot.listener = listener
        slot.bindSurface()
        return player
    }

    private fun applyRole(slot: ShortsPlayerSlot) {
        val exo = slot.player?.mPlayer ?: return
        val aid = slot.item?.aid
        if (aid != null && aid == currentAid) {
            exo.volume = 1f
            exo.setPlaybackSpeed(if (boosting) 2f else 1f)
            exo.playWhenReady = visible && !userPaused
        } else {
            exo.playWhenReady = false
            exo.volume = 0f
            if (exo.playbackParameters.speed != 1f) exo.setPlaybackSpeed(1f)
        }
    }

    private fun onCurrentReady() {
        // 移动网络下等当前条缓冲好再预准备下一条
        if (maxQuality() != null) update(lastIndex, lastItems)
    }

    private fun onSlotError(slot: ShortsPlayerSlot, error: PlaybackException) {
        logger.fWarn { "Shorts player #${slot.index} error: [av=${slot.item?.aid}] ${error.stackTraceToString()}" }
        if (slot.item == null) return
        if (!slot.retried) {
            // 地址过期等情况：重新解析一次
            slot.retried = true
            slot.state = SlotState.Idle
            ensureLoaded(slot)
        } else {
            slot.state = SlotState.Error
        }
    }

    private fun listenerFor(slot: ShortsPlayerSlot) = object : Player.Listener {
        override fun onRenderedFirstFrame() {
            slot.firstFrameRendered = true
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            slot.isPlaying = isPlaying
            if (isPlaying) slot.played = true
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            slot.buffering = playbackState == Player.STATE_BUFFERING
            if (playbackState == Player.STATE_READY) {
                slot.player?.mPlayer?.duration?.takeIf { it > 0 }?.let { slot.durationMs = it }
                if (slot.state == SlotState.Preparing) {
                    slot.state = SlotState.Ready
                    val aid = slot.item?.aid
                    if (aid != null && aid == currentAid) onCurrentReady()
                }
            }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                slot.videoWidth = (videoSize.width * videoSize.pixelWidthHeightRatio).toInt()
                slot.videoHeight = videoSize.height
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            onSlotError(slot, error)
        }
    }

    /** 与播放器相同：上报秒数，播完为 -1；无痕模式、没播过、时长未知时不上报 */
    private fun reportProgress(slot: ShortsPlayerSlot) {
        if (incognito || !slot.played) return
        val item = slot.item ?: return
        val vm = viewModel ?: return
        val exo = slot.player?.mPlayer
        val position = exo?.currentPosition ?: slot.positionMs
        val duration = exo?.duration?.takeIf { it > 0 } ?: slot.durationMs
        if (duration <= 0) return
        val seconds = (position.coerceAtLeast(0L) / 1000).toInt()
        val total = (duration / 1000).toInt()
        vm.reportHistory(item, if (seconds >= total) -1 else seconds)
    }
}

/** 与 ShortsViewModel 同一个 ViewModelStore：返回刷刷时当前条仍保持 prepare 状态 */
internal class ShortsPlayerPoolHolder(context: Context) : ViewModel() {
    val pool = ShortsPlayerPool(context.applicationContext, viewModelScope)

    override fun onCleared() {
        pool.release()
    }
}

/**
 * 创建（或取回）播放器池，并按页面生命周期暂停 / 恢复：ON_PAUSE 全部暂停，ON_STOP 释放当前条之外的实例。
 */
@Composable
internal fun rememberShortsPlayerPool(vm: ShortsViewModel): ShortsPlayerPool {
    val appContext = LocalContext.current.applicationContext
    val isTv = LocalDeviceProfile.current.isTv
    val resolver: PlayUrlResolver = koinInject()
    val holder = viewModel { ShortsPlayerPoolHolder(appContext) }
    val pool = holder.pool
    remember(pool, vm, isTv) {
        // 电视与 Wi-Fi 用用户设置的默认清晰度；移动网络上限 720P
        pool.bind(vm) { if (isTv) null else resolver.recommendedMaxQuality(appContext) }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, pool) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> pool.onVisible()
                Lifecycle.Event.ON_PAUSE -> pool.onHidden()
                Lifecycle.Event.ON_STOP -> pool.releaseInactive()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            pool.onHidden()
            pool.releaseInactive()
        }
    }
    return pool
}

/**
 * 视频画面：TextureView（可被圆角裁剪）+ 首帧出来前的封面。
 *
 * @param crop true = 铺满裁剪（竖屏视频），false = 完整显示
 * @param showStatus 当前条加载中显示加载指示器，失败显示「播放失败 / 重试」
 */
@Composable
internal fun ShortsVideo(
    pool: ShortsPlayerPool,
    item: ShortsItem,
    modifier: Modifier = Modifier,
    crop: Boolean = item.isPortrait,
    showStatus: Boolean = true,
) {
    val context = LocalContext.current
    val slot = pool.slotFor(item.aid)
    val assignId = slot?.assignId ?: -1
    // 画面在屏幕上时保持亮屏
    val textureView = remember { TextureView(context).apply { keepScreenOn = true } }
    DisposableEffect(slot, assignId, textureView) {
        slot?.attach(textureView)
        onDispose { slot?.detach(textureView) }
    }
    // 画面尺寸在 Layout 阶段算：省掉每个 pager 页一次 SubcomposeLayout
    Layout(
        modifier = modifier.clipToBounds(),
        content = {
            Box {
                AndroidView(factory = { textureView }, modifier = Modifier.fillMaxSize())
                val coverAlpha by animateFloatAsState(
                    if (slot?.firstFrameRendered == true) 0f else 1f,
                    MaterialTheme.motionScheme.fastEffectsSpec(),
                    label = "shortsCover"
                )
                if (coverAlpha > 0.01f) {
                    AsyncImage(
                        model = item.cover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = coverAlpha },
                    )
                }
            }
            if (showStatus && slot != null && pool.currentAid == item.aid) {
                Box {
                    when {
                        slot.state == SlotState.Error -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(stringResource(R.string.shorts_play_failed), style = BvType.Body, color = Color.White)
                            BvButton(
                                text = stringResource(R.string.ds_retry),
                                onClick = { pool.retry(item.aid) },
                                style = BvButtonStyle.Tonal,
                            )
                        }

                        !slot.firstFrameRendered || slot.buffering -> LoadingIndicator()
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val containerW = constraints.maxWidth
        val containerH = constraints.maxHeight
        val vw = slot?.videoWidth?.takeIf { it > 0 } ?: item.width
        val vh = slot?.videoHeight?.takeIf { it > 0 } ?: item.height
        val ratio = if (vw > 0 && vh > 0) {
            vw.toFloat() / vh
        } else {
            if (item.isPortrait) 9f / 16f else 16f / 9f
        }
        val boxRatio = if (containerH > 0) containerW.toFloat() / containerH else ratio
        // 完整显示时宽于容器的视频按宽度铺满；裁剪时窄于容器的视频按宽度铺满
        val fitWidth = (ratio > boxRatio) != crop
        val w = (if (fitWidth) containerW.toFloat() else containerH * ratio).roundToInt().coerceAtLeast(1)
        val h = (if (fitWidth) containerW / ratio else containerH.toFloat()).roundToInt().coerceAtLeast(1)
        val video = measurables[0].measure(Constraints.fixed(w, h))
        val status = measurables.getOrNull(1)?.measure(
            Constraints(maxWidth = containerW, maxHeight = containerH)
        )
        layout(containerW, containerH) {
            video.place((containerW - w) / 2, (containerH - h) / 2)
            status?.place(
                (containerW - status.width) / 2,
                (containerH - status.height) / 2,
            )
        }
    }
}
