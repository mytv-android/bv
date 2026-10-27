package com.github.mytv.bv.network.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Release(
    val assets: List<Asset>,
    @SerialName("assets_url")
    val assetsUrl: String,
    val author: User,
    val body: String,
    @SerialName("created_at")
    val createdAt: String,
    val draft: Boolean,
    @SerialName("html_url")
    val htmlUrl: String,
    val id: Int,
    val name: String,
    @SerialName("node_id")
    val nodeId: String,
    val prerelease: Boolean,
    @SerialName("published_at")
    val publishedAt: String,
    val reactions: Reactions? = null,
    @SerialName("tag_name")
    val tagName: String,
    @SerialName("tarball_url")
    val tarballUrl: String,
    @SerialName("target_commitish")
    val targetCommitish: String,
    @SerialName("upload_url")
    val uploadUrl: String,
    val url: String,
    @SerialName("zipball_url")
    val zipballUrl: String
) {
    val isPreRelease = prerelease
    val isRelease = !prerelease

    /** 迁移桥接版用 bridge- 前缀的 tag 标记；新版更新检查必须跳过它，避免装回旧包名。 */
    val isBridgeRelease: Boolean
        get() = tagName.startsWith("bridge-") || name.contains("桥接")

    /** 资产文件名形如 BV_<versionCode>_<versionName>.<buildType>_<flavor>_<abi>.apk。 */
    val revision: Int?
        get() = assets.mapNotNull { it.versionCode }.maxOrNull()
            ?: Regex("r(\\d+)").find(tagName)?.groupValues?.get(1)?.toIntOrNull()

    fun downloadAsset(isDebug: Boolean): Asset? =
        if (isDebug) assets.firstOrNull { it.name.contains(".debug_") }
        else assets.firstOrNull { it.name.contains(".alpha_") || it.name.contains(".release_") }

    @Serializable
    data class Asset(
        @SerialName("browser_download_url")
        val browserDownloadUrl: String,
        @SerialName("content_type")
        val contentType: String,
        @SerialName("created_at")
        val createdAt: String,
        @SerialName("download_count")
        val downloadCount: Int,
        val id: Int,
        val label: String? = null,
        val name: String,
        @SerialName("node_id")
        val nodeId: String,
        val size: Int,
        val state: String,
        @SerialName("updated_at")
        val updatedAt: String,
        val uploader: User,
        val url: String,
        val digest: String? = null,
    ) {
        val versionCode: Int?
            get() = Regex("^BV_(\\d+)_").find(name)?.groupValues?.get(1)?.toIntOrNull()

        /** GitHub 的 digest 形如 "sha256:<hex>"，旧资产或旧 API 没有这个字段。 */
        val sha256: String?
            get() = digest?.removePrefix("sha256:")?.lowercase()?.takeIf { it.length == 64 }
    }

    @Serializable
    data class User(
        @SerialName("avatar_url")
        val avatarUrl: String,
        @SerialName("events_url")
        val eventsUrl: String,
        @SerialName("followers_url")
        val followersUrl: String,
        @SerialName("following_url")
        val followingUrl: String,
        @SerialName("gists_url")
        val gistsUrl: String,
        @SerialName("gravatar_id")
        val gravatarId: String,
        @SerialName("html_url")
        val htmlUrl: String,
        val id: Int,
        val login: String,
        @SerialName("node_id")
        val nodeId: String,
        @SerialName("organizations_url")
        val organizationsUrl: String,
        @SerialName("received_events_url")
        val receivedEventsUrl: String,
        @SerialName("repos_url")
        val reposUrl: String,
        @SerialName("site_admin")
        val siteAdmin: Boolean,
        @SerialName("starred_url")
        val starredUrl: String,
        @SerialName("subscriptions_url")
        val subscriptionsUrl: String,
        val type: String,
        val url: String
    )

    @Serializable
    data class Reactions(
        @SerialName("+1")
        val like: Int,
        @SerialName("-1")
        val dislike: Int,
        val laugh: Int,
        val hooray: Int,
        val confused: Int,
        val heart: Int,
        val rocket: Int,
        val eyes: Int
    )
}
