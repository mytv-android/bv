package dev.aaa1115910.biliapi.http.entity

import kotlinx.serialization.Serializable

/**
 * @param code 0：成功 -101：账号未登录 -400：参数错误 -401：非法访问 -403：访问权限不足
 */
@Serializable
data class BiliResponse<T>(
    val code: Int,
    val message: String,
    val ttl: Int? = null,
    val data: T? = null,
    val result: T? = null
) {
    @Throws()
    fun getResponseData(): T {
        when (code) {
            0 -> {}
            -101 -> throw AuthFailureException(message)
            -352 -> throw RiskControlException(
                message,
                (data as? VVoucherHolder)?.vVoucher
                    ?: (result as? VVoucherHolder)?.vVoucher
            )
            else -> throw IllegalStateException(message)
        }
        check(data != null || result != null) { "response data and result are both null" }
        data?.let { return it }
        result?.let { return it }
        error("response data and result are both null, and code should not run here")
    }
}

/**
 * 携带风控 v_voucher 的响应实体。-352 风控响应的 data 里通常包含该字段，
 * 实体实现此接口后 [BiliResponse.getResponseData] 才能把凭证带进 [RiskControlException]。
 */
interface VVoucherHolder {
    val vVoucher: String?
}

@Serializable
data class BiliResponseWithoutData(
    val code: Int,
    val message: String,
    val ttl: Int
)

@Suppress("unused")
class AuthFailureException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

@Suppress("unused")
class RiskControlException : RuntimeException {
    /** 风控响应携带的 v_voucher；为 null 表示本次响应没有可用于申请验证的凭证 */
    val vVoucher: String?

    constructor() : super() {
        vVoucher = null
    }

    constructor(message: String?) : super(message) {
        vVoucher = null
    }

    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        vVoucher = null
    }

    constructor(cause: Throwable?) : super(cause) {
        vVoucher = null
    }

    constructor(message: String?, vVoucher: String?) : super(message) {
        this.vVoucher = vVoucher
    }
}