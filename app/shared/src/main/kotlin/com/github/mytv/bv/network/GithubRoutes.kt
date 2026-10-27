package com.github.mytv.bv.network

/**
 * GitHub 直连在国内不稳定，沿用 mydv / myHotpot 的做法：
 * 内置一份加速镜像清单，直连优先，失败后按顺序降级。
 * 镜像以 `镜像前缀 + 原始 URL` 的形式转发，空串表示直连。
 */
internal object GithubRoutes {
    // 2026-10-17 实测：前三个 API 与 APK 下载都可用；
    // 后两个只转发下载，API 返回 403，留作下载兜底。
    private val MIRRORS = listOf(
        "",
        "https://gh-proxy.com/",
        "https://gh-proxy.org/",
        "https://gh.llkk.cc/",
        "https://ghproxy.net/",
        "https://ghfast.top/",
    )

    data class Candidate(val label: String, val mirror: String, val url: String)

    /** 上次成功的镜像排在第一位，其余保持清单顺序。 */
    fun candidates(url: String, preferredMirror: String?): List<Candidate> {
        val candidates = MIRRORS.map { mirror ->
            Candidate(
                label = if (mirror.isEmpty()) "直连" else mirror.substringAfter("//").trimEnd('/'),
                mirror = mirror,
                url = mirror + url,
            )
        }
        return candidates.sortedBy { if (it.mirror == preferredMirror.orEmpty()) 0 else 1 }
    }
}
