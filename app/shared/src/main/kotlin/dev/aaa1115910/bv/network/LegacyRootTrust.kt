package dev.aaa1115910.bv.network

import android.content.Context
import dev.aaa1115910.bv.R
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * 给 Android 8 以下的设备补上更新线路依赖的根证书。
 *
 * GitHub 与更新用的加速镜像都挂在 Let's Encrypt 的 ISRG 根上：老设备收到的是
 * 由 ISRG Root X1 交叉签名的 ISRG Root X2，Android 7 及更早的系统信任库两者都没有，
 * 表现为 “Trust anchor for certification path not found”，而同样的更新在新设备上正常。
 *
 * 内置证书与 API 34 的系统信任库一致，只在系统信任库拒绝时才被查询，
 * 新系统不会走到这里，也不会替换任何已有的根证书。
 */
internal object LegacyRootTrust {

    data class Factory(val socketFactory: SSLSocketFactory, val trustManager: X509TrustManager)

    fun open(context: Context): Factory? = runCatching {
        val delegates = listOfNotNull(systemTrustManager(), bundledTrustManager(context))
        check(delegates.isNotEmpty())
        val manager = FallbackTrustManager(delegates)
        Factory(
            SSLContext.getInstance("TLS").apply { init(null, arrayOf(manager), null) }.socketFactory,
            manager,
        )
    }.getOrNull()

    private fun systemTrustManager(): X509TrustManager? =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(null as KeyStore?) }
            .trustManagers
            .filterIsInstance<X509TrustManager>()
            .firstOrNull()

    private fun bundledTrustManager(context: Context): X509TrustManager? {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null as InputStream?, null)
        BUNDLED_ROOTS.forEachIndexed { index, resource ->
            val certificate = context.resources.openRawResource(resource).use { stream ->
                certificateFactory.generateCertificate(stream) as X509Certificate
            }
            keyStore.setCertificateEntry("legacy-root-$index", certificate)
        }
        return TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(keyStore) }
            .trustManagers
            .filterIsInstance<X509TrustManager>()
            .firstOrNull()
    }

    private val BUNDLED_ROOTS = intArrayOf(R.raw.isrg_root_x1, R.raw.isrg_root_x2)

    /**
     * 依次询问每个信任库，全部拒绝才失败；系统信任库的裁决始终优先，
     * 它接受的证书链不会走到内置根证书。
     */
    private class FallbackTrustManager(
        private val delegates: List<X509TrustManager>,
    ) : X509TrustManager {

        override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) =
            check { it.checkClientTrusted(chain, authType) }

        override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) =
            check { it.checkServerTrusted(chain, authType) }

        override fun getAcceptedIssuers(): Array<X509Certificate> =
            delegates.flatMap { it.acceptedIssuers.toList() }.toTypedArray()

        private inline fun check(block: (X509TrustManager) -> Unit) {
            var failure: CertificateException? = null
            for (delegate in delegates) {
                try {
                    block(delegate)
                    return
                } catch (exception: CertificateException) {
                    failure = exception
                }
            }
            throw failure ?: CertificateException("没有信任库接受该证书链")
        }
    }
}
