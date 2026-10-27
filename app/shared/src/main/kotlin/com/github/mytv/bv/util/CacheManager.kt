package com.github.mytv.bv.util

import android.content.Context
import java.io.File

/**
 * 缓存管理器（缓存阈值自动清理）。
 *
 * 统一管理应用缓存目录（Coil 图片缓存 + 更新包下载等文件缓存）：
 * - 大小统计：递归计算全部缓存目录的总字节数。
 * - 阈值检查：App 启动 / 更新包写入后 / 阈值设置变更时调用 [checkCache]。
 * - LRU 清理：总大小超过阈值时，按文件最后修改时间从旧到新删除（近似 LRU），
 *   清理至阈值的 [CLEAN_TARGET_RATIO] 比例，避免刚清理又立即触发。
 * - 受 [Prefs.cacheAutoClean] 开关控制，关闭后不自动清理；阈值为 0（不限制）
 *   时也不会清理，因此默认配置不会删除任何文件。
 * - Coil 的磁盘缓存目录就是 cacheDir/image_cache，文件级 LRU 可直接覆盖；
 *   待安装的更新包通过 [checkCache] 的 preserve 参数保护。
 *
 * 本类无自身状态（状态都在磁盘与 Prefs 中），可按需创建实例；
 * 构造参数全部可注入，便于纯 JVM 单元测试。
 *
 * @param cacheDirs 参与统计与清理的缓存目录列表。
 * @param thresholdMb 缓存阈值（MB）提供者，0 表示不限制；每次检查时读取最新值。
 * @param autoCleanEnabled 自动清理开关提供者，每次检查时读取最新值。
 */
class CacheManager(
    private val cacheDirs: List<File>,
    private val thresholdMb: () -> Int = { Prefs.cacheThreshold },
    private val autoCleanEnabled: () -> Boolean = { Prefs.cacheAutoClean },
) {
    /**
     * 从 Android [Context] 构建缓存管理器。
     *
     * @param context 任意 Context（自动取 applicationContext.cacheDir 下的缓存目录）。
     */
    constructor(context: Context) : this(
        cacheDirs = listOf(
            File(context.cacheDir, IMAGE_CACHE_DIR),
            File(context.cacheDir, UPDATE_CACHE_DIR),
        ),
    )

    /** 当前缓存总大小（字节，全部缓存目录之和）。 */
    fun cacheSize(): Long = cacheDirs.sumOf { folderSize(it) }

    /** 手动清空全部缓存目录（删除目录内容，保留目录本身）。 */
    fun clearAll() {
        cacheDirs.forEach { dir ->
            dir.listFiles()?.forEach { runCatching { it.deleteRecursively() } }
        }
    }

    /**
     * 检查缓存：开启自动清理且总大小超过阈值时，清理至阈值的
     * [CLEAN_TARGET_RATIO] 比例。
     *
     * @param preserve 需要保留的文件（如刚下载完待安装的 APK），清理时跳过。
     * @return 本次清理删除的字节数，未触发清理返回 0。
     */
    fun checkCache(preserve: File? = null): Long {
        if (!autoCleanEnabled()) return 0L
        val thresholdBytes = thresholdMb().toLong() * BYTES_PER_MB
        if (thresholdBytes <= 0L) return 0L // 不限制
        if (cacheSize() <= thresholdBytes) return 0L
        return evictLru(
            cacheDirs,
            (thresholdBytes * CLEAN_TARGET_RATIO).toLong(),
            preserve,
        )
    }

    /**
     * LRU 清理核心：跨 [dirs] 删除最旧的文件，直到总大小不超过 [targetBytes]。
     *
     * 全部目录的文件合并后按 lastModified 升序删除（最旧优先），只删除文件
     * 不删除目录本身；[preserve] 指定的文件参与大小统计但不会被删除。
     *
     * @param dirs 目标目录列表，不存在的目录自动忽略。
     * @param targetBytes 目标总大小（字节）。
     * @param preserve 需要保留的文件，可为 null。
     * @return 实际删除的字节数。
     */
    internal fun evictLru(dirs: List<File>, targetBytes: Long, preserve: File? = null): Long {
        val files = dirs
            .filter { it.exists() }
            .flatMap { dir ->
                runCatching { dir.walkTopDown().filter { it.isFile }.toList() }
                    .getOrDefault(emptyList())
            }
        var total = files.sumOf { it.length() }
        if (total <= targetBytes) return 0L

        var freed = 0L
        for (file in files.sortedBy { it.lastModified() }) {
            if (total <= targetBytes) break
            if (preserve != null && file.absolutePath == preserve.absolutePath) continue
            val length = file.length()
            if (runCatching { file.delete() }.getOrDefault(false)) {
                total -= length
                freed += length
            }
        }
        return freed
    }

    companion object {
        /** 图片缓存目录名（与 Coil 2 默认磁盘缓存目录一致）。 */
        const val IMAGE_CACHE_DIR = "image_cache"

        /** 其他缓存目录名（更新包下载等）。 */
        const val UPDATE_CACHE_DIR = "update_downloader"

        /** 字节/MB 换算基数。 */
        const val BYTES_PER_MB: Long = 1024L * 1024L

        /** 阈值"不限制"标识（0）。 */
        const val THRESHOLD_UNLIMITED = 0

        /** 清理目标比例：清理至阈值的 80%，避免刚清理又立即触发。 */
        internal const val CLEAN_TARGET_RATIO = 0.8

        /**
         * 递归统计目录内所有文件的总大小（字节）。
         *
         * @param dir 目标目录，不存在时返回 0。
         */
        fun folderSize(dir: File): Long {
            if (!dir.exists()) return 0L
            return runCatching {
                dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }.getOrDefault(0L)
        }
    }
}
