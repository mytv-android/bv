package com.github.mytv.bv.migration

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.github.mytv.bv.dataStore
import com.github.mytv.bv.util.Prefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * 新版 myBV 从仍安装着的旧版 BV(applicationId=dev.aaa1115910.bv) 迁移：
 * - Room 数据库(搜索历史/用户列表)在主库尚未创建时整体拷贝；
 * - DataStore(设置/登录 cookie/refresh token)按 key 合并，新版已有的值优先。
 *
 * 旧版必须是带 [LegacyMigrationProvider] 的桥接版。新版首次进入且没有本地数据时，
 * UI 通过 [shouldPromptMigration] 弹出迁移确认，用户确认后再调用 [migrate]。
 */
object LegacyDataMigrator {
    private const val TAG = "LegacyDataMigrator"
    private const val MARKER_FILE = "legacy_migration_v1.done"
    private const val DB_CHECK_MARKER = "legacy_db_checked_v1"

    /**
     * 在 Koin/UI 打开 Room 之前静默拷贝旧数据库：搜索历史和账号列表先就位，
     * 设置与登录态仍然等用户在迁移提示里确认后再合并。
     */
    fun prepareDatabase(context: Context) {
        if (context.packageName != LegacyMigrationContract.NEW_APP_ID) return
        val appContext = context.applicationContext
        if (File(appContext.filesDir, MARKER_FILE).exists()) return
        if (File(appContext.filesDir, DB_CHECK_MARKER).exists()) return
        if (appContext.getDatabasePath(LegacyMigrationContract.DATABASE_NAME).exists()) return
        if (!isLegacyInstalled(appContext)) return
        val info = queryInfo(appContext) ?: return
        if (!info.getBoolean(LegacyMigrationContract.KEY_HAS_DATABASE, false)) {
            runCatching { File(appContext.filesDir, DB_CHECK_MARKER).createNewFile() }
            return
        }
        if (copyDatabase(appContext, legacyAuthority())) {
            runCatching { File(appContext.filesDir, DB_CHECK_MARKER).createNewFile() }
        }
    }

    /** 未登录且桥接版仍安装时提示迁移。 */
    fun shouldPromptMigration(context: Context): Boolean {
        if (context.packageName != LegacyMigrationContract.NEW_APP_ID) return false
        if (File(context.filesDir, MARKER_FILE).exists()) return false
        if (!isLegacyInstalled(context)) return false
        return !Prefs.isLogin
    }

    /** 执行迁移；成功或本轮不需要迁移时返回 true，桥接版不可用等失败情况返回 false。 */
    fun migrate(context: Context): Boolean {
        if (context.packageName != LegacyMigrationContract.NEW_APP_ID) return false
        val appContext = context.applicationContext
        val marker = File(appContext.filesDir, MARKER_FILE)
        if (marker.exists()) return true
        if (!isLegacyInstalled(appContext)) return false

        val authority = legacyAuthority()
        val info = queryInfo(appContext) ?: return false

        val hasDatabase = info.getBoolean(LegacyMigrationContract.KEY_HAS_DATABASE, false)
        val hasDataStore = info.getBoolean(LegacyMigrationContract.KEY_HAS_DATASTORE, false)
        if (!hasDatabase && !hasDataStore) return false

        var success = true
        if (hasDatabase) success = copyDatabase(appContext, authority) && success
        if (hasDataStore) success = mergePreferences(appContext, authority) && success
        if (!success) return false

        runCatching { marker.createNewFile() }
        Log.i(TAG, "legacy data migrated")
        return true
    }

    private fun legacyAuthority(): Uri =
        Uri.parse("content://${LegacyMigrationContract.LEGACY_AUTHORITY}")

    private fun queryInfo(context: Context): Bundle? = runCatching {
        context.contentResolver.call(
            legacyAuthority(),
            LegacyMigrationContract.METHOD_INFO,
            null,
            null,
        )
    }.onFailure {
        Log.i(TAG, "legacy migration provider unavailable: ${it.message}")
    }.getOrNull()

    private fun isLegacyInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo(LegacyMigrationContract.LEGACY_APP_ID, 0)
    }.isSuccess

    private fun copyDatabase(context: Context, authority: Uri): Boolean {
        val databaseFile = context.getDatabasePath(LegacyMigrationContract.DATABASE_NAME)
        if (databaseFile.exists()) return true
        databaseFile.parentFile?.mkdirs()
        val walFile = File(databaseFile.path + "-wal")
        val shmFile = File(databaseFile.path + "-shm")

        val copied = runCatching {
            copyAsset(context, authority, LegacyMigrationContract.PATH_DATABASE, databaseFile)
            runCatching {
                copyAsset(context, authority, LegacyMigrationContract.PATH_DATABASE_WAL, walFile)
            }
            runCatching {
                copyAsset(context, authority, LegacyMigrationContract.PATH_DATABASE_SHM, shmFile)
            }
        }.isSuccess

        if (!copied || !databaseFile.exists() || databaseFile.length() == 0L) {
            databaseFile.delete()
            walFile.delete()
            shmFile.delete()
            Log.w(TAG, "copy legacy database failed")
            return false
        }
        return true
    }

    private fun mergePreferences(context: Context, authority: Uri): Boolean {
        val tempFile = File(context.cacheDir, "legacy_settings.preferences_pb")
        val copied = runCatching {
            val input = context.contentResolver.openInputStream(
                authority.buildUpon()
                    .appendPath(LegacyMigrationContract.PATH_DATASTORE)
                    .build()
            )
            if (input == null) {
                false
            } else {
                input.use { source ->
                    tempFile.outputStream().use { target -> source.copyTo(target) }
                }
                true
            }
        }.getOrDefault(false)
        if (!copied) {
            Log.w(TAG, "copy legacy preferences failed")
            return false
        }

        return try {
            val legacyStore = PreferenceDataStoreFactory.create(produceFile = { tempFile })
            val legacy = runBlocking { legacyStore.data.first() }
            if (legacy.asMap().isEmpty()) {
                true
            } else {
                runBlocking {
                    context.dataStore.edit { current ->
                        legacy.asMap().forEach { (key, value) -> putIfAbsent(current, key, value) }
                    }
                }
                true
            }
        } catch (t: Exception) {
            Log.w(TAG, "merge legacy preferences failed", t)
            false
        } finally {
            tempFile.delete()
        }
    }

    private fun copyAsset(context: Context, authority: Uri, path: String, target: File) {
        target.parentFile?.mkdirs()
        val input = context.contentResolver.openInputStream(
            authority.buildUpon().appendPath(path).build()
        ) ?: throw IllegalStateException("missing migration asset $path")
        input.use { source ->
            target.outputStream().use { output -> source.copyTo(output) }
        }
    }

    /** DataStore 的 Preferences 值类型由 key 决定，跨 key 拷贝只能在这里做类型擦除。 */
    @Suppress("UNCHECKED_CAST")
    private fun putIfAbsent(target: MutablePreferences, key: Preferences.Key<*>, value: Any) {
        val typedKey = key as Preferences.Key<Any>
        if (typedKey !in target) target[typedKey] = value
    }
}
