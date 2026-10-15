@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.aaa1115910.bv.ui.designsystem.theme

import android.content.Context
import android.util.LruCache
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.themeColors
import dev.aaa1115910.bv.entity.DarkMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CoverSeedCache {
    private val cache = LruCache<String, Int>(256)

    fun peek(url: String): Color? = cache.get(url)?.let { Color(it) }

    /** 取 112px 小图，后台线程量化取色，按 URL 缓存。失败返回 null。 */
    suspend fun get(context: Context, url: String): Color? {
        peek(url)?.let { return it }
        val request = ImageRequest.Builder(context).data(url).size(112).allowHardware(false).build()
        val drawable = (context.imageLoader.execute(request) as? SuccessResult)?.drawable ?: return null
        val color = withContext(Dispatchers.Default) {
            drawable.toBitmap().asImageBitmap().themeColors(fallback = Color.Unspecified).firstOrNull()
        } ?: return null
        if (!color.isSpecified) return null
        cache.put(url, color.toArgb())
        return color
    }
}

@Composable
fun rememberCoverSeed(url: String?): State<Color?> {
    val context = LocalContext.current
    return produceState(initialValue = url?.let { CoverSeedCache.peek(it) }, url) {
        value = url?.let { CoverSeedCache.get(context, it) }
    }
}

/**
 * 用封面颜色局部覆盖主题（原型里的 data-seed-cover）。
 * 结构永远不变：封面色没取到时用当前 primary 作种子，取到后颜色自动动画过渡。
 */
@Composable
fun CoverTheme(coverUrl: String?, forceDark: Boolean = false, content: @Composable () -> Unit) {
    val prefs = LocalThemePrefs.current
    val loaded by rememberCoverSeed(if (prefs.coverTheme) coverUrl else null)
    val dark = forceDark || LocalBvDark.current
    val seed = loaded ?: MaterialTheme.colorScheme.primary
    CompositionLocalProvider(LocalBvDark provides dark) {
        DynamicMaterialExpressiveTheme(
            seedColor = seed,
            motionScheme = MaterialTheme.motionScheme,
            isDark = dark,
            isAmoled = dark && prefs.darkMode == DarkMode.PureBlack,
            style = prefs.style,
            contrastLevel = prefs.contrast,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            shapes = MaterialTheme.shapes,
            typography = MaterialTheme.typography,
            animate = true,
            content = content,
        )
    }
}
