@file:Suppress("SpellCheckingInspection")

package dev.aaa1115910.bv.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.materialkolor.PaletteStyle
import de.schnettler.datastore.manager.PreferenceRequest
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.http.util.generateBuvid
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.entity.DarkMode
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.entity.ThemePrefs
import dev.aaa1115910.bv.entity.ThemeSource
import dev.aaa1115910.bv.entity.ThemeType
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.AudioBalanceLevel
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.PlayerLongPressAction
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoCodec
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.runBlocking
import java.util.Date
import java.util.UUID
import kotlin.math.roundToInt

object Prefs {
    private val dsm = BVApp.dataStoreManager
    val logger = KotlinLogging.logger { }

    var isLogin: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefIsLoginRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefIsLoginKey, value) }

    var uid: Long
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefUidRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefUidKey, value) }

    var sid: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefSidRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefSidKey, value) }

    var sessData: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefSessDataRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefSessDataKey, value) }

    var biliJct: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefBiliJctRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefBiliJctKey, value) }

    var uidCkMd5: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefUidCkMd5Request).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefUidCkMd5Key, value) }

    var tokenExpiredData: Date
        get() = Date(runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefTokenExpiredDateRequest).first()
        })
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefTokenExpiredDateKey, value.time)
        }

    var defaultQuality: Resolution
        get() = runBlocking {
            Resolution.fromCode(dsm.getPreferenceFlow(PrefKeys.prefDefaultQualityRequest).first())
                ?: Resolution.R1080P
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultQualityKey, value.code)
        }

    var defaultPlaySpeed: Float
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefDefaultPlaySpeedRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultPlaySpeedKey, value) }

    var defaultAudio: Audio
        get() = runBlocking {
            Audio.fromCode(dsm.getPreferenceFlow(PrefKeys.prefDefaultAudioRequest).first())
                ?: Audio.A192K
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultAudioKey, value.code) }

    var defaultDanmakuSize: Int
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuSizeRequest).first()
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultDanmakuSizeKey, value) }

    var defaultDanmakuScale: Float
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuScaleRequest).first()
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultDanmakuScaleKey, value) }

    var defaultDanmakuTransparency: Int
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuTransparencyRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultDanmakuTransparencyKey, value)
        }

    var defaultDanmakuOpacity: Float
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuOpacityRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultDanmakuOpacityKey, value)
        }

    var defaultDanmakuEnabled: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuEnabledRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultDanmakuEnabledKey, value)
        }

    var defaultDanmakuTypes: List<DanmakuType>
        get() = runBlocking {
            val danmakuTypeIdsString =
                dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuTypesRequest).first()
            if (danmakuTypeIdsString == "") {
                emptyList()
            } else {
                danmakuTypeIdsString.split(",").map { DanmakuType.entries[it.toInt()] }
            }
        }
        set(value) = runBlocking {
            dsm.editPreference(
                PrefKeys.prefDefaultDanmakuTypesKey,
                value.map { it.ordinal }.joinToString(",")
            )
        }

    var defaultDanmakuArea: Float
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuAreaRequest).first()
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultDanmakuAreaKey, value) }

    var defaultVideoCodec: dev.aaa1115910.bv.player.entity.VideoCodec
        get() = dev.aaa1115910.bv.player.entity.VideoCodec.Companion.fromCode(
            runBlocking { dsm.getPreferenceFlow(PrefKeys.prefDefaultVideoCodecRequest).first() }
        )
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultVideoCodecKey, value.ordinal)
        }

    var enableFirebaseCollection: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefEnabledFirebaseCollectionRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefEnabledFirebaseCollectionKey, value)
        }

    var incognitoMode: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefIncognitoModeRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefIncognitoModeKey, value) }

    var defaultSubtitleFontSize: TextUnit
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultSubtitleFontSizeRequest).first().sp
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultSubtitleFontSizeKey, value.value.roundToInt())
        }

    var defaultSubtitleBackgroundOpacity: Float
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultSubtitleBackgroundOpacityRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultSubtitleBackgroundOpacityKey, value)
        }

    var defaultSubtitleBottomPadding: Dp
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultSubtitleBottomPaddingRequest).first().dp
        }
        set(value) = runBlocking {
            dsm.editPreference(
                PrefKeys.prefDefaultSubtitleBottomPaddingKey, value.value.roundToInt()
            )
        }

    var showFps: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefShowFpsRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefShowFpsKey, value) }

    var buvid: String
        get() = runBlocking {
            val id = dsm.getPreferenceFlow(PrefKeys.prefBuvidRequest).first()
            if (id != "") {
                id
            } else {
                val randomBuvid = generateBuvid()
                buvid3 = randomBuvid
                randomBuvid
            }
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefBuvidKey, value) }

    var buvid3: String
        get() = runBlocking {
            val id = dsm.getPreferenceFlow(PrefKeys.prefBuvid3Request).first()
            if (id != "") {
                id
            } else {
                //random buvid3
                val randomBuvid3 = "${UUID.randomUUID()}${(0..9).random()}infoc"
                buvid3 = randomBuvid3
                randomBuvid3
            }
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefBuvid3Key, value) }

    /**
     * Web 指纹/票据 cookie（buvid3、buvid4、b_nut、bili_ticket 等），
     * 以 "name=value;name=value;" 格式存储。账号级 cookie 不放这里，避免登出后仍被注入。
     */
    var webExtraCookies: String
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefWebExtraCookiesRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefWebExtraCookiesKey, value)
        }

    fun getWebCookie(name: String): String? = webCookieMap(webExtraCookies)[name]

    fun setWebCookies(map: Map<String, String>) {
        webExtraCookies = buildString {
            (webCookieMap(webExtraCookies) + map).forEach { (key, value) ->
                if (value.isNotBlank()) append("$key=$value;")
            }
        }
    }

    private fun webCookieMap(raw: String): Map<String, String> =
        raw.split(";").mapNotNull { part ->
            val index = part.indexOf("=")
            if (index <= 0) return@mapNotNull null
            part.substring(0, index).trim() to part.substring(index + 1).trim()
        }.toMap()

    var playerType: PlayerType
        get() = runBlocking {
            runCatching {
                PlayerType.entries[dsm.getPreferenceFlow(PrefKeys.prefPlayerTypeRequest).first()]
            }.getOrDefault(PlayerType.Media3)
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefPlayerTypeKey, value.ordinal) }

    val densityFlow: Flow<Float> get() = dsm.getPreferenceFlow(PrefKeys.prefDensityRequest)
    var density: Float
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefDensityRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDensityKey, value) }

    var updateAlpha: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefAlphaRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefAlphaKey, value) }

    var accessToken: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefAccessTokenRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefAccessTokenKey, value) }

    var refreshToken: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefRefreshTokenRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefRefreshTokenKey, value) }

    var apiType: ApiType
        get() = runBlocking {
            ApiType.entries[dsm.getPreferenceFlow(PrefKeys.prefApiTypeRequest).first()]
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefApiTypeKey, value.ordinal) }

    var enableProxy: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefEnabelProxyRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefEnableProxyKey, value) }

    var proxyHttpServer: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefProxyHttpServerRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefProxyHttpServerKey, value) }

    var proxyGRPCServer: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefProxyGRPCServerRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefProxyGRPCServerKey, value) }

    var lastVersionCode: Int
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefLastVersionCodeRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefLastVersionCodeKey, value) }

    var showedRemoteControllerPanelDemo: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefShowedRemoteControllerPanelDemoRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefShowedRemoteControllerPanelDemoKey, value)
        }

    var preferOfficialCdn: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefPreferOfficialCdnRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefPreferOfficialCdn, value) }

    /** 起播时对候选 CDN 地址测速并按吞吐排序；默认关闭 */
    var autoSelectCdn: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefAutoSelectCdnRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefAutoSelectCdnKey, value) }

    /** 播放失败/频繁缓冲时自动切到下一个候选 CDN 地址；默认开启 */
    var enableCdnFailover: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefEnableCdnFailoverRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefEnableCdnFailoverKey, value) }

    var defaultDanmakuMask: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuMaskRequest).first()
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultDanmakuMask, value) }

    var enableFfmpegAudioRenderer: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefEnableFfmpegEndererRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(
                PrefKeys.prefEnableFfmpegAudioRenderer,
                value
            )
        }

    /** 音量均衡挡位，默认关闭；只在新播放器创建时读取 */
    var playerAudioBalanceLevel: AudioBalanceLevel
        get() = runBlocking {
            AudioBalanceLevel.entries.getOrElse(
                dsm.getPreferenceFlow(PrefKeys.prefPlayerAudioBalanceLevelRequest).first()
            ) { AudioBalanceLevel.Off }
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefPlayerAudioBalanceLevelKey, value.ordinal)
        }

    /** 跳过静音段，默认关闭；只在新播放器创建时读取（2.5 倍速及以上才真正生效） */
    var playerSkipSilence: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefPlayerSkipSilenceRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefPlayerSkipSilenceKey, value)
        }

    /** 自动跳过 PGC 片头片尾，默认关闭（照搬 fantasy） */
    var skipPgcIntroOutro: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefSkipPgcIntroOutroRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefSkipPgcIntroOutroKey, value)
        }

    /** 电视端长按确认键行为，默认加速播放（照搬 fantasy） */
    var playerLongPressAction: PlayerLongPressAction
        get() = runBlocking {
            PlayerLongPressAction.entries.getOrElse(
                dsm.getPreferenceFlow(PrefKeys.prefPlayerLongPressActionRequest).first()
            ) { PlayerLongPressAction.SpeedUp }
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefPlayerLongPressActionKey, value.ordinal)
        }

    /** 长按确认键加速播放时的倍速，默认 2× */
    var playerLongPressSpeed: Float
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefPlayerLongPressSpeedRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefPlayerLongPressSpeedKey, value)
        }

    var blacklistUser: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefBlacklistUserRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefBlacklistUserKey, value) }

    var themeType: ThemeType
        get() = runBlocking {
            ThemeType.entries[dsm.getPreferenceFlow(PrefKeys.prefThemeTypeRequest).first()]
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefThemeTypeKey, value.ordinal) }

    val themeTypeFlow: Flow<ThemeType>
        get() = dsm.getPreferenceFlow(PrefKeys.prefThemeTypeRequest)
            .transform { ordinal -> emit(ThemeType.entries[ordinal]) }

    var defaultPlayMode: PlayMode
        get() = runBlocking {
            PlayMode.entries[dsm.getPreferenceFlow(PrefKeys.prefPlayModeRequest).first()]
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefPlayModeKey, value.ordinal) }

    /** 缓存阈值（MB，0 = 不限制，默认不限制）。 */
    var cacheThreshold: Int
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefCacheThresholdRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefCacheThresholdKey, value)
        }

    /** 缓存自动清理开关（默认开启，但阈值为「不限制」时不会清理）。 */
    var cacheAutoClean: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefCacheAutoCleanRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefCacheAutoCleanKey, value)
        }

    val themePrefsFlow: Flow<ThemePrefs>
        get() = combine(
            dsm.getPreferenceFlow(PrefKeys.prefThemeSourceRequest),
            dsm.getPreferenceFlow(PrefKeys.prefSeedColorRequest),
            dsm.getPreferenceFlow(PrefKeys.prefPaletteStyleRequest),
            dsm.getPreferenceFlow(PrefKeys.prefContrastRequest),
            dsm.getPreferenceFlow(PrefKeys.prefDarkModeRequest),
        ) { source, seed, style, contrast, dark ->
            ThemePrefs(
                source = ThemeSource.entries[source],
                seed = Color(seed),
                style = PaletteStyle.entries[style],
                contrast = contrast.toDouble(),
                darkMode = DarkMode.entries[dark],
            )
        }
            .combine(dsm.getPreferenceFlow(PrefKeys.prefContentSeedRequest)) { p, c -> p.copy(contentSeed = if (c == 0) null else Color(c)) }
            .combine(dsm.getPreferenceFlow(PrefKeys.prefCoverThemeRequest)) { p, v -> p.copy(coverTheme = v) }
            .combine(dsm.getPreferenceFlow(PrefKeys.prefReduceMotionRequest)) { p, v -> p.copy(reduceMotion = v) }

    suspend fun setThemeSource(v: ThemeSource) = dsm.editPreference(PrefKeys.prefThemeSourceKey, v.ordinal)
    suspend fun setSeedColor(v: Color) = dsm.editPreference(PrefKeys.prefSeedColorKey, v.toArgb())
    suspend fun setContentSeed(v: Color) = dsm.editPreference(PrefKeys.prefContentSeedKey, v.toArgb())
    suspend fun setPaletteStyle(v: PaletteStyle) = dsm.editPreference(PrefKeys.prefPaletteStyleKey, v.ordinal)
    suspend fun setContrast(v: Float) = dsm.editPreference(PrefKeys.prefContrastKey, v)
    suspend fun setDarkMode(v: DarkMode) = dsm.editPreference(PrefKeys.prefDarkModeKey, v.ordinal)
    suspend fun setCoverTheme(v: Boolean) = dsm.editPreference(PrefKeys.prefCoverThemeKey, v)
    suspend fun setReduceMotion(v: Boolean) = dsm.editPreference(PrefKeys.prefReduceMotionKey, v)
}

