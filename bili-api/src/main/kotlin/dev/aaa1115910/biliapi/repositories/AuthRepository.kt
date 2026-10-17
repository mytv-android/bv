package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.http.util.WebCookieManager
import org.koin.core.annotation.Single

@Single
class AuthRepository {
    var sessionData: String? = null
    var biliJct: String? = null
    var dedeUserIDCkMd5: String? = null
    var sid: String? = null
    var accessToken: String? = null
    var mid: Long? = null
    var buvid3: String? = null
    var buvid: String? = null

    /**
     * 把当前会话字段接到 [WebCookieManager]。refresh_token 与 cookie 的持久化
     * 属于应用层，仍由应用层另行设置。
     */
    fun bindWebCookieManager() {
        WebCookieManager.sessDataProvider = { sessionData.orEmpty() }
        WebCookieManager.biliJctProvider = { biliJct.orEmpty() }
        WebCookieManager.midProvider = { mid ?: 0L }
        WebCookieManager.uidCkMd5Provider = { dedeUserIDCkMd5.orEmpty() }
        WebCookieManager.sidProvider = { sid.orEmpty() }
        WebCookieManager.appBuvid3Provider = { buvid3 }
    }
}
