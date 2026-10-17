package dev.aaa1115910.bv

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.webkit.WebViewCompat
import de.schnettler.datastore.manager.DataStoreManager
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.BiliHttpProxyApi
import dev.aaa1115910.biliapi.http.util.BiliAppConf
import dev.aaa1115910.biliapi.http.util.BiliWebConf
import dev.aaa1115910.biliapi.http.util.WebCookieManager
import dev.aaa1115910.biliapi.repositories.AuthRepository
import dev.aaa1115910.biliapi.repositories.BiliApiModule
import dev.aaa1115910.biliapi.repositories.ChannelRepository
import dev.aaa1115910.bv.dao.AppDatabase
import dev.aaa1115910.bv.entity.AuthData
import dev.aaa1115910.bv.entity.db.UserDB
import dev.aaa1115910.bv.network.HttpServer
import dev.aaa1115910.bv.player.PlayerHttpClient
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.util.BlacklistUtil
import dev.aaa1115910.bv.util.FirebaseUtil
import dev.aaa1115910.bv.util.LogCatcherUtil
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.KoinApplication
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.ksp.generated.module
import org.slf4j.impl.HandroidLoggerAdapter

class BVApp : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        lateinit var dataStoreManager: DataStoreManager
        lateinit var koinApplication: KoinApplication
        var instance: BVApp? = null

        fun getAppDatabase(context: Context = this.context) = AppDatabase.getDatabase(context)
    }

    override fun onCreate() {
        super.onCreate()
        context = this.applicationContext
        HandroidLoggerAdapter.DEBUG = BuildConfig.DEBUG
        dataStoreManager = DataStoreManager(applicationContext.dataStore)
        if (Prefs.blacklistUser) {
            R.string.blacklist_user_toast.toast(context)
            return
        }
        koinApplication = startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@BVApp)
            modules(AppModule().module)
        }
        initFirebase()
        LogCatcherUtil.installLogCatcher()
        initApiConfig()
        initRepository()
        initProxy()
        warmUpApi()
        instance = this
        updateMigration()
        HttpServer.startServer()
        updateBlacklist()
    }

    private fun initFirebase() {
        FirebaseUtil.init(applicationContext)
        when (BuildConfig.BUILD_TYPE) {
            "debug" -> {}
            else -> FirebaseUtil.setCrashlyticsCollectionEnabled(Prefs.enableFirebaseCollection)
        }
    }

    private fun initApiConfig() {
        BiliAppConf.osVersion = Build.VERSION.RELEASE
        BiliAppConf.model = Build.MODEL
        BiliWebConf.webViewVersion = runCatching {
            WebViewCompat.getCurrentLoadedWebViewPackage()!!.versionName!!
                .substringBefore(".").toInt()
        }.getOrDefault(144)
        initWebCookieManager()
    }

    private fun initWebCookieManager() {
        WebCookieManager.refreshTokenProvider = { Prefs.refreshToken }
        WebCookieManager.midProvider = { Prefs.uid }
        WebCookieManager.cookieGetter = { name ->
            // buvid3 在引入 Web Cookie 之前就由 Prefs 生成并用于请求头，
            // 首次运行时用它兜底，保证与 finger/spi 返回的 buvid4 能配对
            Prefs.getWebCookie(name) ?: if (name == "buvid3") Prefs.buvid3 else null
        }
        WebCookieManager.onCookiesUpdated = { updated ->
            // 账号级 cookie 交给 UserRepository 落盘，写进 webExtraCookies 会在登出后继续被注入
            Prefs.setWebCookies(updated.filterKeys { it !in UserRepository.AUTH_COOKIE_NAMES })
            // 请求头里用的 buvid3 必须与 buvid4 同源
            updated["buvid3"]?.takeIf { it.isNotBlank() }?.let { buvid3 ->
                Prefs.buvid3 = buvid3
                runCatching { koinApplication.koin.get<AuthRepository>().buvid3 = buvid3 }
            }
            val authUpdated = updated.filterKeys { it in UserRepository.AUTH_COOKIE_NAMES }
            if (authUpdated.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching {
                        koinApplication.koin.get<UserRepository>()
                            .applyWebCookieRefresh(authUpdated)
                    }
                }
            }
        }
    }

    fun initRepository() {
        val channelRepository by koinApplication.koin.inject<ChannelRepository>()
        channelRepository.initDefaultChannel(Prefs.accessToken, Prefs.buvid)

        val authRepository by koinApplication.koin.inject<AuthRepository>()
        authRepository.sessionData = Prefs.sessData.takeIf { it.isNotEmpty() }
        authRepository.biliJct = Prefs.biliJct.takeIf { it.isNotEmpty() }
        authRepository.dedeUserIDCkMd5 = Prefs.uidCkMd5.takeIf { it.isNotEmpty() }
        authRepository.sid = Prefs.sid.takeIf { it.isNotEmpty() }
        authRepository.accessToken = Prefs.accessToken.takeIf { it.isNotEmpty() }
        authRepository.mid = Prefs.uid.takeIf { it != 0L }
        authRepository.buvid3 = Prefs.buvid3
        authRepository.buvid = Prefs.buvid
        authRepository.bindWebCookieManager()
    }

    fun initProxy() {
        if (Prefs.enableProxy) {
            BiliHttpProxyApi.createClient(Prefs.proxyHttpServer)

            val channelRepository by koinApplication.koin.inject<ChannelRepository>()
            runCatching {
                channelRepository.initProxyChannel(
                    Prefs.accessToken,
                    Prefs.buvid,
                    Prefs.proxyGRPCServer
                )
            }
        }
    }

    private fun warmUpApi() {
        CoroutineScope(Dispatchers.IO).launch {
            // 无网络或接口报错时保持静默，不能影响启动；
            // cookie 维护涉及多次网络请求，单独并发，避免拖慢后面的预热
            launch { runCatching { WebCookieManager.initialize() } }
            BiliHttpApi.warmUpClient()
            PlayerHttpClient.preheat(this@BVApp)
        }
    }

    private fun updateMigration() {
        val lastVersionCode = Prefs.lastVersionCode
        if (lastVersionCode >= BuildConfig.VERSION_CODE) return
        Log.i("BVApp", "updateMigration from $lastVersionCode")
        if (lastVersionCode < 576) {
            // 从 Prefs 中读取登录数据写入 UserDB
            if (Prefs.isLogin) {
                runBlocking {
                    val existedUser = getAppDatabase().userDao().findUserByUid(Prefs.uid)
                    if (existedUser == null) {
                        val user = UserDB(
                            uid = Prefs.uid,
                            username = "Unknown",
                            avatar = "",
                            auth = AuthData.fromPrefs().toJson()
                        )
                        getAppDatabase().userDao().insert(user)
                    }
                }
            }
        }
        Prefs.lastVersionCode = BuildConfig.VERSION_CODE
    }

    private fun updateBlacklist() {
        CoroutineScope(Dispatchers.IO).launch {
            BlacklistUtil.updateBlacklist(context)
            BlacklistUtil.checkUid(Prefs.uid)
        }
    }
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "Settings")

@Module(includes = [BiliApiModule::class])
@ComponentScan
class AppModule
