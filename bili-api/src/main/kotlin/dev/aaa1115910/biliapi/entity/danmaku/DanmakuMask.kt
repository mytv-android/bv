package dev.aaa1115910.biliapi.entity.danmaku

import io.ktor.util.decodeBase64String
import okio.ByteString.Companion.readByteString
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.zip.GZIPInputStream

data class DanmakuMaskSegment(
    val range: LongRange,
    val frames: List<DanmakuMaskFrame>,
)

sealed class DanmakuMaskFrame(
    open val range: LongRange
)

data class DanmakuWebMaskFrame(
    override val range: LongRange,
    val svg: String
) : DanmakuMaskFrame(range)

data class DanmakuMobMaskFrame(
    override val range: LongRange,
    val width: Int,
    val height: Int,
    val image: ByteArray
) : DanmakuMaskFrame(range) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DanmakuMobMaskFrame

        if (range != other.range) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = range.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + image.contentHashCode()
        return result
    }
}

/**
 * 返回结束时间延伸到 [end] 的帧副本，仅用于补齐段尾空洞（见 [DanmakuMask.fromBinary]）。
 */
private fun DanmakuMaskFrame.withEnd(end: Long): DanmakuMaskFrame =
    when (this) {
        is DanmakuWebMaskFrame -> copy(range = range.first until end)
        is DanmakuMobMaskFrame -> copy(range = range.first until end)
    }

data class DanmakuMask(
    val type: DanmakuMaskType,
    val segments: List<DanmakuMaskSegment>
) {
    companion object {
        fun fromBinary(binary: ByteArray, type: DanmakuMaskType): DanmakuMask {
            val times = mutableListOf<Long>()
            val offsets = mutableListOf<Long>()
            val danmakuMaskSegments = mutableListOf<DanmakuMaskSegment>()

            val inputStream = DataInputStream(ByteArrayInputStream(binary))
            val mask = inputStream.readByteString(4)
            require(mask.string(Charsets.UTF_8) == "MASK") { "Not a mask file" }
            val version = inputStream.readInt()
            val unused = inputStream.readByteString(4)
            val size = inputStream.readInt()

            for (i in 0 until size) {
                times.add(inputStream.readLong())
                offsets.add(inputStream.readLong())
            }

            var lastTime = 0L
            var segLastTime = 0L

            for (i in 0 until size) {
                val frameList = mutableListOf<DanmakuMaskFrame>()

                val bytes = if (i == size - 1) {
                    inputStream.readBytes()
                } else {
                    val offDiff = (offsets[i + 1] - offsets[i]).toInt()
                    val byteArray = ByteArray(offDiff)
                    inputStream.read(byteArray)
                    byteArray
                }

                val stream =
                    DataInputStream(ByteArrayInputStream(GZIPInputStream(bytes.inputStream()).readBytes()))
                while (stream.available() != 0) {
                    when (type) {
                        DanmakuMaskType.WebMask -> {
                            val svgLength = stream.readInt()
                            val time = stream.readLong()
                            val svg = stream.readByteString(svgLength).string(Charsets.UTF_8)

                            val decodedSvg = svg.split(",")[1]
                                .replace("\n", "")
                                .decodeBase64String()
                            frameList.add(
                                DanmakuWebMaskFrame(
                                    range = lastTime..<time,
                                    svg = decodedSvg
                                )
                            )
                            lastTime = time
                        }

                        DanmakuMaskType.MobMask -> {
                            val width = stream.readShort().toInt()
                            val height = stream.readShort().toInt()
                            val time = stream.readLong()
                            // MobMask 是 1bpp 位图，按实际宽高计算字节数；
                            // 原先硬编码 7200 会让流错位（小蒙版少读、大蒙版残留字节污染后续帧）
                            val imageSize = (width * height + 7) / 8
                            val imageBinary = ByteArray(imageSize)
                            stream.read(imageBinary)
                            frameList.add(
                                DanmakuMobMaskFrame(
                                    range = lastTime..<time,
                                    width = width,
                                    height = height,
                                    image = imageBinary
                                )
                            )
                            lastTime = time
                        }
                    }
                }

                val startTime = segLastTime
                val endTime = if (i == size - 1) Long.MAX_VALUE else times[i + 1]

                // 关闭段尾空洞：每段约 10s，末帧结束时间通常比下一段起点早约一帧（~40ms），
                // 消费端在该空洞内取不到蒙版帧，表现为每段边界周期性闪一下。
                // 末段（endTime 为 Long.MAX_VALUE）不延伸，避免蒙版在数据结束后一直残留。
                if (i != size - 1 && frameList.isNotEmpty()) {
                    val lastFrame = frameList.last()
                    frameList[frameList.lastIndex] =
                        lastFrame.withEnd(maxOf(lastFrame.range.last + 1, endTime))
                }

                danmakuMaskSegments.add(
                    DanmakuMaskSegment(range = startTime..<endTime, frames = frameList)
                )
                segLastTime = endTime
            }

            return DanmakuMask(type, danmakuMaskSegments)
        }
    }
}

enum class DanmakuMaskType {
    WebMask, MobMask
}