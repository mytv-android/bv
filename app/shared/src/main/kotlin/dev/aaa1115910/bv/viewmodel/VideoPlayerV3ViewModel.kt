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
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.PlayData
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskSegment
import dev.aaa1115910.biliapi.entity.video.HeartbeatVideoType
import dev.aaa1115910.biliapi.entity.video.InteractiveNode
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleAiStatus
import dev.aaa1115910.biliapi.entity.video.SubtitleAiType
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import dev.aaa1115910.biliapi.entity.video.VideoShot
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.RiskControlException
import dev.aaa1115910.biliapi.http.entity.video.ClipInfo
import dev.aaa1115910.biliapi.repositories.AuthRepository
import dev.aaa1115910.biliapi.repositories.VideoPlayRepository
import dev.aaa1115910.bilisubtitle.SubtitleParser
import dev.aaa1115910.bilisubtitle.entity.SubtitleItem
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.danmaku.DanmakuView
import dev.aaa1115910.bv.player.danmaku.model.Danmaku
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.RequestState
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoAspectRatio
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.entity.VideoListInteractiveNode
import dev.aaa1115910.bv.player.entity.toAudioLoudness
import dev.aaa1115910.bv.repository.PlayUrlResolver
import dev.aaa1115910.bv.repository.VideoInfoRepository
import dev.aaa1115910.bv.util.GeetestSession
import dev.aaa1115910.bv.util.GeetestSubmitResult
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.VVoucherAlreadyAttemptedException
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import java.net.URI

/**
 * [runCatching] 会把协程取消也当成失败，进了 `onFailure` 就会留下"xxx 失败"的假日志/假错误状态。
 * 这里让 [CancellationException] 照常向上抛，只把真正的失败包成 [Result]。
 */
private inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

