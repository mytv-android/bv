package com.github.mytv.bv.viewmodel.user

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.biliapi.entity.FavoriteItemType
import dev.aaa1115910.biliapi.repositories.FavoriteRepository
import com.github.mytv.bv.BVApp
import com.github.mytv.bv.R
import com.github.mytv.bv.entity.carddata.VideoCardData
import com.github.mytv.bv.util.Prefs
import com.github.mytv.bv.util.addWithMainContext
import com.github.mytv.bv.util.fInfo
import com.github.mytv.bv.util.fWarn
import com.github.mytv.bv.util.toast
import com.github.mytv.bv.util.swapList
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class FavoriteViewModel(
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var favoriteFolderMetadataList = mutableStateListOf<FavoriteFolderMetadata>()
    var favorites = mutableStateListOf<VideoCardData>()

    var currentFavoriteFolderMetadata: FavoriteFolderMetadata? by mutableStateOf(null)

    private var pageSize = 20
    private var pageNumber = 1
    private var hasMore = true

    var updatingFolders by mutableStateOf(false)
    var updatingFolderItems by mutableStateOf(false)
    var deleting by mutableStateOf(false)
        private set

    init {
        updateFoldersInfo()
    }

    private fun updateFoldersInfo() {
        if (updatingFolders) return
        updatingFolders = true
        logger.fInfo { "Updating favorite folders" }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val favoriteFolderMetadataList =
                    favoriteRepository.getAllFavoriteFolderMetadataList(
                        mid = Prefs.uid,
                        preferApiType = Prefs.apiType
                    )
                withContext(Dispatchers.Main) {
                    this@FavoriteViewModel.favoriteFolderMetadataList
                        .swapList(favoriteFolderMetadataList)
                    currentFavoriteFolderMetadata = favoriteFolderMetadataList.firstOrNull()
                }
                logger.fInfo { "Update favorite folders success: ${favoriteFolderMetadataList.map { it.id }}" }
            }.onFailure {
                logger.fWarn { "Update favorite folders failed: ${it.stackTraceToString()}" }
                //这里返回的数据并不会有用户认证失败的错误返回，没必要做身份验证失败提示
            }.onSuccess {
                updateFolderItems()
            }
            updatingFolders = false
        }
    }

    private var updateJob: Job? = null

    fun updateFolderItems(force: Boolean = false) {
        if (force) {
            updateJob?.cancel()
            resetPageNumber()
            updatingFolderItems = false
        }
        if (updatingFolderItems || !hasMore) return
        updatingFolderItems = true
        logger.fInfo { "Updating favorite folder items with media id: ${currentFavoriteFolderMetadata?.id}" }
        updateJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val favoriteFolderData = favoriteRepository.getFavoriteFolderData(
                    mediaId = currentFavoriteFolderMetadata!!.id,
                    pageSize = pageSize,
                    pageNumber = pageNumber,
                    preferApiType = Prefs.apiType
                )
                favoriteFolderData.medias.forEach { favoriteItem ->
                    if (favoriteItem.type != FavoriteItemType.Video) return@forEach
                    favorites.addWithMainContext(
                        VideoCardData(
                            avid = favoriteItem.id,
                            title = favoriteItem.title,
                            cover = favoriteItem.cover,
                            upName = favoriteItem.upper.name,
                            time = favoriteItem.duration * 1000L
                        )
                    )
                }
                hasMore = favoriteFolderData.hasMore
                logger.fInfo { "Update favorite items success" }
            }.onFailure {
                logger.fInfo { "Update favorite items failed: ${it.stackTraceToString()}" }
            }.onSuccess {
                pageNumber++
            }
            updatingFolderItems = false
        }
    }

    fun resetPageNumber() {
        pageNumber = 1
        hasMore = true
    }

    /** 把视频[item]从当前收藏夹中移除（取消收藏）。成功后立即从列表里删掉 */
    fun deleteFavorite(item: VideoCardData) {
        if (deleting) return
        val folderId = currentFavoriteFolderMetadata?.id ?: return
        deleting = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                favoriteRepository.delVideoFromFavoriteFolder(
                    aid = item.avid,
                    delMediaIds = listOf(folderId),
                    preferApiType = Prefs.apiType
                )
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    favorites.removeAll { it.avid == item.avid }
                }
                logger.fInfo { "Delete favorite success: avid=${item.avid}, folderId=$folderId" }
                withContext(Dispatchers.Main) {
                    BVApp.context.getString(R.string.favorite_delete_success)
                        .toast(BVApp.context)
                }
            }.onFailure {
                logger.fWarn { "Delete favorite failed: ${it.stackTraceToString()}" }
                withContext(Dispatchers.Main) {
                    BVApp.context.getString(R.string.favorite_delete_failed)
                        .toast(BVApp.context)
                }
            }
            withContext(Dispatchers.Main) {
                deleting = false
            }
        }
    }
}