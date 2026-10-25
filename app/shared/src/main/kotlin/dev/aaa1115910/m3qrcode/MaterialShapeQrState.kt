package dev.aaa1115910.m3qrcode

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import com.google.zxing.qrcode.encoder.QRCode
import dev.aaa1115910.bv.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random


internal data class MaterialShapeQrState(
    private val context: Context,
    private val arrayOf1x1Shapes: List<Drawable>,
    private val arrayOf1x1SemiCircleShapes: List<Drawable>,
    private val arrayOf2x2Shapes: List<Drawable>,
    private val arrayOf3x3Shapes: List<Drawable>,
    private val arrayOf7x7Shapes: List<Drawable>,
    private val arrayOfHorizontalBarShapes: List<Drawable>,
    private val arrayOfHorizontalHalfCapsuleBarShapes: List<Drawable>,
    private val arrayOfVerticalBarShapes: List<Drawable>,
    private val arrayOfFinderPatternCenterShapes: List<Drawable>,
    private val foregroundColorPrimary: Int,
    private val foregroundColorSecondary: Int,
    private val foregroundColorAccent: Int,
    private val backgroundShapeColor: Int,
    private val backgroundDotColor1: Int,
    private val backgroundDotColor2: Int,
    private val backgroundDotColor3: Int,
    private val mainForegroundColorArray: List<Int>,
    private val mainBackgroundColorArray: List<Int>,
    val colorMap: MutableMap<String, Int>

) {
    val finderPatternShapeList = mutableStateListOf<MaterialShapeRenderer>()
    val dataModuleShapeList = mutableStateListOf<MaterialShapeRenderer>()
    val backgroundModuleShapeList = mutableStateListOf<MaterialShapeRenderer>()
    /** 纯生成期去重矩阵，不需要被 Snapshot 观察。 */
    private var hasCreated: Array<BooleanArray> = emptyArray()

    var finderPatternCenterShapeIndex: Int = 0

    /** 仅由帧循环和生成协程读写，不参与 Composition。 */
    var hasFinalDataImage: Boolean = false
    var qrStartTime: Long = 0L
    var qrcode: QRCode? = null
    var qrcodeSize by mutableIntStateOf(0)
    var qrcodeLineCount by mutableIntStateOf(0)
    var moduleSize: Int = 29


    var frameElapsed by mutableLongStateOf(0L)
    var dataFrameElapsed by mutableLongStateOf(0L)
    var lottieScale by mutableFloatStateOf(0f)


    fun getColorForFinderPattern(): Int {
        return foregroundColorPrimary
    }

    fun calculateAnimationBackgroundAlpha(elapsedMs: Long): Float {
        return when {
            elapsedMs < 250 -> 0.0f
            elapsedMs < 583 -> (elapsedMs - 250).toFloat() / 333
            else -> 1.0f
        }
    }

    fun calculateAnimationBackgroundScale(elapsedMs: Long): Float {
        // 根据时间 elapsedMs 计算背景动画的缩放比例
        if (elapsedMs < 250L) return 0.0f
        if (elapsedMs >= 1083L) return 1.0f
        val t = (elapsedMs - 250L).toFloat() / 833f
        return (EmphasizedInterpolator.getInterpolation(t) * 0.19999999f) + 0.8f
    }

    fun drawAnimationBackground(elapsedMs: Long) {
        // 即使慢帧跳过动画终点，也必须落到最终比例和颜色。
        lottieScale = calculateAnimationBackgroundScale(elapsedMs)
        val alphaFraction = calculateAnimationBackgroundAlpha(elapsedMs)
        val alphaColor = ColorUtils.setAlphaComponent(backgroundShapeColor, (alphaFraction * 255).toInt())
        if (colorMap[".bg"] != alphaColor) colorMap[".bg"] = alphaColor
    }

    fun randomSquare(shapeSize: Int): Drawable {
        return when (shapeSize) {
            1 -> arrayOf1x1Shapes.random()
            2 -> arrayOf2x2Shapes.random()
            3 -> arrayOf3x3Shapes.random()
            7 -> arrayOf7x7Shapes.first()
            else -> throw IllegalArgumentException("Unsupported square shape: $shapeSize")
        }
    }

    fun randomHorizontalBar(barWidth: Int): Drawable {
        return arrayOfHorizontalBarShapes[barWidth - 2]
    }

    fun randomHorizontalHalfCapsuleBar(barWidth: Int): Drawable {
        return arrayOfHorizontalHalfCapsuleBarShapes[barWidth - 2]
    }

    fun randomVerticalBar(barHeight: Int): Drawable {
        return arrayOfVerticalBarShapes[barHeight - 2]
    }

    fun getSemiCircle(): Drawable {
        return arrayOf1x1SemiCircleShapes.first()
    }

    fun nextFinderPatternCenter(): Drawable {
        val vectorDrawableArr = arrayOfFinderPatternCenterShapes
        val index = finderPatternCenterShapeIndex
        finderPatternCenterShapeIndex = index + 1
        return vectorDrawableArr[index % vectorDrawableArr.size]
    }

    fun drawShapeListOnCanvas(list: List<MaterialShapeRenderer>, canvas: Canvas, elapsedMs: Long) {
        list.forEach { it.draw(canvas, elapsedMs) }
    }

    fun randomRotationForSquareShape(): Int {
        return Random.nextInt() % 4
    }

    fun randomForegroundColor(width: Int, height: Int): Int {
        if (height == 1) {
            val probs = listOf(0.2f, 0.04f, 0.01f, 0.0f)
            val prob = probs.getOrNull(width - 1) ?: 0f
            if (Random.nextFloat() <= prob) return foregroundColorAccent
        }
        return mainForegroundColorArray.random()
    }

    fun randomBackgroundColor(): Int {
        return mainBackgroundColorArray.random()
    }

    fun calculateRatioToCenter(x: Int, y: Int, width: Int, height: Int): Float {
        val halfLineCount = qrcodeLineCount / 2.0f
        val dx = (x + (width / 2.0f)) - halfLineCount
        val dy = (y + (height / 2.0f)) - halfLineCount
        val distance = sqrt((dx * dx) + (dy * dy))
        val maxDistance = 1.414f * halfLineCount
        return distance / maxDistance
    }

    fun calculateStartDelay(x: Int, y: Int, width: Int, height: Int): Long {
        val base = (calculateRatioToCenter(x, y, width, height) * 1000f).toLong()
        val extra = if (width == 1 && height == 1) 0L else Random.Default.nextLong(400L)
        return base + extra
    }

    fun markAsCreated(x: Int, y: Int, width: Int, height: Int) {
        for (dx in 0 until width) {
            for (dy in 0 until height) {
                hasCreated[x + dx][y + dy] = true
            }
        }
    }

    fun isForeground(x: Int, y: Int): Boolean {
        return (qrcode?.matrix?.get(x, y)?.toInt()?.and(15)) == 1
    }

    suspend fun updateContent(
        content: String,
        errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.L
    ) {
        val encode = withContext(Dispatchers.Default) {
            Encoder.encode(content, errorCorrectionLevel, null)
        }
        qrcode = encode
        qrStartTime = System.currentTimeMillis()
        frameElapsed = 0L
        dataFrameElapsed = 0L
        lottieScale = 0f
        hasFinalDataImage = false
        drawAnimationBackground(0L)

        // 获取矩阵宽度
        qrcodeLineCount = qrcode?.matrix?.width ?: 0

        // 计算 moduleSize（模块尺寸），上限 29
        val displayMetrics = context.resources.displayMetrics
        val screenMin = min(displayMetrics.widthPixels, displayMetrics.heightPixels)
            .coerceAtMost(1200)
        val candidate =
            ceil(screenMin.toDouble() / qrcodeLineCount.toDouble()).toInt()
        moduleSize = min(candidate, 29)

        // 计算最终位图尺寸
        qrcodeSize = moduleSize * qrcodeLineCount

        hasCreated = Array(qrcodeLineCount) { BooleanArray(qrcodeLineCount) }

        // 清空渲染器列表
        finderPatternShapeList.clear()
        dataModuleShapeList.clear()
        backgroundModuleShapeList.clear()

        val colorForFinderPattern = getColorForFinderPattern()

        // 创建 3 个 finder patterns（左上、右上、左下）
        createRendererForShape(
            0, 0, 7, 7,
            finderPatternShapeList, randomSquare(7), colorForFinderPattern
        ) { renderer ->
            renderer.animationStyle = EntryAnimationStyle.EmphasizedZoomIn
            renderer.startDelay = 833
            renderer.duration = 834
        }
        createRendererForShape(
            qrcodeLineCount - 7, 0, 7, 7,
            finderPatternShapeList, randomSquare(7), colorForFinderPattern
        ) { renderer ->
            renderer.animationStyle = EntryAnimationStyle.EmphasizedZoomIn
            renderer.startDelay = 833
            renderer.duration = 834
        }
        createRendererForShape(
            0, qrcodeLineCount - 7, 7, 7,
            finderPatternShapeList, randomSquare(7), colorForFinderPattern
        ) { renderer ->
            renderer.animationStyle = EntryAnimationStyle.EmphasizedZoomIn
            renderer.startDelay = 833
            renderer.duration = 834
        }

        finderPatternCenterShapeIndex = 0

        // Finder pattern centers (3x3 centers inside finder)
        createRendererForShape(
            2, 2, 3, 3,
            finderPatternShapeList, nextFinderPatternCenter(), colorForFinderPattern
        ) { renderer ->
            renderer.animationStyle = EntryAnimationStyle.RotateEmphasizedZoomIn
            renderer.startDelay = 1167
            renderer.duration = 1667
        }
        createRendererForShape(
            qrcodeLineCount - 5, 2, 3, 3,
            finderPatternShapeList, nextFinderPatternCenter(), colorForFinderPattern
        ) { renderer ->
            renderer.animationStyle = EntryAnimationStyle.RotateEmphasizedZoomIn
            renderer.startDelay = 1167
            renderer.duration = 1667
        }
        createRendererForShape(
            2, qrcodeLineCount - 5, 3, 3,
            finderPatternShapeList, nextFinderPatternCenter(), colorForFinderPattern
        ) { renderer ->
            renderer.animationStyle = EntryAnimationStyle.RotateEmphasizedZoomIn
            renderer.startDelay = 1167
            renderer.duration = 1667
        }

        // 扫描并创建不同尺寸的前景/背景形状
        searchAndCreateLargeSquareShapes(3, dataModuleShapeList)
        searchAndCreateLargeSquareShapes(2, dataModuleShapeList)
        searchAndCreateHorizontalBars(4, dataModuleShapeList)
        searchAndCreateBars(3, dataModuleShapeList)
        searchAndCreateBars(2, dataModuleShapeList)
        searchAndCreateSmallForegroundSquareShapes(dataModuleShapeList)
        searchAndCreateSmallBackgroundSquareShapes(backgroundModuleShapeList)
    }


    fun createRendererForShape(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        list: MutableList<MaterialShapeRenderer>,
        vectorDrawable: Drawable,
        foregroundColor: Int,
        rendererConfigs: (MaterialShapeRenderer) -> Unit
    ) {
        val rectF = RectF(
            (x * moduleSize).toFloat(),
            (y * moduleSize).toFloat(),
            ((x + width) * moduleSize).toFloat(),
            ((y + height) * moduleSize).toFloat()
        )
        val paint = Paint()
        paint.colorFilter = PorterDuffColorFilter(foregroundColor, PorterDuff.Mode.SRC_IN)
        val materialShapeRenderer = MaterialShapeRenderer(vectorDrawable, rectF, paint)
        materialShapeRenderer.startDelay = calculateStartDelay(x, y, width, height)
        rendererConfigs(materialShapeRenderer)
        list.add(materialShapeRenderer)
        markAsCreated(x, y, width, height)
    }

    fun createRendererForShape(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        list: MutableList<MaterialShapeRenderer>,
        vectorDrawable: Drawable,
        rendererConfigs: (MaterialShapeRenderer) -> Unit
    ) {
        createRendererForShape(
            x = x,
            y = y,
            width = width,
            height = height,
            list = list,
            vectorDrawable = vectorDrawable,
            foregroundColor = randomForegroundColor(width, height),
            rendererConfigs = rendererConfigs
        )
    }

    fun createSingleVerticalBar(
        x: Int,
        y: Int,
        height: Int,
        list: MutableList<MaterialShapeRenderer>
    ) {
        createRendererForShape(
            x = x,
            y = y,
            width = 1,
            height = height,
            list = list,
            vectorDrawable = randomVerticalBar(height)
        ) { renderer: MaterialShapeRenderer ->
            renderer.animationStyle = EntryAnimationStyle.SpringZoomIn
            renderer.startDelay = calculateStartDelay(x, y, 1, height)
        }
    }

    fun createHorizontalBar(
        x: Int,
        y: Int,
        width: Int,
        list: MutableList<MaterialShapeRenderer>
    ) {
        if (width > 4) {
            throw IllegalArgumentException("barLen must be <= 4")
        }
        if (width <= 2 || (width == 3 && Random.Default.nextFloat() < 0.5f)) {
            createRendererForShape(
                x, y, width, 1, list, randomHorizontalBar(width)
            ) { renderer ->
                renderer.animationStyle = EntryAnimationStyle.SpringZoomIn
                renderer.startDelay = calculateStartDelay(x, y, width, 1)
            }
            return
        }
        if (Random.Default.nextFloat() > 0.5f) {
            createRendererForShape(
                x, y, 1, 1, list, getSemiCircle()
            ) { renderer ->
                renderer.animationStyle = EntryAnimationStyle.SpringZoomIn
                renderer.startDelay = calculateStartDelay(x, y, 1, 1)
                renderer.skipStartProgress = 0.3f
            }
            val longWidth = width - 1
            createRendererForShape(
                x + 1, y, longWidth, 1, list, randomHorizontalHalfCapsuleBar(longWidth)
            ) { renderer ->
                renderer.animationStyle = EntryAnimationStyle.SpringZoomIn
                renderer.startDelay = calculateStartDelay(x + 1, y, width - 1, 1)
            }
        } else {
            val longWidth = width - 1
            createRendererForShape(
                x, y, longWidth, 1, list, randomHorizontalHalfCapsuleBar(longWidth)
            ) { renderer ->
                renderer.initialRotation = 2
                renderer.animationStyle = EntryAnimationStyle.SpringZoomIn
                renderer.startDelay = calculateStartDelay(x, y, width - 1, 1)
            }
            createRendererForShape(
                (x + width) - 1, y, 1, 1, list, getSemiCircle(), randomForegroundColor(1, 1)
            ) { renderer ->
                renderer.initialRotation = 2
                renderer.animationStyle = EntryAnimationStyle.SpringZoomIn
                renderer.startDelay = calculateStartDelay((x + y) - 1, width, 1, 1)
                renderer.skipStartProgress = 0.3f
            }
        }
    }

    fun tryFindingHorizontalBar(
        startX: Int,
        startY: Int,
        width: Int,
        list: MutableList<MaterialShapeRenderer>
    ) {
        if (width > 4) throw IllegalArgumentException("barLen must be <= 4")
        val endX = startX + width
        val n = qrcodeLineCount
        if (endX > n || startY + 1 > n) return
        for (k in 0 until width) {
            val x = startX + k
            if (hasCreated[x][startY] || !isForeground(x, startY)) return
        }
        createHorizontalBar(startX, startY, width, list)
    }

    fun tryFindingVerticalBar(
        startX: Int,
        startY: Int,
        height: Int,
        list: MutableList<MaterialShapeRenderer>
    ) {
        val endY = startY + height
        val n = qrcodeLineCount
        if (endY > n || startX + 1 > n) return

        for (dy in 0 until height) {
            val y = startY + dy
            if (!isForeground(startX, y) || hasCreated[startX][y]) return
        }

        createSingleVerticalBar(startX, startY, height, list)
    }

    fun searchAndCreateSmallBackgroundSquareShapes(list: MutableList<MaterialShapeRenderer>) {
        for (y in 0 until qrcodeLineCount) {
            for (x in 0 until qrcodeLineCount) {
                // 如果已经创建或是前景则跳过；否则为背景模块创建 1x1 方形渲染器
                if (!hasCreated[x][y] && !isForeground(x, y)) {
                    createRendererForShape(
                        x, y, 1, 1, list, randomSquare(1)
                    ) { renderer ->
                        renderer.startDelay = calculateStartDelay(x, y, 1, 1)
                        renderer.animationStyle = EntryAnimationStyle.EmphasizedZoomIn
                        renderer.startDelay =
                            83 + (1250 * calculateRatioToCenter(x, y, 1, 1)).toLong()
                        renderer.skipStartProgress = 0.3f
                        val randomBackgroundColor = randomBackgroundColor()
                        val paint = Paint()
                        paint.colorFilter =
                            PorterDuffColorFilter(randomBackgroundColor, PorterDuff.Mode.SRC_IN)
                        renderer.paint = paint
                    }
                }
            }
        }
    }

    fun searchAndCreateSmallForegroundSquareShapes(list: MutableList<MaterialShapeRenderer>) {
        for (y in 0 until qrcodeLineCount) {
            for (x in 0 until qrcodeLineCount) {
                if (!hasCreated[x][y] && isForeground(x, y)) {
                    createRendererForShape(
                        x, y, 1, 1, list, randomSquare(1)
                    ) { renderer ->
                        renderer.animationStyle = EntryAnimationStyle.ZoomIn
                        renderer.startDelay = calculateStartDelay(x, y, 1, 1)
                        renderer.skipStartProgress = 0.3f
                    }
                }
            }
        }
    }

    fun searchAndCreateLargeSquareShapes(
        len: Int,
        list: MutableList<MaterialShapeRenderer>
    ) {
        val max = (qrcodeLineCount - len) + 1
        for (y in 0 until max) {
            for (x in 0 until max) {
                if (!hasCreated[x][y] && isForeground(x, y)) {
                    var ok = true
                    for (dy in 0 until len) {
                        for (dx in 0 until len) {
                            val nx = x + dx
                            val ny = y + dy
                            if (hasCreated[nx][ny] || !isForeground(nx, ny)) {
                                ok = false
                                break
                            }
                        }
                        if (!ok) break
                    }
                    if (ok) {
                        createRendererForShape(
                            x, y, len, len, list, randomSquare(len)
                        ) { renderer ->
                            renderer.animationStyle = EntryAnimationStyle.SpringZoomIn
                            renderer.startDelay = calculateStartDelay(x, y, len, len)
                            renderer.initialRotation = randomRotationForSquareShape()
                        }
                    }
                }
            }
        }
    }

    fun searchAndCreateBars(len: Int, list: MutableList<MaterialShapeRenderer>) {
        val funcs: List<(Int, Int, Int, MutableList<MaterialShapeRenderer>) -> Unit> =
            listOf(
                { x, y, l, lst -> tryFindingHorizontalBar(x, y, l, lst) },
                { x, y, l, lst -> tryFindingVerticalBar(x, y, l, lst) }
            )
        val reversed = funcs.reversed()
        val n = qrcodeLineCount
        for (y in 0 until n) {
            for (x in 0 until n) {
                if (!hasCreated[x][y] && isForeground(x, y)) {
                    val order = if (Random.Default.nextFloat() < 0.5f) funcs else reversed
                    for (f in order) f(x, y, len, list)
                }
            }
        }
    }

    fun searchAndCreateHorizontalBars(len: Int, list: MutableList<MaterialShapeRenderer>) {
        if (len > 4) throw IllegalArgumentException("barLen must be <= 4")
        for (y in 0 until qrcodeLineCount) {
            val maxX = (qrcodeLineCount - len) + 1
            for (x in 0 until maxX) {
                if (!hasCreated[x][y] && isForeground(x, y)) {
                    var ok = true
                    for (dx in 1 until len) {
                        val nx = x + dx
                        if (hasCreated[nx][y] || !isForeground(nx, y)) {
                            ok = false
                            break
                        }
                    }
                    if (ok) {
                        createHorizontalBar(x, y, len, list)
                    }
                }
            }
        }
    }
}

