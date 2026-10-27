package com.github.mytv.bv.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.github.mytv.bv.entity.db.SearchHistoryDB

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history")
    suspend fun getAll(): List<SearchHistoryDB>

    @Query("SELECT * FROM search_history ORDER BY search_date DESC LIMIT :count")
    suspend fun getHistories(count: Int): List<SearchHistoryDB>

    @Query("SELECT * FROM search_history WHERE keyword = :keyword LIMIT 1")
    suspend fun findHistory(keyword: String): SearchHistoryDB?

    @Query("SELECT * FROM search_history WHERE keyword LIKE '%' || :keyword || '%' LIMIT :count")
    suspend fun findHistories(keyword: String, count: Int): List<SearchHistoryDB>

    @Insert
    suspend fun insert(vararg searchHistoryDB: SearchHistoryDB)

    @Delete
    suspend fun delete(vararg searchHistoryDB: SearchHistoryDB)

    @Query("DELETE FROM search_history")
    suspend fun deleteAll()

    @Update
    suspend fun update(searchHistoryDB: SearchHistoryDB)
}