@KoinViewModel
class VideoPlayerV3ViewModel(
    private val videoInfoRepository: VideoInfoRepository,
    private val videoPlayRepository: VideoPlayRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val logger = KotlinLogging.logger { }
    private val playUrlResolver = PlayUrlResolver(videoPlayRepository)

    var videoPlayer: AbstractVideoPlayer? by mutableStateOf(null)

    /**
     * 自研弹幕引擎的渲染 View：由播放页组合创建并赋值（离开页面时 release），
     * ViewModel 只往里喂数据 / 清空，不负责其生命周期。
     */
    var danmakuView: DanmakuView? by mutableStateOf(null)
    var show by mutableStateOf(false)

    var loadState by mutableStateOf(RequestState.Ready)
    var errorMessage by mutableStateOf("")

    private var playData: PlayData? by mutableStateOf(null)
    var danmakuData = mutableStateListOf<Danmaku>()
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
    var currentDanmakuLaneDensity by mutableStateOf(Prefs.defaultDanmakuLaneDensity)
    var currentDanmakuRollingDurationFactor by mutableFloatStateOf(Prefs.defaultDanmakuRollingDurationFactor)
    var currentDanmakuFilterLevel by mutableIntStateOf(Prefs.defaultDanmakuFilterLevel)
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

    /** 触发 Geetest 验证时的播放请求快照，验证通过后按快照重试原请求 */
    private data class GeetestRetryRequest(
        val avid: Long,
        val cid: Long,
        val epid: Int,
        val proxyArea: ProxyArea,
    )

    // 风控 Geetest 验证状态
    var showGeetestDialog by mutableStateOf(false)
    var geetestGt by mutableStateOf("")
    var geetestChallenge by mutableStateOf("")
    private var pendingRetryRequest: GeetestRetryRequest? = null
    // v_voucher 是一次性凭证，会话内部保证同一个 voucher 只注册一次，避免重复弹窗死循环
    private val geetestSession = GeetestSession(authRepository)

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

    // ---------------------------------------------------------------- 互动视频（stein gate）

    /** 当前播放的是互动视频（节点图上下文已加载且 aid 匹配，避免残留上下文污染普通视频） */
    val isInteractivePlayback
        get() = videoInfoRepository.interactivePlaybackContext?.let { it.aid == currentAid } == true

    /** 当前节点的选项：待选分支，或已走过的节点路径（叶子节点时，可回退） */
    val interactiveOptions get() = availableVideoList.filterIsInstance<VideoListInteractiveNode>()

    var showInteractiveOptionDialog by mutableStateOf(false)

    /** 当前选项是否来自 question choices（false 表示是节点路径，不能作为「待选项」弹出） */
    var interactiveOptionsFromQuestions by mutableStateOf(false)
    private var pendingInteractiveOptionDialogRequest by mutableStateOf(false)

    var currentInteractiveNodeId by mutableLongStateOf(0L)
    var currentInteractiveEdgeId by mutableLongStateOf(0L)

    /** 互动分支切换 / 回退时，新媒体准备完成后要跳到的位置（毫秒）；普通播放为 null */
    private var pendingInitialSeekPositionMs: Long? = null

    /** 换集前清空上一集的弹幕数据（引擎保留，View 生命周期由播放页管理）。 */
    private suspend fun clearDanmakuView() = withContext(Dispatchers.Main) {
        danmakuView?.clearDanmakus()
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { danmakuView?.release() }
        danmakuView = null
    }

    fun loadPlayUrl(
        avid: Long,
        cid: Long,
        epid: Int? = null,
        seasonId: Int? = null,
        continuePlayNext: Boolean = false,
        initialSeekPositionMs: Long? = null,
    ) {
        showInteractiveOptionDialog = false
        pendingInitialSeekPositionMs = initialSeekPositionMs
        if (continuePlayNext) {
            lastPlayed = 0
        }
        currentAid = avid
        currentCid = cid
        currentEpid = epid ?: 0
        syncCurrentInteractivePointersFromList()
        if (!isInteractivePlayback && availableVideoList.none { it is VideoListInteractiveNode }) {
            currentInteractiveNodeId = 0L
            currentInteractiveEdgeId = 0L
        }
        // 换集后先清空上一集的片头片尾，避免新数据返回前用旧配置误跳
        clipInfoList = null
        epid?.let { this.epid = it }
        seasonId?.let { this.seasonId = it }
        viewModelScope.launch(Dispatchers.Default) {
            addLogs("加载视频中")
            clearDanmakuView()
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
            if (isInteractivePlayback && (!interactiveOptionsFromQuestions || interactiveOptions.isEmpty())) {
                refreshInteractiveBranches(currentInteractiveEdgeId.takeIf { it > 0L })
            }
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
            val voucher = (it as? RiskControlException)?.vVoucher
            if (voucher != null) {
                logger.fWarn { "Risk control v_voucher detected" }
                addLogs("触发风控，正在申请验证…")
                handleVVoucher(
                    vVoucher = voucher,
                    retryRequest = GeetestRetryRequest(
                        avid = avid,
                        cid = cid,
                        epid = epid,
                        proxyArea = proxyArea,
                    )
                )
                return@onFailure
            }
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

    // ---------------------------------------------------------------- 互动视频状态机

    /**
     * 刷新当前节点的互动分支（edgeinfo_v2）：普通节点取 question choices 作为待选项，
     * 叶子节点没有待选项，取 story_list（已走过的节点路径）用于回退到上一节点。
     * 拉到待选项后替换选集列表，同时处理挂起的选项弹窗请求。
     */
    fun refreshInteractiveBranches(edgeId: Long? = null) {
        val interactiveContext = videoInfoRepository.interactivePlaybackContext ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                BiliHttpApi.getInteractiveEdgeInfo(
                    bvid = interactiveContext.bvid,
                    graphVersion = interactiveContext.graphVersion,
                    edgeId = edgeId,
                    sessData = Prefs.sessData.ifBlank { null },
                ).getResponseData().let { response ->
                    val questionNodes = response.edges?.questions
                        ?.flatMap { question -> question.choices }
                        ?.mapIndexed { index, choice ->
                            InteractiveNode.fromChoice(choice, fallbackTitle = "")
                        }
                        .orEmpty()
                    Triple(
                        response.edgeId,
                        if (questionNodes.isNotEmpty()) {
                            questionNodes
                        } else {
                            response.storyList.map(InteractiveNode::fromStoryNode)
                        },
                        questionNodes.isNotEmpty(),
                    )
                }
            }.onSuccess { (responseEdgeId, nodes, fromQuestionChoices) ->
                if (nodes.isEmpty()) {
                    logger.fWarn { "Refresh interactive branches returned empty nodes, edgeId=$edgeId" }
                    withContext(Dispatchers.Main) {
                        pendingInteractiveOptionDialogRequest = false
                    }
                    return@onSuccess
                }

                val interactiveVideoList = nodes.mapIndexed { index, node ->
                    VideoListInteractiveNode(
                        aid = currentAid,
                        cid = node.cid,
                        title = title,
                        partTitle = node.title,
                        index = index,
                        nodeId = node.nodeId,
                        edgeId = node.edgeId,
                        startPos = node.startPos,
                        isCurrent = node.isCurrent,
                    )
                }

                withContext(Dispatchers.Main) {
                    interactiveOptionsFromQuestions = fromQuestionChoices
                    videoInfoRepository.videoList.clear()
                    videoInfoRepository.videoList.addAll(interactiveVideoList)
                    currentInteractiveNodeId = nodes.firstOrNull { it.isCurrent }?.nodeId
                        ?: currentInteractiveNodeId
                    currentInteractiveEdgeId = responseEdgeId
                        ?: nodes.firstOrNull { it.isCurrent }?.edgeId
                        ?: edgeId
                        ?: currentInteractiveEdgeId

                    if (pendingInteractiveOptionDialogRequest && fromQuestionChoices) {
                        showInteractiveOptionDialog = true
                    } else if (pendingInteractiveOptionDialogRequest) {
                        showInteractiveOptionDialog = false
                    }
                    pendingInteractiveOptionDialogRequest = false
                }
            }.onFailure {
                pendingInteractiveOptionDialogRequest = false
                logger.fWarn { "Refresh interactive branches failed: ${it.stackTraceToString()}" }
            }
        }
    }

    /**
     * 播完当前节点后请求弹出选项弹窗：待选项已就绪时立即弹出，
     * 否则先刷新分支，拉到待选项后自动弹出；没有待选项（叶子节点）则不弹。
     */
    fun requestInteractiveOptionDialog(): Boolean {
        if (!isInteractivePlayback) return false

        pendingInteractiveOptionDialogRequest = true
        syncCurrentInteractivePointersFromList()
        val hasQuestionOptions = interactiveOptionsFromQuestions && interactiveOptions.isNotEmpty()
        if (hasQuestionOptions) {
            showInteractiveOptionDialog = true
            pendingInteractiveOptionDialogRequest = false
            return true
        }

        refreshInteractiveBranches(currentInteractiveEdgeId.takeIf { it > 0L })
        return false
    }

    fun dismissInteractiveOptionDialog() {
        showInteractiveOptionDialog = false
        pendingInteractiveOptionDialogRequest = false
    }

    fun selectInteractiveNode(nodeId: Long) {
        currentInteractiveNodeId = nodeId
    }

    /** 选择一个分支：切到对应 cid（播放地址 / 弹幕随之刷新），记录节点路径 */
    fun playInteractiveOption(option: VideoListInteractiveNode) {
        showInteractiveOptionDialog = false
        pendingInteractiveOptionDialogRequest = false
        interactiveOptionsFromQuestions = false
        title = option.title
        partTitle = option.partTitle
        currentInteractiveEdgeId = option.edgeId ?: currentInteractiveEdgeId
        selectInteractiveNode(option.nodeId)
        loadPlayUrl(
            avid = option.aid,
            cid = option.cid,
            epid = option.epid,
            seasonId = option.seasonId,
            continuePlayNext = true,
            initialSeekPositionMs = option.startPos?.times(1000L),
        )
    }

    /** 从选集列表同步当前节点的 id / edgeId（选集项可能被 UI 直接播放） */
    private fun syncCurrentInteractivePointersFromList() {
        val currentInteractiveOption = interactiveOptions.firstOrNull {
            it.isCurrent || it.cid == currentCid || it.nodeId == currentInteractiveNodeId
        } ?: return
        currentInteractiveNodeId = currentInteractiveOption.nodeId
        currentInteractiveEdgeId = currentInteractiveOption.edgeId ?: currentInteractiveEdgeId
    }

    private fun startShowPreviewTipCountdown() {
        previewTipJob?.cancel()
        previewTipJob = viewModelScope.launch {
            showPreviewTip = true
            delay(5000)
            showPreviewTip = false
        }
    }

    private suspend fun handleVVoucher(
        vVoucher: String,
        retryRequest: GeetestRetryRequest,
    ) {
        runCatchingCancellable {
            val challenge = geetestSession.start(vVoucher)
            pendingRetryRequest = retryRequest
            geetestGt = challenge.gt
            geetestChallenge = challenge.challenge
            showGeetestDialog = true
            addLogs("请完成人机验证")
        }.onFailure {
            if (it is VVoucherAlreadyAttemptedException) {
                logger.fWarn { "Skip duplicated Geetest verification: ${it.message}" }
                addLogs("风控验证已申请过，跳过重复弹窗")
                errorMessage = "风控验证已申请，请稍后重试"
                loadState = RequestState.Failed
                return@onFailure
            }
            addLogs("风控验证申请失败：${it.localizedMessage}")
            errorMessage = "风控验证申请失败：${it.localizedMessage}"
            loadState = RequestState.Failed
            logger.fException(it) { "gaiaVgateRegister failed" }
        }
    }

    fun onGeetestResult(challenge: String, validate: String, seccode: String) {
        val retryRequest = pendingRetryRequest ?: return
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = geetestSession.submit(challenge, validate, seccode)) {
                is GeetestSubmitResult.StaleChallenge -> {
                    // 失败重试后极验会回传另一个 challenge，其 token 已被服务端丢弃，提交必然失败
                    logger.fWarn {
                        "Ignore stale Geetest result: expected=${result.expected.take(8)} actual=${result.actual.take(8)}"
                    }
                    addLogs("人机验证信息已失效，请重新验证")
                    errorMessage = "人机验证信息已失效，请重新验证"
                    loadState = RequestState.Failed
                    clearGeetestState()
                }

                is GeetestSubmitResult.Failure -> {
                    addLogs("风控验证失败：${result.message}")
                    logger.fException(result.cause) { "gaiaVgateValidate failed" }
                    errorMessage = "风控验证失败：${result.message}"
                    loadState = RequestState.Failed
                    clearGeetestState()
                }

                GeetestSubmitResult.Success -> {
                    clearGeetestState()
                    addLogs("风控验证通过")
                    logger.fInfo { "Gaia vgate validate success, retrying play url" }
                    if (!isCurrentGeetestPlaybackSession(retryRequest)) {
                        logger.fWarn { "Skip Geetest retry: playback session changed" }
                        return@launch
                    }
                    // 验证通过后按快照重试原请求：成功后沿用原有 playQuality 流程自动起播
                    loadPlayUrl(
                        avid = retryRequest.avid,
                        cid = retryRequest.cid,
                        epid = retryRequest.epid,
                        proxyArea = retryRequest.proxyArea,
                    )
                }
            }
        }
    }

    fun onGeetestCancelled() {
        clearGeetestState()
        viewModelScope.launch(Dispatchers.IO) { addLogs("用户取消了风控验证") }
        errorMessage = "验证已取消"
        loadState = RequestState.Failed
    }

    private fun clearGeetestState() {
        showGeetestDialog = false
        pendingRetryRequest = null
        geetestSession.clear()
    }

    /** 校验触发风控的播放会话是否仍是当前会话，避免验证结果作用于已切换的视频 */
    private fun isCurrentGeetestPlaybackSession(retryRequest: GeetestRetryRequest): Boolean =
        currentAid == retryRequest.avid && currentCid == retryRequest.cid

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
            // 互动分支切换 / 回退：新媒体从节点起始位置播（ExoPlayer 在准备完成后生效）
            pendingInitialSeekPositionMs?.let { target ->
                pendingInitialSeekPositionMs = null
                if (target > 0L) {
                    logger.info { "Set initial seek position: ${target}ms" }
                    videoPlayer!!.seekTo(target)
                    danmakuView?.notifySeek(target)
                }
            }
        }
    }

    suspend fun loadDanmaku(cid: Long) {
        runCatching {
            val danmakuXmlData = BiliHttpApi.getDanmakuXml(cid = cid, sessData = Prefs.sessData)

            // mode 映射沿用旧引擎的行为语义：4→顶部、5→底部、其余滚动
            val danmakuList = danmakuXmlData.data.map {
                Danmaku(
                    dmid = it.dmid,
                    positionMs = (it.time * 1000).toInt(),
                    text = it.text,
                    type = when (it.type) {
                        4 -> Danmaku.MODE_TOP
                        5 -> Danmaku.MODE_BOTTOM
                        else -> Danmaku.MODE_SCROLL
                    },
                    textSize = it.size,
                    color = Color(it.color).toArgb(),
                    level = it.level,
                )
            }
            danmakuData.swapListWithMainContext(danmakuList)
            danmakuView?.setDanmakus(danmakuList)
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
        // 互动视频播完不自动连播：弹出选项（没有待选项的叶子节点不弹，等用户回退）
        if (isInteractivePlayback) {
            logger.info { "Interactive video finished, request option dialog" }
            requestInteractiveOptionDialog()
            return
        }
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
                danmakuView?.notifySeek(0L)
                videoPlayer?.seekTo(0L)
            }

            PlayMode.ListLoop -> {
                logger.info { "Play mode: $currentPlayMode, play next video in list or loop to first" }
                playNextVideoInList(loop = true)
            }
        }
    }

    private fun playNextVideoInList(loop: Boolean = false) {
        if (isInteractivePlayback || availableVideoList.any { it is VideoListInteractiveNode }) {
            logger.info { "Interactive branches detected, skip auto playing next branch" }
            return
        }

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