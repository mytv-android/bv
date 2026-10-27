package com.github.mytv.bv.player

import android.content.Context
import okhttp3.OkHttpClient

/**
 * 播放器媒体请求共用的进程级 OkHttpClient。
 *
 * 构建时需读取系统 CA 库并追加 assets 证书，成本较高；且每个 OkHttpClient 自带独立连接池，
 * 按播放器实例创建会让连接池与线程数随播放轮次累积。这里进程内只构建一次。
 * UA / Referer 由各 ExoMediaPlayer 实例设置在各自的 OkHttpDataSource.Factory 上，不经过本 client。
 */
object PlayerHttpClient {
    @Volatile
    private var client: OkHttpClient? = null

    /** 获取共享 client；首次调用时构建，不要在主线程序列化触发首次构建。 */
    fun get(context: Context): OkHttpClient =
        client ?: synchronized(this) {
            client ?: OkHttpUtil.generateCustomSslOkHttpClient(context).also { client = it }
        }

    /** 在 IO 线程调用以预热，避开首次进播放器时的主线程构建等待。 */
    fun preheat(context: Context) {
        get(context)
    }
}
