package com.github.mytv.bv.migration

/**
 * 旧版(BV)与新应用(myBV)之间迁移通道的固定约定。
 *
 * 旧版最后一个版本内置 [LegacyMigrationProvider]，新版首次启动读取这里定义的文件。
 * 两端的包名/authority 必须保持稳定，不能跟着应用品牌改名。
 */
object LegacyMigrationContract {
    const val LEGACY_APP_ID = "dev.aaa1115910.bv"
    const val NEW_APP_ID = "com.github.mytv.bv"

    const val LEGACY_AUTHORITY = "$LEGACY_APP_ID.migration"

    const val METHOD_INFO = "info"
    const val KEY_VERSION_CODE = "versionCode"
    const val KEY_HAS_DATASTORE = "hasDatastore"
    const val KEY_HAS_DATABASE = "hasDatabase"

    const val PATH_DATASTORE = "datastore"
    const val PATH_DATABASE = "database"
    const val PATH_DATABASE_WAL = "database-wal"
    const val PATH_DATABASE_SHM = "database-shm"

    const val DATASTORE_FILE = "datastore/Settings.preferences_pb"
    const val DATABASE_NAME = "AppDatabase.db"
}
