import java.io.File

object AppConfiguration {
    private const val NEW_APP_ID = "com.github.mytv.bv"
    const val LEGACY_APP_ID = "dev.aaa1115910.bv"
    private const val LEGACY_APP_ID_ENV = "BV_LEGACY_APP_ID"
    private const val VERSION_CODE_OFFSET_ENV = "BV_VERSION_CODE_OFFSET"
    private const val MYBV_CERT_SHA256_ENV = "MYBV_CERT_SHA256"

    /** 代码命名空间始终是新包名，迁移桥接版只是把安装身份换回旧 applicationId。 */
    const val namespaceId = NEW_APP_ID
    val appId: String =
        if (System.getenv(LEGACY_APP_ID_ENV) == "true") LEGACY_APP_ID else NEW_APP_ID

    /** 新版 myBV 的签名证书 SHA-256；桥接版用于在双方签名不同的情况下校验调用方。 */
    val myBvCertSha256: String =
        System.getenv(MYBV_CERT_SHA256_ENV)?.trim()?.lowercase().orEmpty()

    const val compileSdk = 36
    const val minSdk = 23
    const val targetSdk = 36
    const val jdk = 21
    private const val major = 1
    private const val minor = 0
    private const val patch = 0
    private const val hotFix = 0

    @Suppress("KotlinConstantConditions")
    val versionName: String by lazy {
        "$major.$minor.$patch${".$hotFix".takeIf { hotFix != 0 } ?: ""}" +
                ".r${versionCode}.${"git rev-list HEAD --abbrev-commit --max-count=1".exec()}"
    }
    val versionCode: Int by lazy {
        "git rev-list --count HEAD".exec().toInt() +
                (System.getenv(VERSION_CODE_OFFSET_ENV)?.toIntOrNull() ?: 0)
    }
    const val libVLCVersion = "3.0.18"
    var googleServicesAvailable = true
    const val blacklistUrl =
        "https://raw.githubusercontent.com/aaa1115910/bv-blacklist/main/blacklist.bin"

    init {
        initConfigurations()
    }

    private fun initConfigurations() {
        val googleServicesJsonFile = File("app/google-services.json")
        googleServicesAvailable =
            googleServicesJsonFile.exists() && googleServicesJsonFile.readText().let {
                it.contains(appId) && it.contains("$appId.debug")
            }
        println("Google Services available: $googleServicesAvailable")
    }
}

fun String.exec() = String(Runtime.getRuntime().exec(this).inputStream.readBytes()).trim()
