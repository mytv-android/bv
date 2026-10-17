package dev.aaa1115910.bv.player.impl.exo

import android.content.Context
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.PlayerHttpClient
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.cdn.FrequentBufferingDetector
import dev.aaa1115910.bv.util.formatHourMinSec
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI

/** 低于该倍速不启用跳过静音：压缩音频时间轴会让作为主时钟的音频位置快进，画面跟着跳帧 */
private const val SKIP_SILENCE_MIN_SPEED = 2.5f

/** 同一个视频最多自动切换 CDN 候选地址的次数（播放失败与频繁缓冲共用此预算） */
private const val MAX_CDN_SWITCHES = 3

@OptIn(UnstableApi::class)
class ExoMediaPlayer(
    private val context: Context,
    private val options: VideoPlayerOptions
) : AbstractVideoPlayer(), Player.Listener {
    private companion object {
        private val logger = KotlinLogging.logger("ExoMediaPlayer")
    }

    var mPlayer: ExoPlayer? = null
    protected var mMediaSource: MediaSource? = null

    /** 视频候选地址：首个起播，播放失败/频繁缓冲时依次前进；空表示没有视频流 */
    private var videoCandidates: List<String> = emptyList()

    /** 音频候选地址：与视频候选同步前进；空表示无独立音频流 */
    private var audioCandidates: List<String> = emptyList()

    /** 当前使用的候选下标（视频与音频共用；较短的一方取最后一个有效地址） */
    private var candidateIndex = 0

    /** 已经发生的自动切换次数，上限 [MAX_CDN_SWITCHES] */
    private var cdnSwitchCount = 0

    private var cdnFailoverEnabled = options.enableCdnFailover

    private val bufferingDetector = FrequentBufferingDetector()

    @OptIn(UnstableApi::class)
    private val dataSourceFactory =
        OkHttpDataSource.Factory(PlayerHttpClient.get(context)).apply {
            options.userAgent?.let { setUserAgent(it) }
            options.referer?.let { setDefaultRequestProperties(mapOf("referer" to it)) }
        }

    private var skipSilenceEnabled = options.enableSkipSilence

    init {
        initPlayer()
    }

    @OptIn(UnstableApi::class)
    override fun initPlayer() {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(
                when (options.enableFfmpegAudioRenderer) {
                    // 用 PREFER 而不是 ON：ON 会对所有轨道强制走扩展渲染器，
                    // 设备上 ffmpeg 扩展初始化失败时整路画面都渲染不出来（黑屏）
                    true -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                    false -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                }
            )
        }
        mPlayer = ExoPlayer
            .Builder(context)
            .setRenderersFactory(renderersFactory)
            .setSeekForwardIncrementMs(1000 * 10)
            .setSeekBackIncrementMs(1000 * 5)
            .build()

        initListener()
    }

    private fun initListener() {
        mPlayer?.addListener(this)
    }

    @OptIn(UnstableApi::class)
    override fun setHeader(headers: Map<String, String>) {

    }

    @OptIn(UnstableApi::class)
    override fun playUrl(videoUrl: String?, audioUrl: String?) {
        // 单地址调用保持旧行为：不做空白过滤、不启用多候选切换，仅有一条候选
        setCandidates(listOfNotNull(videoUrl), listOfNotNull(audioUrl))
    }

    /**
     * 候选地址列表起播：索引 0 起播，播放失败或频繁缓冲时切到下一个候选并重新 prepare，
     * 最多切换 [MAX_CDN_SWITCHES] 次。没有候选时保持与旧实现一致的失败行为（prepare 前无媒体源）。
     */
    @OptIn(UnstableApi::class)
    override fun playUrl(videoUrls: List<String>, audioUrls: List<String>) {
        setCandidates(
            videoUrls.filter { it.isNotBlank() }.distinct(),
            audioUrls.filter { it.isNotBlank() }.distinct()
        )
    }

    private fun setCandidates(videos: List<String>, audios: List<String>) {
        videoCandidates = videos
        audioCandidates = audios
        candidateIndex = 0
        cdnSwitchCount = 0
        bufferingDetector.reset()
        mMediaSource = buildMediaSource()
    }

    private fun buildMediaSource(): MediaSource? {
        val mediaSources = listOfNotNull(
            videoCandidates.candidateAt(candidateIndex)?.let { createMediaSource(it) },
            audioCandidates.candidateAt(candidateIndex)?.let { createMediaSource(it) }
        )
        if (mediaSources.isEmpty()) return null
        return MergingMediaSource(*mediaSources.toTypedArray())
    }

    /** 较短的一方取最后一个候选，避免视频/音频候选数量不一致时丢轨道 */
    private fun List<String>.candidateAt(index: Int): String? =
        getOrNull(index) ?: lastOrNull()

    private fun createMediaSource(url: String): MediaSource =
        ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(url))

    override fun setCdnFailoverEnabled(enabled: Boolean) {
        cdnFailoverEnabled = enabled
    }

    @OptIn(UnstableApi::class)
    override fun prepare() {
        mPlayer?.setMediaSource(mMediaSource!!)
        mPlayer?.prepare()
    }

    override fun start() {
        mPlayer?.play()
    }

    override fun pause() {
        mPlayer?.pause()
    }

    override fun stop() {
        mPlayer?.stop()
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override val isPlaying: Boolean
        get() = mPlayer?.isPlaying == true

    override fun seekTo(time: Long) {
        bufferingDetector.onSeek(SystemClock.elapsedRealtime())
        mPlayer?.seekTo(time)
    }

    override fun release() {
        mPlayer?.release()
    }

    override val currentPosition: Long
        get() = mPlayer?.currentPosition ?: 0
    override val duration: Long
        get() = mPlayer?.duration ?: 0
    override val bufferedPercentage: Int
        get() = mPlayer?.bufferedPercentage ?: 0

    override fun setOptions() {
        mPlayer?.playWhenReady = true
    }

    override var speed: Float
        get() = mPlayer?.playbackParameters?.speed ?: 1f
        set(value) {
            mPlayer?.setPlaybackSpeed(value)
            applySkipSilence()
        }

    /** 运行时开关；实际是否生效还取决于当前播放速度，需达到 [SKIP_SILENCE_MIN_SPEED] 才真正开启 */
    fun setSkipSilenceEnabled(enabled: Boolean) {
        skipSilenceEnabled = enabled
        applySkipSilence()
    }

    private fun applySkipSilence() {
        mPlayer?.setSkipSilenceEnabled(skipSilenceEnabled && speed >= SKIP_SILENCE_MIN_SPEED)
    }

    override val tcpSpeed: Long
        get() = 0L

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_IDLE -> mPlayerEventListener?.onIdle()
            Player.STATE_BUFFERING -> {
                mPlayerEventListener?.onBuffering()
                // 30 秒内缓冲达到 3 次说明当前地址质量差，切到下一个没失败过的候选
                if (isCdnFailoverActive() &&
                    bufferingDetector.onBufferingStart(SystemClock.elapsedRealtime())
                ) {
                    trySwitchToNextCdn("频繁缓冲")
                }
            }

            Player.STATE_READY -> {
                bufferingDetector.onReady()
                mPlayerEventListener?.onReady()
            }

            Player.STATE_ENDED -> mPlayerEventListener?.onEnd()
        }
    }

    /** 是否具备自动切候选的条件：开关打开且确实还有其它候选 */
    private fun isCdnFailoverActive(): Boolean =
        cdnFailoverEnabled &&
            maxOf(videoCandidates.lastIndex, audioCandidates.lastIndex) > candidateIndex

    /**
     * 切到下一个候选地址并重新 prepare，保留播放进度与播放状态。
     *
     * @param reason 触发原因（仅用于日志）
     * @return 是否真的发生了切换；没有下一个候选或达到次数上限时返回 false
     */
    private fun trySwitchToNextCdn(reason: String): Boolean {
        if (!cdnFailoverEnabled) return false
        if (cdnSwitchCount >= MAX_CDN_SWITCHES) {
            logger.warn { "CDN $reason：已达到最大切换次数（$MAX_CDN_SWITCHES），不再切换" }
            return false
        }
        val maxIndex = maxOf(videoCandidates.lastIndex, audioCandidates.lastIndex)
        if (candidateIndex >= maxIndex) return false

        val fromHost = currentVideoHost()
        val position = mPlayer?.currentPosition ?: 0L
        val wasPlaying = mPlayer?.playWhenReady == true
        val source = runCatching {
            candidateIndex++
            buildMediaSource()
        }.getOrNull()
        if (source == null) {
            candidateIndex--
            return false
        }
        cdnSwitchCount++
        mMediaSource = source
        mPlayer?.setMediaSource(source)
        mPlayer?.prepare()
        if (position > 0L) mPlayer?.seekTo(position)
        if (wasPlaying) mPlayer?.play()
        bufferingDetector.reset()
        val toHost = currentVideoHost()
        logger.warn {
            "CDN $reason，自动切换候选地址: $fromHost -> $toHost（第 $cdnSwitchCount/$MAX_CDN_SWITCHES 次）"
        }
        mPlayerEventListener?.onCdnSwitched(fromHost, toHost)
        return true
    }

    private fun currentVideoHost(): String? {
        val url = videoCandidates.candidateAt(candidateIndex)
            ?: audioCandidates.candidateAt(candidateIndex)
            ?: return null
        return runCatching { URI(url).host }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            mPlayerEventListener?.onPlay()
        } else {
            mPlayerEventListener?.onPause()
        }
    }

    override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) {
        mPlayerEventListener?.onSeekBack(seekBackIncrementMs)
    }

    override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) {
        mPlayerEventListener?.onSeekForward(seekForwardIncrementMs)
    }

    override val debugInfo: String
        get() {
            return """
                player: ${androidx.media3.common.MediaLibraryInfo.VERSION_SLASHY}
                time: ${currentPosition.formatHourMinSec()} / ${duration.formatHourMinSec()}
                buffered: $bufferedPercentage%
                resolution: ${mPlayer?.videoSize?.width} x ${mPlayer?.videoSize?.height}
                audio: ${mPlayer?.audioFormat?.bitrate ?: 0} kbps
                video codec: ${mPlayer?.videoFormat?.sampleMimeType ?: "null"}
                audio codec: ${mPlayer?.audioFormat?.sampleMimeType ?: "null"} (${getAudioRendererName()})
            """.trimIndent()
        }

    private fun getAudioRendererName(): String {
        val rendererCount = mPlayer?.rendererCount ?: return "UnknownRenderer"
        for (i in 0 until rendererCount) {
            val renderer = mPlayer!!.getRenderer(i)
            if (renderer.trackType == C.TRACK_TYPE_AUDIO && renderer.state == Renderer.STATE_STARTED) {
                return renderer.name
            }
        }
        return "UnknownRenderer"
    }

    override val videoWidth: Int
        get() = mPlayer?.videoSize?.width ?: 0
    override val videoHeight: Int
        get() = mPlayer?.videoSize?.height ?: 0

    override fun onPlayerError(error: PlaybackException) {
        // 播放失败先尝试下一个候选地址；候选耗尽或次数用尽才把错误抛给上层
        if (trySwitchToNextCdn("播放失败")) return
        mPlayerEventListener?.onError(error)
    }
}
