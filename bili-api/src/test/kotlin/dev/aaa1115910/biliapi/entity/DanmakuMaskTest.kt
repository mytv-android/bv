package dev.aaa1115910.biliapi.entity

import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMask
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskType
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMobMaskFrame
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuWebMaskFrame
import java.io.File
import kotlin.test.Test

class DanmakuMaskTest {
    @Test
    fun `parse web mask file`() {
        val maskFile = Any::class::class.java.getResource("/3540266_25_2.exp.webmask")!!
        val binary = File(maskFile.toURI()).readBytes()
        val mask = DanmakuMask.fromBinary(binary, DanmakuMaskType.WebMask)
        println(mask)
    }

    @Test
    fun `parse web mask file and output`() {
        val maskFile = Any::class::class.java.getResource("/3540266_25_2.exp.webmask")!!
        val binary = File(maskFile.toURI()).readBytes()
        val mask = DanmakuMask.fromBinary(binary, DanmakuMaskType.WebMask)
        val outputDir = File("/home/seele/Documents/output/webmask")
        outputDir.mkdirs()
        mask.segments.forEachIndexed { index, danmakuMaskSegment ->
            val dir = File(outputDir, "$index")
            dir.mkdir()
            danmakuMaskSegment.frames.forEach { danmakuMaskFrame ->
                File(dir, "${danmakuMaskFrame.range}.svg")
                    .writeText((danmakuMaskFrame as DanmakuWebMaskFrame).svg)
            }
        }
    }

    @Test
    fun `parse mob mask file`() {
        val maskFile = Any::class::class.java.getResource("/3540266_25_2.exp.mobmask")!!
        val binary = File(maskFile.toURI()).readBytes()
        val mask = DanmakuMask.fromBinary(binary, DanmakuMaskType.MobMask)
        println()
    }

    @Test
    fun `mob mask frames carry 1bpp payload sized from width and height`() {
        val maskFile = Any::class::class.java.getResource("/3540266_25_2.exp.mobmask")!!
        val binary = File(maskFile.toURI()).readBytes()
        val mask = DanmakuMask.fromBinary(binary, DanmakuMaskType.MobMask)

        val frames = mask.segments.flatMap { it.frames }.filterIsInstance<DanmakuMobMaskFrame>()
        assert(frames.isNotEmpty()) { "sample mob mask should contain frames" }

        frames.forEach { frame ->
            // MobMask 是 1bpp 位图：每 8 个像素占 1 字节（向上取整）。
            // 解析时若按固定长度读取，遇到非 320x180 的蒙版就会让字节流错位。
            val expectedSize = (frame.width * frame.height + 7) / 8
            assert(frame.image.size == expectedSize) {
                "frame ${frame.range}: expected $expectedSize bytes for " +
                    "${frame.width}x${frame.height} 1bpp, got ${frame.image.size}"
            }
        }
    }

    @Test
    fun `web mask frames cover the end of every non-last segment`() {
        val maskFile = Any::class::class.java.getResource("/3540266_25_2.exp.webmask")!!
        val binary = File(maskFile.toURI()).readBytes()
        val mask = DanmakuMask.fromBinary(binary, DanmakuMaskType.WebMask)

        // 服务端每段通常少生成最后一帧，末帧结束时间比下一段起点早约一帧（~40ms）。
        // 若不把末帧延伸到本段末尾，消费端在段边界取不到蒙版帧，表现为每 ~10s 闪烁一次。
        mask.segments.dropLast(1).forEach { segment ->
            val lastFrame = segment.frames.last()
            assert(lastFrame.range.last >= segment.range.last) {
                "segment ${segment.range} has tail gap: " +
                    "last frame ${lastFrame.range} does not reach the segment end"
            }
        }
    }

    @Test
    fun `web mask last segment is not extended`() {
        val maskFile = Any::class::class.java.getResource("/3540266_25_2.exp.webmask")!!
        val binary = File(maskFile.toURI()).readBytes()
        val mask = DanmakuMask.fromBinary(binary, DanmakuMaskType.WebMask)

        // 末段没有下一段可衔接，延伸到段末尾（Long.MAX_VALUE - 1）会让蒙版在数据结束后一直残留
        val lastSegment = mask.segments.last()
        assert(lastSegment.frames.last().range.last < Long.MAX_VALUE - 1) {
            "last segment must not be extended to the segment end"
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `parse mob mask file and output`() {
        val maskFile = Any::class::class.java.getResource("/3540266_25_2.exp.mobmask")!!
        val binary = File(maskFile.toURI()).readBytes()
        val mask = DanmakuMask.fromBinary(binary, DanmakuMaskType.MobMask)
        val outputDir = File("/home/seele/Documents/output/mobmask")
        outputDir.mkdirs()
        mask.segments.forEachIndexed { index, danmakuMaskSegment ->
            val dir = File(outputDir, "$index")
            dir.mkdir()
            danmakuMaskSegment.frames.forEach { danmakuMaskFrame ->
                val content = (danmakuMaskFrame as DanmakuMobMaskFrame)
                    .image
                    .toHexString(HexFormat.Default)
                    .chunked(80)
                    .joinToString("\n")
                File(dir, "${danmakuMaskFrame.range}.txt").writeText(content)
            }
        }
    }
}