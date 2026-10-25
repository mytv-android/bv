plugins {
    alias(gradleLibs.plugins.android.library)
    alias(gradleLibs.plugins.compose.compiler)
    alias(gradleLibs.plugins.google.ksp)
    alias(gradleLibs.plugins.google.services) apply false
    alias(gradleLibs.plugins.kotlin.android)
    alias(gradleLibs.plugins.kotlin.serialization)
}

android {
    namespace = AppConfiguration.appId + ".appui"
    compileSdk = AppConfiguration.compileSdk

    defaultConfig {
        minSdk = AppConfiguration.minSdk
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("r8Test") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("alpha") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    lint {
        targetSdk = AppConfiguration.targetSdk
    }

    testOptions {
        targetSdk = AppConfiguration.targetSdk
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(AppConfiguration.jdk))
    }
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.ui.ExperimentalComposeUiApi",
        )
    }
}

dependencies {
    implementation(project(":app:shared"))
    implementation(project(":player:core"))
    implementation(project(":player:shared"))
    implementation(project(":player:ui"))
    implementation(androidx.lifecycle.runtime.compose)
    implementation(androidx.media3.common)
    implementation(androidx.media3.exoplayer)
    implementation(libs.logging)
    testImplementation(libs.kotlin.test)
    debugImplementation(androidx.compose.ui.tooling)
}
