package dev.aaa1115910.bv.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.DashAudio
import dev.aaa1115910.biliapi.entity.DashVideo
import dev.aaa1115910.biliapi.entity.PlayData
import dev.aaa1115910.biliapi.repositories.VideoPlayRepository
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.net.URI

/**
 * 杜比视界编码不是每个片源都提供，缺失时回退到同族的 H.265，
 * 避免选了 DVH1 / HVC1 的片源在没有杜比视界时直接掉到 H.264。
 */
private val VideoCodec.fallbackCodecs: List<VideoCodec>
    get() = when (this) {
        VideoCodec.HVC1 -> listOf(VideoCodec.HEVC)
        VideoCodec.DVH1 -> listOf(VideoCodec.HVC1, VideoCodec.HEVC)
        else -> emptyList()
    }

/**
 * 根据 aid / cid（PGC 另需 epid）获取播放地址，并按用户设置选择清晰度、视频编码、音频和 CDN。
 *
 * 逻辑从 VideoPlayerV3ViewModel 中抽出，播放器和刷刷的播放器池共用这一份实现：
 * - 播放器按步骤调用 [fetchPlayData] → [getAvailableQualities] / [getAvailableAudios] →
 *   [selectQuality] / [selectAudio] → [selectCodec] → [selectStreams]，以便保留切换清晰度等交互状态；
 * - 刷刷等只要一个结果的地方直接调用 [resolve]。
 *
 * 本类无状态，所有方法都可以在任意线程调用（网络请求在 bili-api 内部切换线程）。
 */
