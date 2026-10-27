package com.github.mytv.bv.util

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics

object FirebaseUtil {
    private var initialized = false

    fun init(context: Context) {
        initialized = FirebaseApp.initializeApp(context) != null
    }

    fun log(msg: String) {
        if (!initialized) return
        Firebase.crashlytics.log(msg)
    }

    fun recordException(throwable: Throwable) {
        if (!initialized) return
        Firebase.crashlytics.recordException(throwable)
    }

    fun setCrashlyticsCollectionEnabled(enable: Boolean) {
        if (!initialized) return
        Firebase.crashlytics.setCrashlyticsCollectionEnabled(enable)
        Firebase.analytics.setAnalyticsCollectionEnabled(enable)
    }

}