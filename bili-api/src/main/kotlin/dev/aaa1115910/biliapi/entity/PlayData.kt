package dev.aaa1115910.biliapi.entity

import bilibili.app.playerunite.v1.PlayViewUniteReply
import bilibili.pgc.gateway.player.v2.dashVideoOrNull
import bilibili.pgc.gateway.player.v2.dolbyOrNull
import bilibili.pgc.gateway.player.v2.segmentVideoOrNull
import bilibili.playershared.dashVideoOrNull
import bilibili.playershared.dolbyOrNull
import bilibili.playershared.lossLessItemOrNull
import bilibili.playershared.segmentVideoOrNull
import bilibili.playershared.volumeOrNull
import dev.aaa1115910.biliapi.http.entity.video.ClipInfo
import dev.aaa1115910.biliapi.http.entity.video.Durl
import dev.aaa1115910.biliapi.http.entity.video.ClipType
import dev.aaa1115910.biliapi.http.entity.video.resolveClipInfoList

data class PlayData(
    val dashVideos: List<DashVideo>,
    val dashAudios: List<DashAudio>,
    val dolby: DashAudio? = null,
    val flac: DashAudio? = null,
    val codec: Map<Int, List<String>> = emptyMap(),
    val needPay: Boolean = false,
    /** 响度元数据；为 null 表示服务端未提供（gRPC PGC 的 proto 没有该字段），播放器应保持原音量 */
    val volume: VolumeInfo? = null,
    /**
     * PGC 跳过片头/片尾配置；为 null 表示接口没有提供（UGC 恒为 null），
     * 播放器据此完全跳过相关逻辑。
     */
    val clipInfoList: List<ClipInfo>? = null,
) {
    companion object {
        fun fromPlayViewUniteReply(playViewUniteReply: PlayViewUniteReply): PlayData {
            val streamList = playViewUniteReply.vodInfo.streamListList
            val dashStreams = streamList.filter { it.dashVideoOrNull != null }
            val segmentStreams = streamList.filter { it.segmentVideoOrNull != null }
            // 付费/充电视频未购买时没有 dash，只有可试看的分段流
            val isPreview = dashStreams.isEmpty() && segmentStreams.isNotEmpty()
            val audioList = playViewUniteReply.vodInfo.dashAudioList
            val dolbyItem = playViewUniteReply.vodInfo.dolbyOrNull?.audioList?.firstOrNull()
            val lossLessItem =
                playViewUniteReply.vodInfo.lossLessItemOrNull?.audio.takeIf { it?.id != 0 }

            val previewDashVideos = if (isPreview) {
                segmentStreams.mapNotNull { stream ->
                    stream.segmentVideo.segmentList.firstOrNull()?.let { segment ->
                        DashVideo(
                            quality = stream.streamInfo.quality,
                            baseUrl = segment.url,
                            bandwidth = 0,
                            codecId = stream.streamInfo.quality,
                            width = 0,
                            height = 0,
                            frameRate = "",
                            backUrl = segment.backupUrlList,
                            codecs = CodeType.fromCodecId(stream.streamInfo.quality).str
                        )
                    }
                }
            } else {
                emptyList()
            }
            val dashVideos = dashStreams.map {
                DashVideo(
                    quality = it.streamInfo.quality,
                    baseUrl = it.dashVideo.baseUrl,
                    bandwidth = it.dashVideo.bandwidth,
                    codecId = it.dashVideo.codecid,
                    width = it.dashVideo.width,
                    height = it.dashVideo.height,
                    frameRate = it.dashVideo.frameRate,
                    backUrl = it.dashVideo.backupUrlList,
                    codecs = CodeType.fromCodecId(it.dashVideo.codecid).str
                )
            } + previewDashVideos
            val dashAudios = audioList.map {
                DashAudio(
                    baseUrl = it.baseUrl,
                    bandwidth = it.bandwidth,
                    codecId = it.id,
                    backUrl = it.backupUrlList
                )
            }
            val dolby = dolbyItem?.let {
                DashAudio(
                    baseUrl = it.baseUrl,
                    bandwidth = it.bandwidth,
                    codecId = it.id,
                    backUrl = it.backupUrlList
                )
            }
            val flac = lossLessItem?.let {
                DashAudio(
                    baseUrl = it.baseUrl,
                    bandwidth = it.bandwidth,
                    codecId = it.id,
                    backUrl = it.backupUrlList
                )
            }

            val codecs = if (dashStreams.isNotEmpty()) {
                dashStreams.associate {
                    it.streamInfo.quality to listOf(CodeType.fromCodecId(it.dashVideo.codecid).str)
                }
            } else {
                segmentStreams.associate {
                    it.streamInfo.quality to listOf(CodeType.fromCodecId(it.streamInfo.quality).str)
                }
            }

            return PlayData(
                dashVideos = dashVideos,
                dashAudios = dashAudios,
                dolby = dolby,
                flac = flac,
                codec = codecs,
                needPay = isPreview,
                volume = playViewUniteReply.vodInfo.volumeOrNull?.toVolumeInfo()
            )
        }

        fun fromPgcPlayViewReply(pgcPlayViewReply: bilibili.pgc.gateway.player.v2.PlayViewReply): PlayData {
            val streamList = pgcPlayViewReply.videoInfo.streamListList
            val dashStreams = streamList.filter { it.dashVideoOrNull != null }
            val segmentStreams = streamList.filter { it.segmentVideoOrNull != null }
            val isPreviewStreams = dashStreams.isEmpty() && segmentStreams.isNotEmpty()
            val audioList = pgcPlayViewReply.videoInfo.dashAudioList
            val dolbyItem = pgcPlayViewReply.videoInfo.dolbyOrNull?.audio
            val codecs = if (dashStreams.isNotEmpty()) {
                dashStreams.associate {
                    it.info.quality to listOf(CodeType.fromCodecId(it.dashVideo.codecid).str)
                }
            } else {
                segmentStreams.associate {
                    it.info.quality to listOf(CodeType.fromCodecId(it.info.quality).str)
                }
            }
            val needPay = pgcPlayViewReply.business.isPreview

            val previewDashVideos = if (isPreviewStreams) {
                segmentStreams.mapNotNull { stream ->
                    stream.segmentVideo.segmentList.firstOrNull()?.let { segment ->
                        DashVideo(
                            quality = stream.info.quality,
                            baseUrl = segment.url,
                            bandwidth = 0,
                            codecId = stream.info.quality,
                            width = 0,
                            height = 0,
                            frameRate = "",
                            backUrl = segment.backupUrlList,
                            codecs = CodeType.fromCodecId(stream.info.quality).str
                        )
                    }
                }
            } else {
                emptyList()
            }
            val dashVideos = dashStreams.map {
                DashVideo(
                    quality = it.info.quality,
                    baseUrl = it.dashVideo.baseUrl,
                    bandwidth = it.dashVideo.bandwidth,
                    codecId = it.dashVideo.codecid,
                    width = it.dashVideo.width,
                    height = it.dashVideo.height,
                    frameRate = it.dashVideo.frameRate,
                    backUrl = it.dashVideo.backupUrlList,
                    codecs = CodeType.fromCodecId(it.dashVideo.codecid).str
                )
            } + previewDashVideos
            val dashAudios = audioList.map {
                DashAudio(
                    baseUrl = it.baseUrl,
                    bandwidth = it.bandwidth,
                    codecId = it.id,
                    backUrl = it.backupUrlList
                )
            }
            val dolby = dolbyItem?.let {
                DashAudio(
                    baseUrl = it.baseUrl,
                    bandwidth = it.bandwidth,
                    codecId = it.codecid,
                    backUrl = it.backupUrlList
                )
            }

            // gRPC PGC 的 proto VideoInfo 没有 volume 字段，无法提供响度元数据
            return PlayData(
                dashVideos = dashVideos,
                dashAudios = dashAudios,
                dolby = dolby,
                flac = null,
                codec = codecs,
                needPay = needPay,
                clipInfoList = pgcPlayViewReply.business.clipInfoList
                    .map { it.toClipInfo() }
                    .takeIf { it.isNotEmpty() }
            )
        }

        fun fromPlayUrlData(playUrlData: dev.aaa1115910.biliapi.http.entity.video.PlayUrlData): PlayData {
            val hasDash = playUrlData.dash != null
            val isPreview = !hasDash && playUrlData.durl.isNotEmpty()
            val videos = playUrlData.dash?.video ?: emptyList()
            val audios = playUrlData.dash?.audio
            val dolbyItem = playUrlData.dash?.dolby?.audio?.firstOrNull()
            val flacItem = playUrlData.dash?.flac?.audio
            val codec = if (hasDash) {
                playUrlData.supportFormats.mapNotNull { format ->
                    format.codecs?.let { format.quality to it }
                }.toMap()
            } else {
                mapOf(
                    playUrlData.quality to
                        listOf(CodeType.fromCodecId(playUrlData.videoCodecId).str)
                )
            }
            val needPay = playUrlData.isPreview == 1 || isPreview

            val dashVideos = if (hasDash) {
                videos.map {
                    DashVideo(
                        quality = it.id,
                        baseUrl = it.baseUrl,
                        bandwidth = it.bandwidth,
                        codecId = it.id,
                        width = it.width,
                        height = it.height,
                        frameRate = it.frameRate,
                        backUrl = it.backupUrl,
                        codecs = it.codecs
                    )
                }
            } else {
                playUrlData.durl.toPreviewDashVideos(
                    quality = playUrlData.quality,
                    codecId = playUrlData.videoCodecId
                )
            }
            val dashAudios = audios?.map {
                DashAudio(
                    baseUrl = it.baseUrl,
                    bandwidth = it.bandwidth,
                    codecId = it.id,
                    backUrl = it.backupUrl
                )
            } ?: emptyList()
            val dolby = dolbyItem?.let {
                DashAudio(
                    baseUrl = it.baseUrl,
                    bandwidth = it.bandwidth,
                    codecId = it.id,
                    backUrl = it.backupUrl
                )
            }
            val flac = flacItem?.let {
                DashAudio(
                    baseUrl = it.baseUrl,
                    bandwidth = it.bandwidth,
                    codecId = it.id,
                    backUrl = it.backupUrl
                )
            }

            return PlayData(
                dashVideos = dashVideos,
                dashAudios = dashAudios,
                dolby = dolby,
                flac = flac,
                codec = codec,
                needPay = needPay,
                volume = playUrlData.volume,
                clipInfoList = playUrlData.resolveClipInfoList().takeIf { it.isNotEmpty() }
            )
        }

        fun fromPlayUrlData(playUrlData: dev.aaa1115910.biliapi.http.entity.proxy.ProxyWebPlayUrlData): PlayData {
            val hasDash = playUrlData.dash != null
            val isPreview = !hasDash && playUrlData.durl.isNotEmpty()
            val videos = playUrlData.dash?.video ?: emptyList()
            val audios = playUrlData.dash?.audio
            val dolbyItem = playUrlData.dash?.dolby?.audio?.firstOrNull()
            val flacItem = playUrlData.dash?.flac?.audio
            val codec = if (hasDash) {
                playUrlData.supportFormats.mapNotNull { format ->
                    format.codecs?.let { format.quality to it }
                }.toMap()
            } else {
                mapOf(
                    playUrlData.quality to
                        listOf(CodeType.fromCodecId(playUrlData.videoCodecId).str)
                )
            }
            val needPay = playUrlData.isPreview == 1 || isPreview

            val dashVideos = if (hasDash) {
                videos.map {
                    DashVideo(
                        quality = it.id,
                        baseUrl = it.baseUrl,
                        bandwidth = it.bandwidth,
                        codecId = it.id,
                        width = it.width,
                        height = it.height,
                        frameRate = it.frameRate,
                        backUrl = it.backupUrl,
                        codecs = it.codecs
                    )
                }
            } else {
                playUrlData.durl.toPreviewDashVideos(
                    quality = playUrlData.quality,
                    codecId = playUrlData.videoCodecId
                )
            }
            val dashAudios = audios?.map {
                DashAudio(
                    baseUrl = it.baseUrl,
                    bandwidth = it.bandwidth,
                    codecId = it.id,
                    backUrl = it.backupUrl
                )
            } ?: emptyList()
            val dolby = dolbyItem?.let {
                DashAudio(
                    baseUrl = it.baseUrl,
                    bandwidth = it.bandwidth,
                    codecId = it.id,
                    backUrl = it.backupUrl
                )
            }
            val flac = flacItem?.let {
                DashAudio(
                    baseUrl = it.baseUrl,
                    bandwidth = it.bandwidth,
                    codecId = it.id,
                    backUrl = it.backupUrl
                )
            }

            return PlayData(
                dashVideos = dashVideos,
                dashAudios = dashAudios,
                dolby = dolby,
                flac = flac,
                codec = codec,
                needPay = needPay,
                volume = playUrlData.volume,
                clipInfoList = playUrlData.clipInfoList.takeIf { it.isNotEmpty() }
            )
        }

        fun fromPlayUrlData(playUrlData: dev.aaa1115910.biliapi.http.entity.proxy.ProxyAppPlayUrlData): PlayData {
            val hasDash = playUrlData.dash != null
            val isPreview = !hasDash && playUrlData.durl.isNotEmpty()
            val videos = playUrlData.dash?.video ?: emptyList()
            val audios = playUrlData.dash?.audio
            val dolbyItem = playUrlData.dash?.dolby?.audio?.firstOrNull()
            val flacItem = playUrlData.dash?.flac?.audio
            val codec = if (hasDash) {
                playUrlData.supportFormats.mapNotNull { format ->
                    format.codecs?.let { format.quality to it }
                }.toMap()
            } else {
                mapOf(
                    playUrlData.quality to
                        listOf(CodeType.fromCodecId(playUrlData.videoCodecId).str)
                )
            }
            val needPay = playUrlData.isPreview == 1 || isPreview

            val dashVideos = if (hasDash) {
                videos.map {
                    DashVideo(
                        quality = it.id,
                        baseUrl = it.baseUrl,
                        bandwidth = it.bandwidth,
                        codecId = it.id,
                        width = it.width,
                        height = it.height,
                        frameRate = it.frameRate,
                        backUrl = it.backupUrl,
                        codecs = it.codecs
                    )
                }
            } else {
                playUrlData.durl.toPreviewDashVideos(
                    quality = playUrlData.quality,
                    codecId = playUrlData.videoCodecId
                )
            }
            val dashAudios = audios?.map {
                DashAudio(
                    baseUrl = it.baseUrl,
                    bandwidth = it.bandwidth,
                    codecId = it.id,
                    backUrl = it.backupUrl
                )
            } ?: emptyList()
            val dolby = dolbyItem?.let {
                DashAudio(
                    baseUrl = it.baseUrl,
                    bandwidth = it.bandwidth,
                    codecId = it.id,
                    backUrl = it.backupUrl
                )
            }
            val flac = flacItem?.let {
                DashAudio(
                    baseUrl = it.baseUrl,
                    bandwidth = it.bandwidth,
                    codecId = it.id,
                    backUrl = it.backupUrl
                )
            }

            return PlayData(
                dashVideos = dashVideos,
                dashAudios = dashAudios,
                dolby = dolby,
                flac = flac,
                codec = codec,
                needPay = needPay,
                volume = playUrlData.volume,
                clipInfoList = playUrlData.clipInfoList.takeIf { it.isNotEmpty() }
            )
        }

        /** 未购买时接口只下发 durl 试看片段，转成播放器使用的 DashVideo 结构。 */
        private fun List<Durl>.toPreviewDashVideos(quality: Int, codecId: Int): List<DashVideo> =
            map {
                DashVideo(
                    quality = quality,
                    baseUrl = it.url,
                    bandwidth = 0,
                    codecId = codecId,
                    width = 0,
                    height = 0,
                    frameRate = "",
                    backUrl = it.backupUrl,
                    codecs = CodeType.fromCodecId(codecId).str
                )
            }
    }

    operator fun plus(other: PlayData): PlayData {
        return PlayData(
            dashVideos = (dashVideos + other.dashVideos)
                .distinctBy { "${it.codecId}_${it.quality}" }
                .sortedByDescending { it.quality },
            dashAudios = (dashAudios + other.dashAudios)
                .distinctBy { it.codecId }
                .sortedByDescending { it.codecId },
            dolby = dolby ?: other.dolby,
            flac = flac ?: other.flac,
            codec = codec.map {
                it.key to (it.value + other.codec[it.key].orEmpty())
                    .distinct()
                    .filter { it != "none" }
            }.toMap(),
            needPay = needPay || other.needPay,
            volume = volume ?: other.volume,
            clipInfoList = (clipInfoList.orEmpty() + other.clipInfoList.orEmpty())
                .distinct()
                .takeIf { it.isNotEmpty() }
        )
    }
}

