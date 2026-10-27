package com.github.mytv.bv.ui.feature.verify

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.geetest.sdk.GT3ConfigBean
import com.geetest.sdk.GT3ErrorBean
import com.geetest.sdk.GT3GeetestUtils
import com.geetest.sdk.GT3Listener
import com.github.mytv.bv.viewmodel.login.GeetestResult
import kotlinx.serialization.json.Json

/**
 * 触屏端 Geetest 风控验证宿主（极验 sensebot SDK，WebView 承载）。
 *
 * 与电视端 [GeetestTvVerifyDialog] 面向同一个 ViewModel 状态：
 * [show] 为 true 时拉起验证面板，结果回调 [onResult]，取消/失败回调 [onCancelled]。
 */
@Composable
fun GeetestVerifyDialogHost(
    show: Boolean,
    gt: String,
    challenge: String,
    onResult: (challenge: String, validate: String, seccode: String) -> Unit,
    onCancelled: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnCancelled by rememberUpdatedState(onCancelled)
    // 只给回调使用，不参与 Composition，无需 Snapshot state
    val utilsRef = remember { GeetestUtilsRef() }
    val gt3ConfigBean = remember { GT3ConfigBean() }

    DisposableEffect(context) {
        utilsRef.value = GT3GeetestUtils(context)
        gt3ConfigBean.apply {
            pattern = 1
            isCanceledOnTouchOutside = false
            lang = null
            timeout = 10000
            webviewTimeout = 10000
            corners = 24
            listener = object : GT3Listener() {
                override fun onReceiveCaptchaCode(p0: Int) {}
                override fun onStatistics(p0: String?) {}
                override fun onSuccess(p0: String?) {}
                override fun onButtonClick() {}

                override fun onClosed(p0: Int) {
                    currentOnCancelled()
                }

                override fun onFailed(p0: GT3ErrorBean?) {
                    currentOnCancelled()
                }

                override fun onDialogResult(result: String) {
                    runCatching {
                        val geetestResult = Json.decodeFromString<GeetestResult>(result)
                        utilsRef.value?.showSuccessDialog()
                        currentOnResult(
                            geetestResult.geetestChallenge,
                            geetestResult.geetestValidate,
                            geetestResult.geetestSeccode
                        )
                    }.onFailure {
                        utilsRef.value?.showFailedDialog()
                        currentOnCancelled()
                    }
                }
            }
        }
        utilsRef.value!!.init(gt3ConfigBean)

        onDispose {
            utilsRef.value?.destory()
        }
    }

    LaunchedEffect(show) {
        if (show) {
            utilsRef.value?.startCustomFlow()
            gt3ConfigBean.api1Json = org.json.JSONObject().apply {
                put("success", 1)
                put("gt", gt)
                put("challenge", challenge)
            }
            utilsRef.value?.getGeetest()
        }
    }
}

private class GeetestUtilsRef {
    var value: GT3GeetestUtils? = null
}
