package com.github.mytv.bv.entity

import android.net.Uri

sealed class BvScheme(open val host: String) {
    companion object {
        private const val SCHEME = "bugvideo"
    }

    open fun buildUri(): String {
        return "bv://${host}"
    }

    data class QrToken(
        val uid: Long,
        val username: String,
        val avatar: String,
        val auth: String,
    ) : BvScheme(HOST) {
        companion object {
            const val HOST = "qrtoken"

            fun fromUri(uri: Uri): BvScheme? {
                return if (uri.host == HOST) {
                    val uid = uri.getQueryParameter("uid")?.toLongOrNull() ?: return null
                    val username = uri.getQueryParameter("username") ?: return null
                    val avatar = uri.getQueryParameter("avatar") ?: return null
                    val auth = uri.getQueryParameter("auth") ?: return null
                    QrToken(uid, username, avatar, auth)
                } else {
                    null
                }
            }
        }

        override fun buildUri(): String {
            val uri = Uri.Builder()
                .scheme(SCHEME)
                .authority(host)
                .appendQueryParameter("uid", uid.toString())
                .appendQueryParameter("username", username)
                .appendQueryParameter("avatar", avatar)
                .appendQueryParameter("auth", auth)
                .build()
            return uri.toString()
        }
    }
}

