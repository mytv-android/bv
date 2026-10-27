@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(gradleLibs.plugins.android.application)
    alias(gradleLibs.plugins.compose.compiler)
    alias(gradleLibs.plugins.firebase.crashlytics)
    alias(gradleLibs.plugins.google.ksp)
    alias(gradleLibs.plugins.google.services) apply false
    alias(gradleLibs.plugins.kotlin.android)
    alias(gradleLibs.plugins.kotlin.serialization)
}

if (AppConfiguration.googleServicesAvailable) {
    apply(plugin = gradleLibs.plugins.google.services.get().pluginId)
}


val signingProp = file(project.rootProject.file("signing.properties"))
val signingProperties = Properties().apply {
    if (signingProp.exists()) FileInputStream(signingProp).use { load(it) }
}
// 同时兼容 bv 旧格式(keystore.path/keystore.pwd)与 myMusic 格式(storeFile/storePassword)
val signingKeystore = rootProject.file(
    (System.getenv("KEYSTORE")
        ?: signingProperties.getProperty("storeFile")
        ?: signingProperties.getProperty("keystore.path")
        ?: "keystore.jks").trim()
)
val signingConfigured = signingProp.exists() || signingKeystore.exists()

android {
    signingConfigs {
        if (signingConfigured) {
            create("key") {
                storeFile = signingKeystore
                storePassword = (System.getenv("KEYSTORE_PASSWORD")
                    ?: signingProperties.getProperty("storePassword")
                    ?: signingProperties.getProperty("keystore.pwd"))?.trim()
                keyAlias = (System.getenv("KEY_ALIAS")
                    ?: signingProperties.getProperty("keyAlias")
                    ?: signingProperties.getProperty("keystore.alias"))?.trim()
                keyPassword = (System.getenv("KEY_PASSWORD")
                    ?: signingProperties.getProperty("keyPassword")
                    ?: signingProperties.getProperty("keystore.alias_pwd"))?.trim()
            }
        }
    }

    namespace = AppConfiguration.namespaceId
    compileSdk = AppConfiguration.compileSdk

    defaultConfig {
        applicationId = AppConfiguration.appId
        minSdk = AppConfiguration.minSdk
        targetSdk = AppConfiguration.targetSdk
        versionCode = AppConfiguration.versionCode
        versionName = AppConfiguration.versionName
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    flavorDimensions.add("channel")

    productFlavors {
        create("lite") {
            dimension = "channel"
        }
        create("default") {
            dimension = "channel"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingConfigured) signingConfig = signingConfigs.getByName("key")
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = AppConfiguration.googleServicesAvailable
            }
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ".debug"
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
        create("r8Test") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ".r8test"
            if (signingConfigured) signingConfig = signingConfigs.getByName("key")
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
        create("alpha") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingConfigured) signingConfig = signingConfigs.getByName("key")
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = AppConfiguration.googleServicesAvailable
            }
        }
    }

    buildFeatures {
        compose = true
        //buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "**/*.proto"
        }

        if (gradle.startParameter.taskNames.find { it.startsWith("assembleLite") } != null) {
            jniLibs {
                val vlcLibs = listOf("libvlc", "libc++_shared", "libvlcjni")
                val abis = listOf("x86_64", "x86", "arm64-v8a", "armeabi-v7a")
                vlcLibs.forEach { vlcLibName -> abis.forEach { abi -> excludes.add("lib/$abi/$vlcLibName.so") } }
            }
        }
    }

    /*splits {
        if (gradle.startParameter.taskNames.find { it.startsWith("assembleDefault") } != null) {
            abi {
                isEnable = true
                reset()
                include("x86_64", "x86", "arm64-v8a", "armeabi-v7a")
                isUniversalApk = true
            }
        }
    }*/

    applicationVariants.configureEach {
        val variant = this
        outputs.configureEach {
            (this as ApkVariantOutputImpl).apply {
                val abi = this.filters.find { it.filterType == "ABI" }?.identifier ?: "universal"
                outputFileName =
                    "BV_${AppConfiguration.versionCode}_${AppConfiguration.versionName}.${variant.buildType.name}_${variant.flavorName}_$abi.apk"
                versionNameOverride =
                    "${variant.versionName}.${variant.buildType.name}"
            }
        }
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_build_reports")
    stabilityConfigurationFiles.addAll(
        layout.projectDirectory.file("compose_compiler_config.conf")
    )
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(AppConfiguration.jdk))
    }
}

dependencies {
    implementation(project(":app:ui"))
    // 安装 src/main/baselineProfiles/baseline-prof.txt 里编译出的 baseline profile
    implementation(androidx.profileinstaller)
}

tasks.withType<Test> {
    useJUnitPlatform()
}