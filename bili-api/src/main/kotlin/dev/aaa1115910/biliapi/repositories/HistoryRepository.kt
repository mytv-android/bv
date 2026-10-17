package dev.aaa1115910.biliapi.repositories

import bilibili.app.interfaces.v1.HistoryGrpcKt
import bilibili.app.interfaces.v1.cursor
import bilibili.app.interfaces.v1.cursorV2Req
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.user.HistoryData
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.http.entity.BiliResponseWithoutData
import org.koin.core.annotation.Single

@Single
class HistoryRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository
) {
    private val historyStub
        get() = runCatching {
            HistoryGrpcKt.HistoryCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    suspend fun getHistories(
        cursor: Long,
        preferApiType: ApiType = ApiType.Web
    ): HistoryData {
        return when (preferApiType) {
            ApiType.Web -> {
                val data = BiliHttpApi.getHistories(
                    viewAt = cursor,
                    sessData = authRepository.sessionData!!,
                ).getResponseData()
                HistoryData.fromHistoryResponse(data)
            }

            ApiType.App -> {
                val reply = historyStub?.cursorV2(cursorV2Req {
                    this.cursor = cursor {
                        max = cursor
                    }
                    business = "archive"
                })
                HistoryData.fromHistoryResponse(reply!!)
            }
        }
    }

    /**
     * 删除单条历史记录（Web 登录 Cookie）。[kid] 为列表项的目标 id，[business] 为业务类型（archive / pgc）。
     * 失败时抛出异常，消息为服务端提示
     */
    suspend fun deleteHistory(
        business: String,
        kid: Long,
        preferApiType: ApiType = ApiType.Web
    ) {
        val sessData = authRepository.sessionData?.takeIf { it.isNotBlank() }
        val csrf = authRepository.biliJct?.takeIf { it.isNotBlank() }
        if (sessData == null || csrf == null) throw AuthFailureException("not logged in")
        BiliHttpApi.deleteHistory(
            kid = "${business}_$kid",
            csrf = csrf,
            sessData = sessData
        ).checkSuccess()
    }

    /**
     * 清空历史记录（Web 登录 Cookie）。失败时抛出异常，消息为服务端提示
     */
    suspend fun clearHistory(
        preferApiType: ApiType = ApiType.Web
    ) {
        val sessData = authRepository.sessionData?.takeIf { it.isNotBlank() }
        val csrf = authRepository.biliJct?.takeIf { it.isNotBlank() }
        if (sessData == null || csrf == null) throw AuthFailureException("not logged in")
        BiliHttpApi.clearHistory(
            csrf = csrf,
            sessData = sessData
        ).checkSuccess()
    }

    private fun BiliResponseWithoutData.checkSuccess() {
        when (code) {
            0 -> {}
            -101 -> throw AuthFailureException(message)
            else -> throw IllegalStateException(message)
        }
    }
}