internal fun bilibili.pgc.gateway.player.v2.ClipInfo.toClipInfo() = ClipInfo(
    materialNo = materialNo,
    start = start,
    end = end,
    clipType = when (clipType) {
        bilibili.pgc.gateway.player.v2.ClipType.CLIP_TYPE_OP -> ClipType.Op
        bilibili.pgc.gateway.player.v2.ClipType.CLIP_TYPE_ED -> ClipType.Ed
        bilibili.pgc.gateway.player.v2.ClipType.CLIP_TYPE_HE -> ClipType.He
        bilibili.pgc.gateway.player.v2.ClipType.CLIP_TYPE_MULTI_VIEW -> ClipType.MultiView
        bilibili.pgc.gateway.player.v2.ClipType.CLIP_TYPE_AD -> ClipType.Ad
        else -> ClipType.Unknown
    },
    toastText = toastText,
)

/**
 * @param quality 视频分辨率
 * @param baseUrl 主线流
 * @param bandwidth 码率
 * @param codecId 编码ID
 * @param width 视频宽度
 * @param height 视频高度
 * @param frameRate 帧率
 * @param backUrl 备用流
 * @param codecs 编码格式 仅 Web 接口有该值
 */
data class DashVideo(
    val quality: Int,
    val baseUrl: String,
    val bandwidth: Int,
    val codecId: Int,
    val width: Int,
    val height: Int,
    val frameRate: String,
    val backUrl: List<String>,
    val codecs: String? = null
)

/**
 * @param baseUrl 主线流
 * @param bandwidth 码率
 * @param codecId 编码ID
 * @param backUrl 备用流
 */
data class DashAudio(
    val baseUrl: String,
    val bandwidth: Int,
    val codecId: Int,
    val backUrl: List<String>
)

/**
 * gRPC 的 VolumeInfo 没有 multi_scene_args，两挡目标响度会退化成共用 [VolumeInfo.targetI]。
 *
 * `targetOffset` 原样带过来仅作留档：增益策略不使用它（见 player 的 `AudioBalanceGain`），
 * 因此 proto3 把它读成 0.0 也不会影响结果。
 */
internal fun bilibili.playershared.VolumeInfo.toVolumeInfo() = VolumeInfo(
    measuredI = measuredI,
    measuredLra = measuredLra,
    measuredTp = measuredTp,
    measuredThreshold = measuredThreshold,
    targetOffset = targetOffset,
    targetI = targetI,
    targetTp = targetTp,
    multiSceneArgs = null,
)