object PrefKeys {
    val prefIsLoginKey = booleanPreferencesKey("il")
    val prefUidKey = longPreferencesKey("uid")
    val prefSidKey = stringPreferencesKey("sid")
    val prefSessDataKey = stringPreferencesKey("sd")
    val prefBiliJctKey = stringPreferencesKey("bj")
    val prefUidCkMd5Key = stringPreferencesKey("ucm")
    val prefTokenExpiredDateKey = longPreferencesKey("ted")
    val prefDefaultQualityKey = intPreferencesKey("dq")
    val prefDefaultAudioKey = intPreferencesKey("da")
    val prefDefaultPlaySpeedKey = floatPreferencesKey("dps")
    val prefDefaultDanmakuSizeKey = intPreferencesKey("dds")
    val prefDefaultDanmakuScaleKey = floatPreferencesKey("dds2")
    val prefDefaultDanmakuTransparencyKey = intPreferencesKey("ddt")
    val prefDefaultDanmakuOpacityKey = floatPreferencesKey("ddo")
    val prefDefaultDanmakuEnabledKey = booleanPreferencesKey("dde")
    val prefDefaultDanmakuTypesKey = stringPreferencesKey("ddts")
    val prefDefaultDanmakuAreaKey = floatPreferencesKey("dda")
    val prefDefaultVideoCodecKey = intPreferencesKey("dvc")
    val prefEnabledFirebaseCollectionKey = booleanPreferencesKey("efc")
    val prefIncognitoModeKey = booleanPreferencesKey("im")
    val prefDefaultSubtitleFontSizeKey = intPreferencesKey("dsfs")
    val prefDefaultSubtitleBackgroundOpacityKey = floatPreferencesKey("dsbo")
    val prefDefaultSubtitleBottomPaddingKey = intPreferencesKey("dsbp")
    val prefShowFpsKey = booleanPreferencesKey("sf")
    val prefBuvidKey = stringPreferencesKey("random_buvid")
    val prefBuvid3Key = stringPreferencesKey("random_buvid3")
    val prefWebExtraCookiesKey = stringPreferencesKey("web_extra_cookies")
    val prefPlayerTypeKey = intPreferencesKey("pt")
    val prefDensityKey = floatPreferencesKey("density")
    val prefAlphaKey = booleanPreferencesKey("alpha")
    val prefAccessTokenKey = stringPreferencesKey("access_token")
    val prefRefreshTokenKey = stringPreferencesKey("refresh_token")
    val prefApiTypeKey = intPreferencesKey("api_type")
    val prefEnableProxyKey = booleanPreferencesKey("enable_proxy")
    val prefProxyHttpServerKey = stringPreferencesKey("proxy_http_server")
    val prefProxyGRPCServerKey = stringPreferencesKey("proxy_grpc_server")
    val prefLastVersionCodeKey = intPreferencesKey("last_version_code")
    val prefShowedRemoteControllerPanelDemoKey = booleanPreferencesKey("showed_rcpd")
    val prefPreferOfficialCdn = booleanPreferencesKey("prefer_official_cdn")
    val prefAutoSelectCdnKey = booleanPreferencesKey("auto_select_cdn")
    val prefEnableCdnFailoverKey = booleanPreferencesKey("enable_cdn_failover")
    val prefDefaultDanmakuMask = booleanPreferencesKey("prefer_enable_webmark")
    val prefEnableFfmpegAudioRenderer = booleanPreferencesKey("enable_ffmpeg_audio_renderer")
    val prefPlayerAudioBalanceLevelKey = intPreferencesKey("player_audio_balance_level")
    val prefPlayerSkipSilenceKey = booleanPreferencesKey("player_skip_silence")
    val prefSkipPgcIntroOutroKey = booleanPreferencesKey("skip_pgc_intro_outro")
    val prefPlayerLongPressActionKey = intPreferencesKey("player_long_press_action")
    val prefPlayerLongPressSpeedKey = floatPreferencesKey("player_long_press_speed")
    val prefBlacklistUserKey = booleanPreferencesKey("blacklist_user")
    val prefThemeTypeKey = intPreferencesKey("theme_type")
    val prefPlayModeKey = intPreferencesKey("play_mode")
    val prefCacheThresholdKey = intPreferencesKey("cache_threshold")
    val prefCacheAutoCleanKey = booleanPreferencesKey("cache_auto_clean")

