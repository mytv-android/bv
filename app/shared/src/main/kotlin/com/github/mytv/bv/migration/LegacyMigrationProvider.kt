package com.github.mytv.bv.migration

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Process
import com.github.mytv.bv.dao.AppDatabase
import java.io.File
import java.io.FileNotFoundException

/**
 * 旧版应用升级到桥接版后暴露的只读迁移入口：DataStore 与 Room 文件。
 *
 * 只允许签名一致的应用读取，避免登录 cookie 泄露给第三方。
 * 新版首次启动时通过 [LegacyDataMigrator] 拉取这些文件。
 */
class LegacyMigrationProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        requireSameSignature()
        if (method != LegacyMigrationContract.METHOD_INFO) return null
        val context = context ?: return null
        val databaseFile = context.getDatabasePath(LegacyMigrationContract.DATABASE_NAME)
        return Bundle().apply {
            putInt(LegacyMigrationContract.KEY_VERSION_CODE, packageVersionCode(context))
            putBoolean(
                LegacyMigrationContract.KEY_HAS_DATASTORE,
                File(context.filesDir, LegacyMigrationContract.DATASTORE_FILE).exists()
            )
            putBoolean(LegacyMigrationContract.KEY_HAS_DATABASE, databaseFile.exists())
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        requireSameSignature()
        val file = resolveAsset(uri) ?: throw FileNotFoundException("unknown migration asset: $uri")
        if (!file.exists()) throw FileNotFoundException(file.absolutePath)
        if (uri.lastPathSegment == LegacyMigrationContract.PATH_DATABASE) {
            checkpointDatabase()
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int = 0

    private fun resolveAsset(uri: Uri): File? {
        val context = context ?: return null
        val databaseFile = context.getDatabasePath(LegacyMigrationContract.DATABASE_NAME)
        return when (uri.lastPathSegment) {
            LegacyMigrationContract.PATH_DATASTORE ->
                File(context.filesDir, LegacyMigrationContract.DATASTORE_FILE)
            LegacyMigrationContract.PATH_DATABASE -> databaseFile
            LegacyMigrationContract.PATH_DATABASE_WAL -> File(databaseFile.path + "-wal")
            LegacyMigrationContract.PATH_DATABASE_SHM -> File(databaseFile.path + "-shm")
            else -> null
        }
    }

    /** 把 WAL 落盘后再让新版拷贝，避免只拿到主库文件、丢掉最近的写入。 */
    private fun checkpointDatabase() {
        val context = context ?: return
        if (!context.getDatabasePath(LegacyMigrationContract.DATABASE_NAME).exists()) return
        runCatching {
            AppDatabase.getDatabase(context).openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(FULL)")
                .close()
        }
    }

    private fun requireSameSignature() {
        val callerUid = Binder.getCallingUid()
        if (callerUid == Process.myUid()) return
        val packageManager = context?.packageManager ?: throw SecurityException("no context")
        if (packageManager.checkSignatures(callerUid, Process.myUid()) !=
            PackageManager.SIGNATURE_MATCH
        ) {
            throw SecurityException("migration provider is only available to same-signed apps")
        }
    }

    @Suppress("DEPRECATION")
    private fun packageVersionCode(context: Context): Int = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionCode
    }.getOrDefault(0)
}
