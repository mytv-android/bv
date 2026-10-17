package dev.aaa1115910.biliapi.repositories

import bilibili.app.interfaces.v1.HistoryGrpcKt
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.user.ToViewData
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.http.entity.BiliResponseWithoutData
import org.koin.core.annotation.Single

@Single
class ToViewRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository
) {
    private val historyStub
        get() = runCatching {
            HistoryGrpcKt.HistoryCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    suspend fun getToView(
        cursor: Long,
        preferApiType: ApiType = ApiType.Web
    ): ToViewData {
        return when (preferApiType) {
            ApiType.Web -> {
                val data = BiliHttpApi.getToView(
                    // viewAt = cursor,
                    sessData = authRepository.sessionData!!,
                ).getResponseData()
                print(data)
                ToViewData.fromToViewResponse(data)
            }

            ApiType.App -> {
                val data = BiliHttpApi.getToView(
                    // viewAt = cursor,
                    sessData = authRepository.sessionData!!,
                ).getResponseData()
                ToViewData.fromToViewResponse(data)
            }
        }
    }

    /**
     * 添加视频[aid]到稍后再看（Web 登录 Cookie）。失败时抛出异常，消息为服务端提示（例如列表已满）
     */
    suspend fun addToView(aid: Long) {
        val sessData = authRepository.sessionData?.takeIf { it.isNotBlank() }
        val csrf = authRepository.biliJct?.takeIf { it.isNotBlank() }
        if (sessData == null || csrf == null) throw AuthFailureException("not logged in")
        BiliHttpApi.addToView(
            avid = aid,
            csrf = csrf,
            sessData = sessData
        ).checkSuccess()
    }

    /**
     * 从稍后再看删除视频[aid]（Web 登录 Cookie）。失败时抛出异常，消息为服务端提示
     */
    suspend fun delToView(aid: Long) {
        val sessData = authRepository.sessionData?.takeIf { it.isNotBlank() }
        val csrf = authRepository.biliJct?.takeIf { it.isNotBlank() }
        if (sessData == null || csrf == null) throw AuthFailureException("not logged in")
        BiliHttpApi.delToView(
            avid = aid,
            csrf = csrf,
            sessData = sessData
        ).checkSuccess()
    }

    /**
     * 清空稍后再看（Web 登录 Cookie）。失败时抛出异常，消息为服务端提示
     */
    suspend fun clearToView(
        preferApiType: ApiType = ApiType.Web
    ) {
        val sessData = authRepository.sessionData?.takeIf { it.isNotBlank() }
        val csrf = authRepository.biliJct?.takeIf { it.isNotBlank() }
        if (sessData == null || csrf == null) throw AuthFailureException("not logged in")
        BiliHttpApi.clearToView(
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