    val prefIsLoginRequest = PreferenceRequest(prefIsLoginKey, false)
    val prefUidRequest = PreferenceRequest(prefUidKey, 0)
    val prefSidRequest = PreferenceRequest(prefSidKey, "")
    val prefSessDataRequest = PreferenceRequest(prefSessDataKey, "")
    val prefBiliJctRequest = PreferenceRequest(prefBiliJctKey, "")
    val prefUidCkMd5Request = PreferenceRequest(prefUidCkMd5Key, "")
    val prefTokenExpiredDateRequest = PreferenceRequest(prefTokenExpiredDateKey, 0)
    val prefDefaultPlaySpeedRequest = PreferenceRequest(prefDefaultPlaySpeedKey, 1f)
    val prefDefaultQualityRequest = PreferenceRequest(prefDefaultQualityKey, Resolution.R1080P.code)
    val prefDefaultAudioRequest = PreferenceRequest(prefDefaultAudioKey, Audio.A192K.code)
    val prefDefaultDanmakuSizeRequest = PreferenceRequest(prefDefaultDanmakuSizeKey, 6)
    val prefDefaultDanmakuScaleRequest = PreferenceRequest(prefDefaultDanmakuScaleKey, 1f)
    val prefDefaultDanmakuTransparencyRequest =
        PreferenceRequest(prefDefaultDanmakuTransparencyKey, 0)
    val prefDefaultDanmakuOpacityRequest = PreferenceRequest(prefDefaultDanmakuOpacityKey, 1f)
    val prefDefaultDanmakuEnabledRequest = PreferenceRequest(prefDefaultDanmakuEnabledKey, true)
    val prefDefaultDanmakuTypesRequest =
        PreferenceRequest(prefDefaultDanmakuTypesKey, "0,1,2,3")
    val prefDefaultDanmakuAreaRequest = PreferenceRequest(prefDefaultDanmakuAreaKey, 1f)
    val prefDefaultVideoCodecRequest =
        PreferenceRequest(prefDefaultVideoCodecKey, VideoCodec.AVC.ordinal)
    val prefEnabledFirebaseCollectionRequest =
        PreferenceRequest(prefEnabledFirebaseCollectionKey, true)
    val prefIncognitoModeRequest = PreferenceRequest(prefIncognitoModeKey, false)
    val prefDefaultSubtitleFontSizeRequest = PreferenceRequest(prefDefaultSubtitleFontSizeKey, 24)
    val prefDefaultSubtitleBackgroundOpacityRequest =
        PreferenceRequest(prefDefaultSubtitleBackgroundOpacityKey, 0.4f)
    val prefDefaultSubtitleBottomPaddingRequest =
        PreferenceRequest(prefDefaultSubtitleBottomPaddingKey, 12)
    val prefShowFpsRequest = PreferenceRequest(prefShowFpsKey, false)
    val prefBuvidRequest = PreferenceRequest(prefBuvidKey, "")
    val prefBuvid3Request = PreferenceRequest(prefBuvid3Key, "")
    val prefWebExtraCookiesRequest = PreferenceRequest(prefWebExtraCookiesKey, "")
    val prefPlayerTypeRequest = PreferenceRequest(prefPlayerTypeKey, PlayerType.Media3.ordinal)
    val prefDensityRequest =
        PreferenceRequest(
            prefDensityKey,
            runCatching { BVApp.context.resources.displayMetrics.widthPixels / 960f }
                .getOrDefault(2f)
        )