@Single
class PlayUrlResolver(
    private val videoPlayRepository: VideoPlayRepository
) {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    /**
     * 获取播放数据
     *
     * @param isPgc 为 true 时按剧集（番剧、影视）请求，此时 [epid] 有效
     * @param preferCodec 仅 PGC 请求使用
     * @param proxyArea 仅 PGC 请求使用，非大陆地区时走代理服务器
     */
    suspend fun fetchPlayData(
        aid: Long,
        cid: Long,
        epid: Int = 0,
        isPgc: Boolean = false,
        preferApiType: ApiType = Prefs.apiType,
        preferCodec: VideoCodec = Prefs.defaultVideoCodec,
        proxyArea: ProxyArea = ProxyArea.MainLand
    ): PlayData {
        logger.fInfo { "Fetch play data: [av=$aid, cid=$cid, epid=$epid, isPgc=$isPgc, preferApi=$preferApiType, proxyArea=$proxyArea]" }
        return if (isPgc) {
            videoPlayRepository.getPgcPlayData(
                aid = aid,
                cid = cid,
                epid = epid,
                preferCodec = preferCodec.toBiliApiCodeType(),
                preferApiType = preferApiType,
                enableProxy = proxyArea != ProxyArea.MainLand,
                proxyArea = when (proxyArea) {
                    ProxyArea.MainLand -> ""
                    ProxyArea.HongKong -> "hk"
                    ProxyArea.TaiWan -> "tw"
                }
            )
        } else {
            videoPlayRepository.getPlayData(
                aid = aid,
                cid = cid,
                preferApiType = preferApiType
            )
        }
    }

    /**
     * 播放数据里的全部清晰度（去重，保持接口返回顺序）
     */
    fun getAvailableQualities(playData: PlayData): List<Resolution> {
        val resolutionList = mutableListOf<Resolution>()
        playData.dashVideos.forEach {
            Resolution.fromCode(it.quality)?.let { resolution ->
                if (!resolutionList.contains(resolution)) resolutionList.add(resolution)
            }
        }
        logger.fInfo { "Video available resolution: $resolutionList" }
        return resolutionList
    }

    /**
     * 播放数据里的全部音频（普通音轨去重，再追加杜比、无损）
     */
    fun getAvailableAudios(playData: PlayData): List<Audio> {
        val audioList = mutableListOf<Audio>()
        playData.dashAudios.forEach {
            Audio.fromCode(it.codecId)?.let { audio ->
                if (!audioList.contains(audio)) audioList.add(audio)
            }
        }
        playData.dolby?.let {
            Audio.fromCode(it.codecId)?.let { audio -> audioList.add(audio) }
        }
        playData.flac?.let {
            Audio.fromCode(it.codecId)?.let { audio -> audioList.add(audio) }
        }
        logger.fInfo { "Video available audio: $audioList" }
        return audioList
    }

    /**
     * 选择清晰度：[preferred] 可用时返回 [current]（播放器用来保留当前选择，默认就是 [preferred]）；
     * 否则返回低于 [preferred] 的最高清晰度，都没有时返回最低清晰度。
     *
     * @throws NoSuchElementException [available] 为空
     */
    fun selectQuality(
        available: List<Resolution>,
        preferred: Resolution = Prefs.defaultQuality,
        current: Resolution = preferred
    ): Resolution {
        if (available.contains(preferred)) return current
        val sorted = available.sortedByDescending { it.code }
        return sorted.firstOrNull { it.code < preferred.code } ?: sorted.last()
    }

    /**
     * 在 [selectQuality] 基础上加清晰度上限（例如移动网络下 720P），[maxQuality] 为 null 时不限制
     */
    fun selectQualityCapped(
        available: List<Resolution>,
        preferred: Resolution = Prefs.defaultQuality,
        maxQuality: Resolution?
    ): Resolution {
        val capped =
            if (maxQuality != null && preferred.code > maxQuality.code) maxQuality else preferred
        return selectQuality(available = available, preferred = capped, current = capped)
    }

    /**
     * 选择音质：[preferred] 可用时返回 [current]；否则杜比全景声与 Hi-Res 互为替补，再依次尝试
     * 192K、132K、64K，最后取第一个可用音质。[available] 为空（App 源可能只有视频流）时返回 [current]。
     */
    fun selectAudio(
        available: List<Audio>,
        preferred: Audio = Prefs.defaultAudio,
        current: Audio = preferred
    ): Audio {
        if (available.contains(preferred)) return current
        return when {
            preferred == Audio.ADolbyAtoms && available.contains(Audio.AHiRes) -> Audio.AHiRes
            preferred == Audio.AHiRes && available.contains(Audio.ADolbyAtoms) -> Audio.ADolbyAtoms
            available.contains(Audio.A192K) -> Audio.A192K
            available.contains(Audio.A132K) -> Audio.A132K
            available.contains(Audio.A64K) -> Audio.A64K
            else -> available.firstOrNull() ?: current
        }
    }

    /**
     * 视频编码选择结果
     *
     * @param available 该清晰度下可用的编码；为 null 表示接口没有给出编码列表（App 接口），
     * 播放器此时不更新可选编码列表
     * @param selected 最终使用的编码
     */
    data class CodecSelection(
        val available: List<VideoCodec>?,
        val selected: VideoCodec
    )

    /**
     * 选择清晰度[quality]下的视频编码：[preferred] 可用则用它，否则用可用编码里排序最靠前的
     */
    fun selectCodec(
        playData: PlayData,
        quality: Resolution,
        preferred: VideoCodec = Prefs.defaultVideoCodec,
        apiType: ApiType = Prefs.apiType
    ): CodecSelection {
        if (apiType == ApiType.App && playData.codec.isEmpty()) {
            // 纠正当前实际播放的编码
            val videoItem = playData.dashVideos.find { it.quality == quality.code }
                ?: playData.dashVideos.first()
            return CodecSelection(
                available = null,
                selected = VideoCodec.fromCodecId(videoItem.codecId)
            )
        }

        val codecList = playData.codec[quality.code].orEmpty()
            .mapNotNull { VideoCodec.fromCodecString(it) }
        logger.fInfo { "Video available codec: $codecList" }
        logger.fInfo { "Default codec: $preferred" }

        val selected = when {
            codecList.contains(preferred) -> preferred
            codecList.isNotEmpty() -> preferred.fallbackCodecs.firstOrNull { it in codecList }
                ?: codecList.minBy { it.ordinal }
            // 编码表里没有该清晰度时，按实际视频流推断，避免直接失败
            else -> playData.dashVideos.firstOrNull { it.quality == quality.code }
                ?.let { video ->
                    video.codecs?.let { VideoCodec.fromCodecString(it) }
                        ?: VideoCodec.fromCodecId(video.codecId)
                }
                ?: preferred
        }
        logger.fInfo { "Select codec: $selected" }
        return CodecSelection(available = codecList, selected = selected)
    }

    /**
     * 选出的音视频流
     *
     * @param videoUrl 视频地址（已按设置选择官方 CDN / 替换代理 CDN）
     * @param audioUrl 音频地址；为 null 表示只有视频流（或视频地址本身已含音频）
     * @param video 与所选清晰度、编码完全匹配的视频流；为 null 表示没有匹配项，[videoUrl] 取自第一条视频流
     * @param audio 所选音频流
     * @param videoUrls 视频候选地址：首个就是 [videoUrl]，其后是备用 CDN（已去重）。
     *   启用代理时只有替换后的单个地址，避免切到未经代理的备用地址
     * @param audioUrls 音频候选地址：首个就是 [audioUrl]，其后是备用 CDN；无音频流时为空
     */
    data class SelectedStreams(
        val videoUrl: String,
        val audioUrl: String?,
        val video: DashVideo?,
        val audio: DashAudio?,
        val videoUrls: List<String> = listOf(videoUrl),
        val audioUrls: List<String> = listOfNotNull(audioUrl)
    )

    /**
     * 按清晰度[qn]、编码[codec]、音质[audio]从播放数据中选出音视频流和 CDN 地址
     */
    fun selectStreams(
        playData: PlayData,
        qn: Int,
        codec: VideoCodec,
        audio: Audio,
        apiType: ApiType = Prefs.apiType,
        proxyArea: ProxyArea = ProxyArea.MainLand,
        enableProxy: Boolean = Prefs.enableProxy,
        preferOfficialCdn: Boolean = Prefs.preferOfficialCdn
    ): SelectedStreams {
        val videoItem = playData.dashVideos.find {
            when (apiType) {
                ApiType.Web -> it.quality == qn && it.codecs!!.startsWith(codec.prefix)
                ApiType.App -> {
                    if (playData.codec.isEmpty()) it.quality == qn
                    else it.quality == qn && it.codecs!!.startsWith(codec.prefix)
                }
            }
        }
        val firstVideo = playData.dashVideos.firstOrNull()
            ?: throw IllegalStateException("No video stream in play data")
        var videoUrl = videoItem?.baseUrl ?: firstVideo.baseUrl
        val videoUrls = mutableListOf<String>()
        if (videoItem != null) {
            videoUrls.add(videoItem.baseUrl)
            videoUrls.addAll(videoItem.backUrl)
        } else {
            // 没有完全匹配的流时退回第一条视频流，同样参与 CDN 选择
            videoUrls.add(firstVideo.baseUrl)
            videoUrls.addAll(firstVideo.backUrl)
        }

        val audioItem = playData.dashAudios.find { it.codecId == audio.code }
            ?: playData.dolby.takeIf { it?.codecId == audio.code }
            ?: playData.flac.takeIf { it?.codecId == audio.code }
            ?: playData.dashAudios.minByOrNull { it.codecId }
        // App 播放源可能返回空的 dashAudios，此时允许仅播放视频
        var audioUrl: String? = audioItem?.baseUrl
        val audioUrls = mutableListOf<String>()
        audioItem?.baseUrl?.let { audioUrls.add(it) }
        audioUrls.addAll(audioItem?.backUrl ?: emptyList())

        logger.fInfo { "all video hosts: ${videoUrls.map { it.urlHost() }}" }
        logger.fInfo { "all audio hosts: ${audioUrls.map { it.urlHost() }}" }

        //replace cdn
        val selectedVideoUrls: List<String>
        val selectedAudioUrls: List<String>
        if (enableProxy && proxyArea != ProxyArea.MainLand) {
            videoUrl = videoUrl.replaceUrlDomainWithAliCdn()
            audioUrl = audioUrl?.replaceUrlDomainWithAliCdn()
            // 代理场景不暴露原始备用地址，避免自动切换绕过代理
            selectedVideoUrls = listOf(videoUrl)
            selectedAudioUrls = listOfNotNull(audioUrl)
        } else {
            // 如果未通过网络代理获得播放地址，才判断是否应该替换为官方 cdn
            videoUrl = selectOfficialCdnUrl(videoUrls, preferOfficialCdn)
            audioUrl = if (audioUrls.isNotEmpty()) selectOfficialCdnUrl(audioUrls, preferOfficialCdn) else null
            selectedVideoUrls = candidateUrls(videoUrl, videoUrls)
            selectedAudioUrls = audioUrl?.let { candidateUrls(it, audioUrls) } ?: emptyList()
        }
        logger.fInfo { "Select audio: $audioItem" }

        return SelectedStreams(
            videoUrl = videoUrl,
            audioUrl = audioUrl,
            video = videoItem,
            audio = audioItem,
            videoUrls = selectedVideoUrls,
            audioUrls = selectedAudioUrls
        )
    }

    /**
     * 候选地址列表：当前选中的地址放首位，其余保持接口返回顺序（已去重）。
     * 供起播测速排序与播放失败自动切换使用；列表首项始终等于选中地址。
     */
    private fun candidateUrls(selectedUrl: String, urls: List<String>): List<String> =
        (listOf(selectedUrl) + urls.filter { it.isNotBlank() }).distinct()

    /**
     * 一次解析的完整结果
     *
     * @param quality 实际选择的清晰度
     * @param codec 实际选择的视频编码
     * @param audio 实际选择的音质；只有视频流时为 null
     * @param width 视频流宽度（未知时为 0）
     * @param height 视频流高度（未知时为 0）
     * @param availableQualities 全部可选清晰度（不受上限影响，可用于手动切换）
     * @param availableCodecs 所选清晰度下可用的编码；接口未给出时为空列表
     * @param videoUrls 视频候选地址：首个就是 [videoUrl]，其后为备用 CDN
     * @param audioUrls 音频候选地址：首个就是 [audioUrl]，其后为备用 CDN；无音频流时为空
     */
    data class ResolvedPlayUrl(
        val aid: Long,
        val cid: Long,
        val videoUrl: String,
        val audioUrl: String?,
        val quality: Resolution,
        val codec: VideoCodec,
        val audio: Audio?,
        val width: Int,
        val height: Int,
        val availableQualities: List<Resolution>,
        val availableCodecs: List<VideoCodec>,
        val availableAudios: List<Audio>,
        val playData: PlayData,
        val videoUrls: List<String> = listOf(videoUrl),
        val audioUrls: List<String> = listOfNotNull(audioUrl)
    )

    /**
     * 需要付费（或仅能试看）时由 [resolve] 抛出
     */
    class NeedPayException(message: String = "This video needs to be paid") :
        IllegalStateException(message)

    /**
     * 一步解析播放地址：获取播放数据并按用户设置选好清晰度、编码、音质与 CDN。
     *
     * @param epid 剧集 epid，非 null 时按 PGC 请求
     * @param maxQuality 清晰度上限（例如移动网络下传 [Resolution.R720P]），null 表示不限制
     * @throws NeedPayException 需要购买
     */
    suspend fun resolve(
        aid: Long,
        cid: Long,
        epid: Int? = null,
        maxQuality: Resolution? = null,
        preferredQuality: Resolution = Prefs.defaultQuality,
        preferredCodec: VideoCodec = Prefs.defaultVideoCodec,
        preferredAudio: Audio = Prefs.defaultAudio,
        preferApiType: ApiType = Prefs.apiType,
        proxyArea: ProxyArea = ProxyArea.MainLand
    ): ResolvedPlayUrl {
        val playData = fetchPlayData(
            aid = aid,
            cid = cid,
            epid = epid ?: 0,
            isPgc = epid != null,
            preferApiType = preferApiType,
            preferCodec = preferredCodec,
            proxyArea = proxyArea
        )
        // 只有没有任何可播放流时才拦截；付费/充电视频未购买时会下发试看片段
        if (playData.needPay && playData.dashVideos.isEmpty()) throw NeedPayException()

        val qualities = getAvailableQualities(playData)
        val audios = getAvailableAudios(playData)
        val quality = selectQualityCapped(qualities, preferredQuality, maxQuality)
        val audio = selectAudio(audios, preferredAudio)
        val codecSelection = selectCodec(playData, quality, preferredCodec, preferApiType)
        val streams = selectStreams(
            playData = playData,
            qn = quality.code,
            codec = codecSelection.selected,
            audio = audio,
            apiType = preferApiType,
            proxyArea = proxyArea
        )
        val videoStream = streams.video ?: playData.dashVideos.first()
        logger.fInfo { "Resolved play url: [av=$aid, cid=$cid, quality=$quality, codec=${codecSelection.selected}, audio=${streams.audio?.codecId}]" }
        return ResolvedPlayUrl(
            aid = aid,
            cid = cid,
            videoUrl = streams.videoUrl,
            audioUrl = streams.audioUrl,
            quality = quality,
            codec = codecSelection.selected,
            audio = streams.audio?.let { Audio.fromCode(it.codecId) },
            width = videoStream.width,
            height = videoStream.height,
            availableQualities = qualities,
            availableCodecs = codecSelection.available.orEmpty(),
            availableAudios = audios,
            playData = playData,
            videoUrls = streams.videoUrls,
            audioUrls = streams.audioUrls
        )
    }

    /**
     * 刷刷播放器池等场景的清晰度上限：当前网络按流量计费（移动数据）时返回 720P，否则返回 null（使用用户设置）
     */
    fun recommendedMaxQuality(context: Context): Resolution? {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return null
        return if (runCatching { connectivityManager.isActiveNetworkMetered }.getOrDefault(false)) {
            Resolution.R720P
        } else {
            null
        }
    }

    private fun String.urlHost(): String =
        runCatching { with(URI(this)) { "$scheme://$authority" } }.getOrDefault(this)

    private fun String.replaceUrlDomainWithAliCdn(): String {
        val replaceDomainKeywords = listOf(
            "mirroraliov",
            "mirrorakam"
        )
        if (replaceDomainKeywords.none { this.contains(it) }) return this

        return Uri.parse(this)
            .buildUpon()
            .authority("upos-sz-mirrorali.bilivideo.com")
            .build()
            .toString()
    }

    private fun selectOfficialCdnUrl(urls: List<String>, preferOfficialCdn: Boolean): String {
        if (!preferOfficialCdn) {
            logger.fInfo { "doesn't need to filter official cdn url, select the first url" }
            return urls.first()
        }
        val filteredUrls = urls
            .filter { !it.contains(".mcdn.bilivideo.") }
            .filter { !it.contains(".szbdyd.com") }
            .filter {
                !Regex("^(https?://)?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d{1,5})?)(/[a-zA-Z0-9_./-]*)?(\\?.*)?$")
                    .matches(it)
            }
        if (filteredUrls.isEmpty()) {
            logger.fInfo { "doesn't find any official cdn url, select the first url" }
            return urls.first()
        } else {
            logger.fInfo { "filtered official cdn urls: $filteredUrls" }
            return filteredUrls.first()
        }
    }
}
