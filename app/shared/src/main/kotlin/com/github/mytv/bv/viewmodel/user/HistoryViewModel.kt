package com.github.mytv.bv.viewmodel.user

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.user.HistoryItemType
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.repositories.HistoryRepository
import com.github.mytv.bv.BVApp
import com.github.mytv.bv.BuildConfig
import com.github.mytv.bv.R
import com.github.mytv.bv.entity.carddata.VideoCardData
import com.github.mytv.bv.repository.UserRepository
import com.github.mytv.bv.util.Prefs
import com.github.mytv.bv.util.addWithMainContext
import com.github.mytv.bv.util.fInfo
import com.github.mytv.bv.util.fWarn
import com.github.mytv.bv.util.formatHourMinSec
import com.github.mytv.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class HistoryViewModel(
    private val userRepository: UserRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var histories = mutableStateListOf<VideoCardData>()
    var noMore by mutableStateOf(false)

    private var cursor = 0L
    var updating by mutableStateOf(false)
    var deleting by mutableStateOf(false)
        private set

    fun update() {
        viewModelScope.launch(Dispatchers.IO) {
            updateHistories()
        }
    }

    private suspend fun updateHistories(context: Context = BVApp.context) {
        if (updating || noMore) return
        logger.fInfo { "Updating histories with params [cursor=$cursor, apiType=${Prefs.apiType}]" }
        updating = true
        runCatching {
            val data = historyRepository.getHistories(
                cursor = cursor,
                preferApiType = Prefs.apiType
            )

            data.data.forEach { historyItem ->
                histories.addWithMainContext(
                    VideoCardData(
                        avid = historyItem.oid,
                        title = historyItem.title,
                        cover = historyItem.cover,
                        upName = historyItem.author,
                        timeString = if (historyItem.progress == -1) context.getString(R.string.play_time_finish)
                        else context.getString(
                            R.string.play_time_history,
                            (historyItem.progress * 1000L).formatHourMinSec(),
                            (historyItem.duration * 1000L).formatHourMinSec()
                        ),
                        historyBusiness = when (historyItem.type) {
                            HistoryItemType.Archive -> "archive"
                            HistoryItemType.Pgc -> "pgc"
                            HistoryItemType.Unknown -> null
                        },
                        historyKid = historyItem.kid
                    )
                )
            }
            //update cursor
            cursor = data.cursor
            logger.fInfo { "Update history cursor: [cursor=$cursor]" }
            logger.fInfo { "Update histories success" }
            if (cursor == 0L) {
                withContext(Dispatchers.Main) { noMore = true }
                logger.fInfo { "No more history" }
            }
        }.onFailure {
            logger.fWarn { "Update histories failed: ${it.stackTraceToString()}" }
            when (it) {
                is AuthFailureException -> {
                    withContext(Dispatchers.Main) {
                        BVApp.context.getString(R.string.exception_auth_failure)
                            .toast(BVApp.context)
                    }
                    logger.fInfo { "User auth failure" }
                    if (!BuildConfig.DEBUG) userRepository.logout()
                }

                else -> {}
            }
        }
        updating = false
    }

    fun deleteHistory(business: String?, kid: Long?) {
        if (deleting || business == null || kid == null) return
        deleting = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                historyRepository.deleteHistory(
                    business = business,
                    kid = kid,
                    preferApiType = Prefs.apiType
                )
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    histories.removeAll { it.historyBusiness == business && it.historyKid == kid }
                }
                logger.fInfo { "Delete history success: business=$business, kid=$kid" }
                withContext(Dispatchers.Main) {
                    BVApp.context.getString(R.string.history_delete_success)
                        .toast(BVApp.context)
                }
            }.onFailure {
                logger.fWarn { "Delete history failed: ${it.stackTraceToString()}" }
                withContext(Dispatchers.Main) {
                    BVApp.context.getString(R.string.history_delete_failed)
                        .toast(BVApp.context)
                }
            }
            withContext(Dispatchers.Main) {
                deleting = false
            }
        }
    }

    fun clearHistory() {
        if (deleting) return
        deleting = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                historyRepository.clearHistory(
                    preferApiType = Prefs.apiType
                )
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    histories.clear()
                    cursor = 0L
                    noMore = false
                }
                logger.fInfo { "Clear history success" }
                withContext(Dispatchers.Main) {
                    BVApp.context.getString(R.string.history_clear_success)
                        .toast(BVApp.context)
                }
            }.onFailure {
                logger.fWarn { "Clear history failed: ${it.stackTraceToString()}" }
                withContext(Dispatchers.Main) {
                    BVApp.context.getString(R.string.history_clear_failed)
                        .toast(BVApp.context)
                }
            }
            withContext(Dispatchers.Main) {
                deleting = false
            }
        }
    }
}