    @Suppress("KotlinConstantConditions")
    val prefAlphaRequest = PreferenceRequest(prefAlphaKey, BuildConfig.BUILD_TYPE == "alpha")
    val prefAccessTokenRequest = PreferenceRequest(prefAccessTokenKey, "")
    val prefRefreshTokenRequest = PreferenceRequest(prefRefreshTokenKey, "")
    val prefApiTypeRequest = PreferenceRequest(prefApiTypeKey, 0)
    val prefEnabelProxyRequest = PreferenceRequest(prefEnableProxyKey, false)
    val prefProxyHttpServerRequest = PreferenceRequest(prefProxyHttpServerKey, "")
    val prefProxyGRPCServerRequest = PreferenceRequest(prefProxyGRPCServerKey, "")
    val prefLastVersionCodeRequest = PreferenceRequest(prefLastVersionCodeKey, 0)
    val prefShowedRemoteControllerPanelDemoRequest =
        PreferenceRequest(prefShowedRemoteControllerPanelDemoKey, false)
    val prefPreferOfficialCdnRequest = PreferenceRequest(prefPreferOfficialCdn, false)
    val prefAutoSelectCdnRequest = PreferenceRequest(prefAutoSelectCdnKey, false)
    val prefEnableCdnFailoverRequest = PreferenceRequest(prefEnableCdnFailoverKey, true)
    val prefDefaultDanmakuMaskRequest = PreferenceRequest(prefDefaultDanmakuMask, false)
    val prefEnableFfmpegEndererRequest = PreferenceRequest(prefEnableFfmpegAudioRenderer, false)
    val prefPlayerAudioBalanceLevelRequest =
        PreferenceRequest(prefPlayerAudioBalanceLevelKey, AudioBalanceLevel.Off.ordinal)
    val prefPlayerSkipSilenceRequest = PreferenceRequest(prefPlayerSkipSilenceKey, false)
    val prefSkipPgcIntroOutroRequest = PreferenceRequest(prefSkipPgcIntroOutroKey, false)
    val prefPlayerLongPressActionRequest =
        PreferenceRequest(prefPlayerLongPressActionKey, PlayerLongPressAction.SpeedUp.ordinal)
    val prefPlayerLongPressSpeedRequest = PreferenceRequest(prefPlayerLongPressSpeedKey, 2f)
    val prefBlacklistUserRequest = PreferenceRequest(prefBlacklistUserKey, false)
    val prefThemeTypeRequest = PreferenceRequest(prefThemeTypeKey, ThemeType.Auto.ordinal)
    val prefPlayModeRequest = PreferenceRequest(prefPlayModeKey, PlayMode.Sequential.ordinal)
    val prefCacheThresholdRequest = PreferenceRequest(prefCacheThresholdKey, 0)
    val prefCacheAutoCleanRequest = PreferenceRequest(prefCacheAutoCleanKey, true)

