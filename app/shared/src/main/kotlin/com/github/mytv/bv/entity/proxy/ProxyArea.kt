package com.github.mytv.bv.entity.proxy

import com.github.mytv.bv.util.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging

enum class ProxyArea {
    MainLand, HongKong, TaiWan;

    companion object {
        private val logger = KotlinLogging.logger { }
        fun checkProxyArea(title: String): ProxyArea {
            val enableProxy = Prefs.enableProxy
            val proxyArea = when {
                !enableProxy -> MainLand
                title.contains(Regex("僅.*港")) -> HongKong
                title.contains(Regex("僅.*台")) -> TaiWan
                else -> MainLand
            }
            if (enableProxy) logger.debug { "Check proxy area: $title->$proxyArea" }
            return proxyArea
        }
    }
}