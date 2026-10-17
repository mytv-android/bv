package dev.aaa1115910.bv.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.render.SimpleRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.PlayData
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskSegment
import dev.aaa1115910.biliapi.entity.video.HeartbeatVideoType
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleAiStatus
import dev.aaa1115910.biliapi.entity.video.SubtitleAiType
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import dev.aaa1115910.biliapi.entity.video.VideoShot
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.video.ClipInfo
import dev.aaa1115910.biliapi.repositories.VideoPlayRepository
import dev.aaa1115910.bilisubtitle.SubtitleParser
import dev.aaa1115910.bilisubtitle.entity.SubtitleItem
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.RequestState
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoAspectRatio
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.entity.toAudioLoudness
import dev.aaa1115910.bv.repository.PlayUrlResolver
import dev.aaa1115910.bv.repository.VideoInfoRepository
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fException
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.swapList
import dev.aaa1115910.bv.util.swapListWithMainContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import java.net.URI

@KoinViewModel
class VideoPlayerV3ViewModel(
    private val videoInfoRepository: VideoInfoRepository,
    private val videoPlayRepository: VideoPlayRepository,
) : ViewModel() {
    private val logger = KotlinLogging.logger { }
    private val playUrlResolver = PlayUrlResolver(videoPlayRepository)

    var videoPlayer: AbstractVideoPlayer? by mutableStateOf(null)
    var danmakuPlayer: DanmakuPlayer? by mutableStateOf(null)
    var show by mutableStateOf(false)

    var loadState by mutableStateOf(RequestState.Ready)
    var errorMessage by mutableStateOf("")

    private var playData: PlayData? by mutableStateOf(null)
    var danmakuData = mutableStateListOf<DanmakuItemData>()
    val danmakuMasks = mutableStateListOf<DanmakuMaskSegment>()
    var videoShot: VideoShot? by mutableStateOf(null)

    /** 当前剧集的片头/片尾配置；换集时先清空，拿到新播放数据后再更新 */
    var clipInfoList: List<ClipInfo>? by mutableStateOf(null)

    var availableQuality = mutableStateListOf<Resolution>()
    var availableVideoCodec = mutableStateListOf<VideoCodec>()
    var availableSubtitle = mutableStateListOf<Subtitle>()
    var availableAudio = mutableStateListOf<Audio>()
    val availableVideoList get() = videoInfoRepository.videoList

    var currentVideoHeight by mutableIntStateOf(0)
    var currentVideoWidth by mutableIntStateOf(0)

    var currentQuality by mutableStateOf(Prefs.defaultQuality)
    var currentVideoCodec by mutableStateOf(Prefs.defaultVideoCodec)
    var currentPlaySpeed by mutableFloatStateOf(Prefs.defaultPlaySpeed)
    var currentVideoAspectRatio by mutableStateOf(VideoAspectRatio.Default)
    var currentAudio by mutableStateOf(Prefs.defaultAudio)
    var currentDanmakuScale by mutableFloatStateOf(Prefs.defaultDanmakuScale)
    var currentDanmakuOpacity by mutableFloatStateOf(Prefs.defaultDanmakuOpacity)
    var currentDanmakuEnabled by mutableStateOf(Prefs.defaultDanmakuEnabled)
    val currentDanmakuTypes = mutableStateListOf<DanmakuType>().apply {
        addAll(Prefs.defaultDanmakuTypes)
    }
    var currentDanmakuArea by mutableFloatStateOf(Prefs.defaultDanmakuArea)
    var currentDanmakuMask by mutableStateOf(Prefs.defaultDanmakuMask)
    var currentSubtitleId by mutableLongStateOf(-1L)
    var currentSubtitleData = mutableStateListOf<SubtitleItem>()
    var currentSubtitleFontSize by mutableStateOf(Prefs.defaultSubtitleFontSize)
    var currentSubtitleBackgroundOpacity by mutableFloatStateOf(Prefs.defaultSubtitleBackgroundOpacity)
    var currentSubtitleBottomPadding by mutableStateOf(Prefs.defaultSubtitleBottomPadding)

    var currentPlayMode by mutableStateOf(Prefs.defaultPlayMode)

    var title by mutableStateOf("")
    var partTitle by mutableStateOf("")
    var lastPlayed by mutableIntStateOf(0)
    var fromSeason by mutableStateOf(false)
    var subType by mutableIntStateOf(0)
    var epid by mutableIntStateOf(0)
    var seasonId by mutableIntStateOf(0)
    var isVerticalVideo by mutableStateOf(false)
    var proxyArea by mutableStateOf(ProxyArea.MainLand)

    var needPay by mutableStateOf(false)

    /** 当前播放的是试看片段（needPay 且仍有可播放流） */
    var isPreviewPlayback by mutableStateOf(false)

    /** 试看提示在起播后短暂显示 */
    var showPreviewTip by mutableStateOf(false)

    private var previewTipJob: Job? = null

    var logs by mutableStateOf("")
    var lastChangedLog by mutableLongStateOf(System.currentTimeMillis())
    var showBuffering by mutableStateOf(false)

    var playerIconIdle by mutableStateOf("")
    var playerIconMoving by mutableStateOf("")

    private var currentAid = 0L
    var currentCid by mutableLongStateOf(0L)
    private var currentEpid = 0

    private suspend fun releaseDanmakuPlayer() = withContext(Dispatchers.Main) {
        danmakuPlayer?.release()
    }

    suspend fun initDanmakuPlayer() = withContext(Dispatchers.Main) {
        danmakuPlayer = DanmakuPlayer(SimpleRenderer())
    }

    fun loadPlayUrl(
        avid: Long,
        cid: Long,
        epid: Int? = null,
        seasonId: Int? = null,
        continuePlayNext: Boolean = false
    ) {
        currentAid = avid
        currentCid = cid
        currentEpid = epid ?: 0
        // 换集后先清空上一集的片头片尾，避免新数据返回前用旧配置误跳
        clipInfoList = null
        epid?.let { this.epid = it }
        seasonId?.let { this.seasonId = it }
        viewModelScope.launch(Dispatchers.Default) {
            addLogs("加载视频中")
            releaseDanmakuPlayer()
            initDanmakuPlayer()
            addLogs("初始化弹幕引擎")
            if (epid != null || seasonId != null) {
                addLogs("av$avid，cid:$cid, epid:$epid, seasonId:$seasonId")
            } else {
                addLogs("av$avid，cid:$cid")
            }

            val lastPlayEnabledSubtitle = currentSubtitleId != -1L
            if (lastPlayEnabledSubtitle) {
                logger.info { "Subtitle is enabled, next video will enable subtitle automatic" }
            }

            updateSubtitle()
            loadPlayUrl(avid, cid, epid ?: 0, preferApi = Prefs.apiType, proxyArea = proxyArea)
            addLogs("加载弹幕中")
            loadDanmaku(cid)
            updateDanmakuMask()

            updateVideoShot()

            //如果是继续播放下一集，且之前开启了字幕，就会自动加载第一条字幕，主要用于观看番剧时自动加载字幕
            if (continuePlayNext) {
                if (lastPlayEnabledSubtitle) enableFirstSubtitle()
            }
        }
    }

    private suspend fun loadPlayUrl(
        avid: Long,
        cid: Long,
        epid: Int = 0,
        preferApi: ApiType = Prefs.apiType,
        proxyArea: ProxyArea = ProxyArea.MainLand
    ) {
        logger.fInfo { "Load play url: [av=$avid, cid=$cid, preferApi=$preferApi, proxyArea=$proxyArea]" }
        withContext(Dispatchers.Main) { loadState = RequestState.Ready }
        logger.fInfo { "Set request state: ready" }
        logger.fInfo { "fromSeason: $fromSeason" }
        runCatching {
            val playData = playUrlResolver.fetchPlayData(
                aid = avid,
                cid = cid,
                epid = epid,
                isPgc = fromSeason,
                preferApiType = Prefs.apiType,
                preferCodec = Prefs.defaultVideoCodec,
                proxyArea = proxyArea
            )

            // 付费/充电视频未购买时接口会下发试看片段：有可播放流就继续播并给出提示
            val previewPlayback = playData.needPay && playData.dashVideos.isNotEmpty()
            withContext(Dispatchers.Main) {
                needPay = playData.needPay
                isPreviewPlayback = previewPlayback
            }
            if (playData.needPay && !previewPlayback) return@runCatching

            withContext(Dispatchers.Main) {
                this@VideoPlayerV3ViewModel.playData = playData
                this@VideoPlayerV3ViewModel.clipInfoList = playData.clipInfoList
            }
            logger.fInfo { "Load play data response success" }
            //logger.info { "Play data: $playData" }

            //读取清晰度
            val resolutionList = playUrlResolver.getAvailableQualities(playData)
            availableQuality.swapListWithMainContext(resolutionList)

            //读取音频
            val audioList = playUrlResolver.getAvailableAudios(playData)
            availableAudio.swapListWithMainContext(audioList)

            //先确认最终所选清晰度
            val selectedQuality = playUrlResolver.selectQuality(
                available = resolutionList,
                preferred = Prefs.defaultQuality,
                current = currentQuality
            )
            withContext(Dispatchers.Main) {
                this@VideoPlayerV3ViewModel.currentQuality = selectedQuality
            }

            //确认最终所选音质
            val selectedAudio = playUrlResolver.selectAudio(
                available = audioList,
                preferred = Prefs.defaultAudio,
                current = currentAudio
            )
            withContext(Dispatchers.Main) {
                this@VideoPlayerV3ViewModel.currentAudio = selectedAudio
            }

            //再确认最终所选视频编码
            updateAvailableCodec()

            playQuality(qn = currentQuality.code, codec = currentVideoCodec)

            if (previewPlayback) startShowPreviewTipCountdown()

        }.onFailure {
            addLogs("加载视频地址失败：${it.localizedMessage}")
            errorMessage = it.localizedMessage ?: "Unknown error"
            loadState = RequestState.Failed
            logger.fException(it) { "Load video failed" }
        }.onSuccess {
            addLogs("加载视频地址成功")
            loadState = RequestState.Success
            logger.fInfo { "Load play url success" }
        }
    }

    private fun startShowPreviewTipCountdown() {
        previewTipJob?.cancel()
        previewTipJob = viewModelScope.launch {
            showPreviewTip = true
            delay(5000)
            showPreviewTip = false
        }
    }

    private suspend fun updateAvailableCodec() {
        val codecSelection = playUrlResolver.selectCodec(
            playData = playData!!,
            quality = currentQuality,
            preferred = Prefs.defaultVideoCodec,
            apiType = Prefs.apiType
        )
        // App 接口没有编码列表时只纠正当前实际播放的编码，不更新可选编码
        codecSelection.available?.let { availableVideoCodec.swapListWithMainContext(it) }
        withContext(Dispatchers.Main) {
            this@VideoPlayerV3ViewModel.currentVideoCodec = codecSelection.selected
        }
    }

    suspend fun playQuality(
        qn: Resolution = currentQuality,
        codec: VideoCodec = currentVideoCodec,
        audio: Audio = currentAudio
    ) {
        if (qn != currentQuality) {
            // 更新清晰度后需要先设置清晰度再更新编码列表
            withContext(Dispatchers.Main) { currentQuality = qn }
            updateAvailableCodec()
            playQuality(qn.code, currentVideoCodec, audio)
        } else {
            playQuality(qn.code, codec, audio)
        }
    }

    private suspend fun playQuality(
        qn: Int = currentQuality.code,
        codec: VideoCodec = currentVideoCodec,
        audio: Audio = currentAudio
    ) {
        logger.fInfo { "Select resolution: $qn, codec: $codec, audio: $audio" }
        addLogs(
            "播放清晰度：${availableQuality.firstOrNull { it.code == qn }}, " +
                    "视频编码：${codec.getDisplayName(BVApp.context)}"
        )

        val streams = playUrlResolver.selectStreams(
            playData = playData!!,
            qn = qn,
            codec = codec,
            audio = audio,
            apiType = Prefs.apiType,
            proxyArea = proxyArea
        )
        val videoUrl = streams.videoUrl
        val audioUrl = streams.audioUrl

        addLogs("video host: ${with(URI(videoUrl)) { "$scheme://$authority" }}")
        addLogs("audio host: ${audioUrl?.let { with(URI(it)) { "$scheme://$authority" } } ?: "无音频流"}")

        addLogs("音频编码：${(Audio.fromCode(streams.audio?.codecId ?: 0))?.getDisplayName(BVApp.context) ?: "未知"}")

        withContext(Dispatchers.Main) {
            currentVideoHeight = streams.video?.height ?: 0
            currentVideoWidth = streams.video?.width ?: 0
            logger.info { "Video url: $videoUrl" }
            logger.info { "Audio url: $audioUrl" }
            // 换集/换源后重新推一次：null（如 gRPC PGC）表示保持原音量
            videoPlayer!!.setAudioLoudness(playData?.volume?.toAudioLoudness())
            videoPlayer!!.playUrl(videoUrl, audioUrl)
            videoPlayer!!.prepare()
            showBuffering = true
        }
    }

    suspend fun loadDanmaku(cid: Long) {
        runCatching {
            val danmakuXmlData = BiliHttpApi.getDanmakuXml(cid = cid, sessData = Prefs.sessData)

            val danmakuItemDataList = danmakuXmlData.data.map {
                DanmakuItemData(
                    danmakuId = it.dmid,
                    position = (it.time * 1000).toLong(),
                    content = it.text,
                    mode = when (it.type) {
                        4 -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                        5 -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                        else -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    },
                    textSize = it.size,
                    textColor = Color(it.color).toArgb()
                )
            }
            danmakuData.swapListWithMainContext(danmakuItemDataList)
            danmakuPlayer?.updateData(danmakuData)
        }.onFailure {
            addLogs("加载弹幕失败：${it.localizedMessage}")
            logger.fWarn { "Load danmaku filed: ${it.stackTraceToString()}" }
        }.onSuccess {
            addLogs("已加载 ${danmakuData.size} 条弹幕")
            logger.fInfo { "Load danmaku success, size=${danmakuData.size}" }
        }
    }

    private suspend fun updateSubtitle() {
        currentSubtitleId = -1
        currentSubtitleData.clear()

        runCatching {
            val subtitleData = videoPlayRepository.getSubtitle(
                aid = currentAid,
                cid = currentCid,
                preferApiType = Prefs.apiType
            )
            withContext(Dispatchers.Main) {
                availableSubtitle.clear()
                availableSubtitle.add(
                    Subtitle(
                        id = -1,
                        lang = "",
                        langDoc = "关闭",
                        url = "",
                        type = SubtitleType.CC,
                        aiType = SubtitleAiType.Normal,
                        aiStatus = SubtitleAiStatus.None
                    )
                )
                availableSubtitle.addAll(subtitleData)
                availableSubtitle.sortBy { it.id }
            }
            addLogs("获取到 ${subtitleData.size} 条字幕: ${subtitleData.map { it.langDoc }}")
            logger.fInfo { "Update subtitle size: ${subtitleData.size}" }
        }.onFailure {
            addLogs("获取字幕失败：${it.localizedMessage}")
            logger.fWarn { "Update subtitle failed: ${it.stackTraceToString()}" }
        }
    }

    private fun enableFirstSubtitle() {
        runCatching {
            logger.info { "Load first subtitle" }
            logger.info { "availableSubtitle: ${availableSubtitle.toList()}" }
            loadSubtitle(
                availableSubtitle
                    .firstOrNull { it.id != -1L }?.id
                    ?: throw IllegalStateException("No available subtitle")
            )
        }.onFailure {
            logger.error { "Load first subtitle failed: ${it.stackTraceToString()}" }
        }
    }

    private suspend fun addLogs(text: String) {
        logger.fInfo { text }
        val lines = logs.lines().toMutableList()
        lines.add(text)
        while (lines.size > 8) {
            lines.removeAt(0)
        }
        var newTip = ""
        lines.forEach {
            newTip += if (newTip == "") it else "\n$it"
        }
        withContext(Dispatchers.Main) {
            logs = newTip
            lastChangedLog = System.currentTimeMillis()
        }
    }

    suspend fun uploadHistory(time: Int) {
        runCatching {
            if (!fromSeason) {
                logger.info { "Send heartbeat: [avid=$currentAid, cid=$currentCid, time=$time]" }
                videoPlayRepository.sendHeartbeat(
                    aid = currentAid,
                    cid = currentCid,
                    time = time,
                    preferApiType = Prefs.apiType
                )
            } else {
                logger.info { "Send heartbeat: [avid=$currentAid, cid=$currentCid, epid=$epid, sid=$seasonId, time=$time]" }
                videoPlayRepository.sendHeartbeat(
                    aid = currentAid,
                    cid = currentCid,
                    time = time,
                    type = HeartbeatVideoType.Season,
                    subType = subType,
                    epid = epid,
                    seasonId = seasonId,
                    preferApiType = Prefs.apiType
                )
            }
        }.onSuccess {
            logger.info { "Send heartbeat success" }
        }.onFailure {
            logger.warn { "Send heartbeat failed: ${it.stackTraceToString()}" }
        }
    }

    fun loadSubtitle(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (id == -1L) {
                withContext(Dispatchers.Main) {
                    currentSubtitleData.clear()
                    currentSubtitleId = -1
                }
                return@launch
            }
            var subtitleName = ""
            runCatching {
                val subtitle = availableSubtitle.find { it.id == id } ?: return@runCatching
                subtitleName = subtitle.langDoc
                logger.info { "Subtitle url: ${subtitle.url}" }
                val client = HttpClient(OkHttp)
                val responseText = client.get(subtitle.url).bodyAsText()
                val subtitleData = SubtitleParser.fromBccString(responseText)
                withContext(Dispatchers.Main) {
                    currentSubtitleId = id
                    currentSubtitleData.swapList(subtitleData)
                }
            }.onFailure {
                logger.fInfo { "Load subtitle failed: ${it.stackTraceToString()}" }
                addLogs("加载字幕 $subtitleName 失败: ${it.localizedMessage}")
            }.onSuccess {
                logger.fInfo { "Load subtitle $subtitleName success" }
                addLogs("加载字幕 $subtitleName 成功，数量: ${currentSubtitleData.size}")
            }
        }
    }

    private suspend fun updateDanmakuMask() {
        runCatching {
            val masks = videoPlayRepository.getDanmakuMask(
                aid = currentAid,
                cid = currentCid,
                preferApiType = Prefs.apiType
            )
            danmakuMasks.swapListWithMainContext(masks)
            logger.fInfo { "Load danmaku mask size: ${danmakuMasks.size}" }
        }.onFailure {
            logger.fWarn { "Load danmaku mask failed: ${it.stackTraceToString()}" }
        }
    }

    private suspend fun updateVideoShot() {
        withContext(Dispatchers.Main) { videoShot = null }
        runCatching {
            val videoShot = videoPlayRepository.getVideoShot(
                aid = currentAid,
                cid = currentCid,
                preferApiType = Prefs.apiType
            )
            withContext(Dispatchers.Main) { this@VideoPlayerV3ViewModel.videoShot = videoShot }
            logger.fInfo { "Load video shot success" }
        }.onFailure {
            logger.fWarn { "Load video shot failed: ${it.stackTraceToString()}" }
        }
    }

    fun playNextVideo() {
        logger.fInfo { "Video finished" }
        when (currentPlayMode) {
            PlayMode.Single -> {
                logger.info { "Play mode: $currentPlayMode, do nothing" }
            }

            PlayMode.Sequential -> {
                logger.info { "Play mode: $currentPlayMode, play next video in list" }
                playNextVideoInList()
            }

            PlayMode.SingleLoop -> {
                logger.info { "Play mode: $currentPlayMode, replay current video" }
                danmakuPlayer?.seekTo(0L)
                danmakuPlayer?.pause()
                videoPlayer?.seekTo(0L)
            }

            PlayMode.ListLoop -> {
                logger.info { "Play mode: $currentPlayMode, play next video in list or loop to first" }
                playNextVideoInList(loop = true)
            }
        }
    }

    private fun playNextVideoInList(loop: Boolean = false) {
        val currentIndex = availableVideoList
            .indexOfFirst {
                when (it) {
                    is VideoListItemData -> it.cid == currentCid
                    else -> false
                }
            }
        if (currentIndex + 1 < availableVideoList.size) {
            val nextVideos = availableVideoList.subList(
                currentIndex + 1,
                availableVideoList.size
            )
            val nextVideo =
                nextVideos.firstOrNull { it is VideoListItemData }!! as VideoListItemData
            logger.info { "Play next video: $nextVideo" }
            partTitle = nextVideo.title
            loadPlayUrl(
                avid = nextVideo.aid,
                cid = nextVideo.cid,
                epid = nextVideo.epid,
                seasonId = nextVideo.seasonId,
                continuePlayNext = true
            )
        } else if (loop) {
            //loop to first
            val firstVideo =
                availableVideoList.firstOrNull { it is VideoListItemData }!! as VideoListItemData
            logger.info { "Loop to first video: $firstVideo" }
            partTitle = firstVideo.title
            loadPlayUrl(
                avid = firstVideo.aid,
                cid = firstVideo.cid,
                epid = firstVideo.epid,
                seasonId = firstVideo.seasonId,
                continuePlayNext = true
            )
        }
    }
}