    val prefThemeSourceKey = intPreferencesKey("bv_theme_source")
    val prefSeedColorKey = intPreferencesKey("bv_seed_color")
    val prefContentSeedKey = intPreferencesKey("bv_content_seed")          // 0 表示没有
    val prefPaletteStyleKey = intPreferencesKey("bv_palette_style")
    val prefContrastKey = floatPreferencesKey("bv_contrast")
    val prefDarkModeKey = intPreferencesKey("bv_dark_mode")
    val prefCoverThemeKey = booleanPreferencesKey("bv_cover_theme")
    val prefReduceMotionKey = booleanPreferencesKey("bv_reduce_motion")

    val prefThemeSourceRequest = PreferenceRequest(prefThemeSourceKey, ThemeSource.Custom.ordinal)
    val prefSeedColorRequest = PreferenceRequest(prefSeedColorKey, 0xFFFB7299.toInt())
    val prefContentSeedRequest = PreferenceRequest(prefContentSeedKey, 0)
    val prefPaletteStyleRequest = PreferenceRequest(prefPaletteStyleKey, PaletteStyle.Vibrant.ordinal)
    val prefContrastRequest = PreferenceRequest(prefContrastKey, 0f)
    val prefDarkModeRequest = PreferenceRequest(prefDarkModeKey, DarkMode.System.ordinal)
    val prefCoverThemeRequest = PreferenceRequest(prefCoverThemeKey, true)
    val prefReduceMotionRequest = PreferenceRequest(prefReduceMotionKey, false)
}