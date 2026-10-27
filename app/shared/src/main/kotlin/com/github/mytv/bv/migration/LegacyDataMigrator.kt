package com.github.mytv.bv.migration

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.github.mytv.bv.R
import com.github.mytv.bv.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * 新版 myBV 首次启动时，从仍安装着的旧版 BV(applicationId=dev.aaa1115910.bv) 迁移：
 * - Room 数据库(搜索历史/用户列表)在主库尚未创建时整体拷贝；
 * - DataStore(设置/登录 cookie/refresh token)按 key 合并，新版已有的值优先。
 *
 * 旧版必须是带 [LegacyMigrationProvider] 的桥接版；比它更旧的版本没有迁移通道，
 * 只能重新登录。迁移成功后写入标记文件，后续启动不再重复。
 */
object LegacyDataMigrator {
    private const val TAG = "LegacyDataMigrator"
    private const val MARKER_FILE = "legacy_migration_v1.done"

    fun migrateIfNeeded(context: Context) {
        if (context.packageName != LegacyMigrationContract.NEW_APP_ID) return
        val appContext = context.applicationContext
        val marker = File(appContext.filesDir, MARKER_FILE)
        if (marker.exists()) return
        if (!isLegacyInstalled(appContext)) return

        val authority = Uri.parse("content://${LegacyMigrationContract.LEGACY_AUTHORITY}")
        val info = runCatching {
            appContext.contentResolver.call(
                authority,
                LegacyMigrationContract.METHOD_INFO,
                null,
                null,
            )
        }.onFailure {
            Log.i(TAG, "legacy migration provider unavailable: ${it.message}")
        }.getOrNull() ?: return

        val hasDatabase = info.getBoolean(LegacyMigrationContract.KEY_HAS_DATABASE, false)
        val hasDataStore = info.getBoolean(LegacyMigrationContract.KEY_HAS_DATASTORE, false)
        if (!hasDatabase && !hasDataStore) return

        var success = true
        if (hasDatabase) success = copyDatabase(appContext, authority) && success
        if (hasDataStore) success = mergePreferences(appContext, authority) && success
        if (!success) return

        runCatching { marker.createNewFile() }
        Log.i(TAG, "legacy data migrated")
        Toast.makeText(
            appContext,
            R.string.legacy_migration_success,
            Toast.LENGTH_LONG,
        ).show()
    }

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