@Composable
internal fun rememberMaterialShapeQrState(
    context: Context = LocalContext.current,
    colorScheme: ColorScheme
): MaterialShapeQrState {
    val foregroundColorPrimary = colorScheme.primary.toArgb()
    val foregroundColorSecondary = colorScheme.secondary.toArgb()
    val foregroundColorAccent = colorScheme.tertiary.toArgb()

    val backgroundShapeColor = colorScheme.surfaceContainerHighest.toArgb()
    val backgroundDotColor1 = colorScheme.primary.copy(38 / 255f).toArgb()
    val backgroundDotColor2 = colorScheme.onSurface.copy(25 / 255f).toArgb()
    val backgroundDotColor3 = colorScheme.tertiaryContainer.copy(76 / 255f).toArgb()

    val mainForegroundColorArray = listOf(foregroundColorPrimary, foregroundColorSecondary)
    val mainBackgroundColorArray =
        listOf(backgroundDotColor1, backgroundDotColor2, backgroundDotColor3)

    val arrayOf1x1Shapes = remember { mutableStateListOf<Drawable>() }
    val arrayOf1x1SemiCircleShapes = remember { mutableStateListOf<Drawable>() }
    val arrayOf2x2Shapes = remember { mutableStateListOf<Drawable>() }
    val arrayOf3x3Shapes = remember { mutableStateListOf<Drawable>() }
    val arrayOf7x7Shapes = remember { mutableStateListOf<Drawable>() }

    val arrayOfHorizontalBarShapes = remember { mutableStateListOf<Drawable>() }
    val arrayOfHorizontalHalfCapsuleBarShapes = remember { mutableStateListOf<Drawable>() }
    val arrayOfVerticalBarShapes = remember { mutableStateListOf<Drawable>() }
    val arrayOfFinderPatternCenterShapes = remember { mutableStateListOf<Drawable>() }

    val colorMap = remember { mutableStateMapOf<String, Int>() }

    fun loadVectorDrawable(resId: Int): Drawable {
        return AppCompatResources.getDrawable(context, resId)!!
    }

    fun loadDrawables() {
        val s1Circle = loadVectorDrawable(R.drawable.qrcode_square_s1_circle)
        val s1Drop = loadVectorDrawable(R.drawable.qrcode_square_s1_drop)
        val s1SemiCircle = loadVectorDrawable(R.drawable.qrcode_square_s1_semi_circle)
        val s1Square = loadVectorDrawable(R.drawable.qrcode_square_s1_square)

        arrayOf1x1Shapes.addAll(listOf(s1Circle, s1Drop, s1Square))
        arrayOf1x1SemiCircleShapes.addAll(listOf(s1SemiCircle))

        arrayOf2x2Shapes.addAll(
            listOf(
                loadVectorDrawable(R.drawable.qrcode_square_s2_circle),
                loadVectorDrawable(R.drawable.qrcode_square_s2_clover),
                loadVectorDrawable(R.drawable.qrcode_square_s2_hexagonal),
                loadVectorDrawable(R.drawable.qrcode_square_s2_meteroid),
                loadVectorDrawable(R.drawable.qrcode_square_s2_wiggle_star)
            )
        )

        val s3Circle = loadVectorDrawable(R.drawable.qrcode_square_s3_circle)
        val s3Clover = loadVectorDrawable(R.drawable.qrcode_square_s3_clover)
        val s3Hexagonal = loadVectorDrawable(R.drawable.qrcode_square_s3_hexagonal)
        val s3Meteroid = loadVectorDrawable(R.drawable.qrcode_square_s3_meteroid)
        val s3WiggleStar = loadVectorDrawable(R.drawable.qrcode_square_s3_wiggle_star)

        arrayOf3x3Shapes.addAll(
            listOf(s3Circle, s3Clover, s3Hexagonal, s3Meteroid, s3WiggleStar)
        )
        arrayOfFinderPatternCenterShapes.addAll(
            listOf(s3Hexagonal, s3Meteroid, s3WiggleStar).shuffled()
        )

        arrayOf7x7Shapes.addAll(
            listOf(loadVectorDrawable(R.drawable.qrcode_square_s7_ring))
        )

        val horBarS2Capsule = loadVectorDrawable(R.drawable.qrcode_hor_bar_s2_capsule)
        val horBarS3Capsule = loadVectorDrawable(R.drawable.qrcode_hor_bar_s3_capsule)
        val horBarS2HalfCapsule = loadVectorDrawable(R.drawable.qrcode_hor_bar_s2_half_capsule)
        val horBarS3HalfCapsule = loadVectorDrawable(R.drawable.qrcode_hor_bar_s3_half_capsule)
        val verBarS2Capsule = loadVectorDrawable(R.drawable.qrcode_ver_bar_s2_capsule)
        val verBarS3Capsule = loadVectorDrawable(R.drawable.qrcode_ver_bar_s3_capsule)

        arrayOfHorizontalBarShapes.addAll(
            listOf(horBarS2Capsule, horBarS3Capsule)
        )
        arrayOfHorizontalHalfCapsuleBarShapes.addAll(
            listOf(horBarS2HalfCapsule, horBarS3HalfCapsule)
        )
        arrayOfVerticalBarShapes.addAll(
            listOf(verBarS2Capsule, verBarS3Capsule)
        )
    }

    fun applyLottieDynamicColor() {
        colorMap.clear()
        colorMap[".bg"] = backgroundShapeColor
        colorMap[".dot1"] = backgroundDotColor1
        colorMap[".dot2"] = backgroundDotColor2
        colorMap[".dot3"] = backgroundDotColor3
    }

    LaunchedEffect(Unit) {
        loadDrawables()
        applyLottieDynamicColor()
    }

    return remember(
        context,
    ) {
        MaterialShapeQrState(
            context = context,
            foregroundColorPrimary = foregroundColorPrimary,
            foregroundColorSecondary = foregroundColorSecondary,
            foregroundColorAccent = foregroundColorAccent,
            backgroundShapeColor = backgroundShapeColor,
            backgroundDotColor1 = backgroundDotColor1,
            backgroundDotColor2 = backgroundDotColor2,
            backgroundDotColor3 = backgroundDotColor3,
            mainForegroundColorArray = mainForegroundColorArray,
            mainBackgroundColorArray = mainBackgroundColorArray,
            arrayOf1x1Shapes = arrayOf1x1Shapes,
            arrayOf1x1SemiCircleShapes = arrayOf1x1SemiCircleShapes,
            arrayOf2x2Shapes = arrayOf2x2Shapes,
            arrayOf3x3Shapes = arrayOf3x3Shapes,
            arrayOf7x7Shapes = arrayOf7x7Shapes,
            arrayOfHorizontalBarShapes = arrayOfHorizontalBarShapes,
            arrayOfHorizontalHalfCapsuleBarShapes = arrayOfHorizontalHalfCapsuleBarShapes,
            arrayOfVerticalBarShapes = arrayOfVerticalBarShapes,
            arrayOfFinderPatternCenterShapes = arrayOfFinderPatternCenterShapes,
            colorMap = colorMap
        )
    }
}