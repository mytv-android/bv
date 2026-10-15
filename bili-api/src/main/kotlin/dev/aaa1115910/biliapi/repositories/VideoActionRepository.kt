package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

/**
 * 视频操作的统一入口：点赞、投币、收藏、关注 UP 主、稍后再看，以及这些操作的状态查询。
 *
 * - 点赞 / 投币此前只有 [BiliHttpApi] 的裸接口，这里补上与其它仓库一致的鉴权处理（Web 登录 Cookie +
 *   bili_jct，App 端暂无对应实现，App 登录同样会保存 Cookie）。
 * - 收藏、关注、稍后再看直接委托给已有的 [FavoriteRepository]、[UserRepository]、[ToViewRepository]。
 *
 * 写操作失败时抛出异常（未登录为 [AuthFailureException]，其余为带服务端提示的 [IllegalStateException]），
 * 调用方用 runCatching 处理即可。
 */
@Single
class VideoActionRepository(
    private val authRepository: AuthRepository,
    private val favoriteRepository: FavoriteRepository,
    private val userRepository: UserRepository,
    private val toViewRepository: ToViewRepository
) {
    val isLoggedIn: Boolean
        get() = !authRepository.sessionData.isNullOrBlank() || !authRepository.accessToken.isNullOrBlank()

    private fun requireSessData(): String =
        authRepository.sessionData?.takeIf { it.isNotBlank() }
            ?: throw AuthFailureException("not logged in")

    private fun requireCsrf(): String =
        authRepository.biliJct?.takeIf { it.isNotBlank() }
            ?: throw AuthFailureException("not logged in")

    // ---------- 点赞 ----------

    /**
     * 点赞（[like] = true）或取消赞视频[aid]
     */
    suspend fun likeVideo(aid: Long, like: Boolean = true) {
        val (success, message) = BiliHttpApi.sendVideoLike(
            avid = aid,
            like = like,
            csrf = requireCsrf(),
            sessData = requireSessData()
        )
        check(success) { message }
    }

    /**
     * 是否已点赞视频[aid]；未登录或请求失败时返回 false
     */
    suspend fun checkLiked(aid: Long): Boolean {
        val sessData = authRepository.sessionData?.takeIf { it.isNotBlank() } ?: return false
        return runCatching {
            BiliHttpApi.checkVideoLiked(avid = aid, sessData = sessData)
        }.getOrDefault(false)
    }

    // ---------- 投币 ----------

    /**
     * 为视频[aid]投 [multiply] 枚硬币（1 或 2），[alsoLike] 为 true 时同时点赞
     */
    suspend fun coinVideo(aid: Long, multiply: Int = 1, alsoLike: Boolean = false) {
        require(multiply == 1 || multiply == 2) { "multiply must be 1 or 2" }
        val (success, message) = BiliHttpApi.sendVideoCoin(
            avid = aid,
            multiply = multiply,
            like = alsoLike,
            csrf = requireCsrf(),
            sessData = requireSessData()
        )
        check(success) { message }
    }

    /**
     * 当前用户已为视频[aid]投出的硬币数（0、1、2）；未登录或请求失败时返回 0
     */
    suspend fun getSentCoinCount(aid: Long): Int {
        val sessData = authRepository.sessionData?.takeIf { it.isNotBlank() } ?: return 0
        return runCatching {
            BiliHttpApi.getVideoSentCoinCount(avid = aid, sessData = sessData)
        }.getOrDefault(0)
    }

    // ---------- 收藏 ----------

    /**
     * 视频[aid]是否在任一收藏夹中；未登录或请求失败时返回 false
     */
    suspend fun checkFavoured(aid: Long, preferApiType: ApiType = ApiType.Web): Boolean {
        if (!isLoggedIn) return false
        return runCatching {
            favoriteRepository.checkVideoFavoured(aid = aid, preferApiType = preferApiType)
        }.getOrDefault(false)
    }

    /**
     * 当前用户的全部收藏夹，[FavoriteFolderMetadata.videoInThisFav] 表示视频[aid]是否在该收藏夹中
     */
    suspend fun getFavoriteFolders(
        aid: Long,
        preferApiType: ApiType = ApiType.Web
    ): List<FavoriteFolderMetadata> {
        val mid = authRepository.mid?.takeIf { it != 0L }
            ?: throw AuthFailureException("not logged in")
        return favoriteRepository.getAllFavoriteFolderMetadataList(
            mid = mid,
            rid = aid,
            preferApiType = preferApiType
        )
    }

    /**
     * 把视频[aid]的收藏夹设为[selectedFolderIds]：加入选中的，从[allFolderIds]里其余的收藏夹移除
     * （与电视版 FavoriteButton 的逻辑相同）
     */
    suspend fun updateFavoriteFolders(
        aid: Long,
        selectedFolderIds: List<Long>,
        allFolderIds: List<Long>,
        preferApiType: ApiType = ApiType.Web
    ) {
        val delIds = allFolderIds - selectedFolderIds.toSet()
        if (selectedFolderIds.isEmpty() && delIds.isEmpty()) return
        favoriteRepository.updateVideoToFavoriteFolder(
            aid = aid,
            addMediaIds = selectedFolderIds,
            delMediaIds = delIds,
            preferApiType = preferApiType
        )
    }

    // ---------- 关注 UP 主 ----------

    /**
     * 是否已关注用户[mid]；未登录或请求失败时返回 null
     */
    suspend fun checkFollowing(mid: Long, preferApiType: ApiType = ApiType.Web): Boolean? =
        userRepository.checkIsFollowing(mid = mid, preferApiType = preferApiType)

    /**
     * 关注用户[mid]，返回是否成功
     */
    suspend fun followUser(mid: Long, preferApiType: ApiType = ApiType.Web): Boolean =
        userRepository.followUser(mid = mid, preferApiType = preferApiType)

    /**
     * 取消关注用户[mid]，返回是否成功
     */
    suspend fun unfollowUser(mid: Long, preferApiType: ApiType = ApiType.Web): Boolean =
        userRepository.unfollowUser(mid = mid, preferApiType = preferApiType)

    // ---------- 稍后再看 ----------

    suspend fun addToView(aid: Long) = toViewRepository.addToView(aid)

    suspend fun delToView(aid: Long) = toViewRepository.delToView(aid)

    // ---------- 汇总 ----------

    /**
     * 并行查询视频[aid]的点赞、投币、收藏状态，以及是否关注了 UP 主[upMid]（[upMid] 为 null 时不查）。
     * 未登录时直接返回默认状态，单项失败按未操作处理。
     */
    suspend fun getActionState(
        aid: Long,
        upMid: Long? = null,
        preferApiType: ApiType = ApiType.Web
    ): VideoActionState {
        if (!isLoggedIn) return VideoActionState()
        return withContext(Dispatchers.IO) {
            val liked = async { checkLiked(aid) }
            val coinCount = async { getSentCoinCount(aid) }
            val favoured = async { checkFavoured(aid, preferApiType) }
            val following = async {
                upMid?.takeIf { it > 0 }?.let { checkFollowing(it, preferApiType) }
            }
            VideoActionState(
                liked = liked.await(),
                coinCount = coinCount.await(),
                favoured = favoured.await(),
                following = following.await()
            )
        }
    }
}

/**
 * 视频操作状态
 *
 * @param coinCount 已投硬币数（0、1、2）
 * @param following 是否已关注 UP 主，null 表示未知（未登录、未查询或查询失败）
 */
data class VideoActionState(
    val liked: Boolean = false,
    val coinCount: Int = 0,
    val favoured: Boolean = false,
    val following: Boolean? = null
)
