package com.vatsalya.founderpocket.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.data.model.CaptureType
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(capture: Capture): Long

    @Update
    suspend fun update(capture: Capture)

    @Query("UPDATE capture SET embedding = :embedding WHERE id = :id")
    suspend fun updateEmbedding(id: Long, embedding: ByteArray)

    @Query("SELECT * FROM capture ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Capture>>

    @Query("SELECT * FROM capture WHERE id = :id")
    suspend fun getById(id: Long): Capture?

    @Query("""
        SELECT capture.* FROM capture
        INNER JOIN capture_fts ON capture.rowid = capture_fts.rowid
        WHERE capture_fts MATCH :query
        ORDER BY capture.createdAt DESC
    """)
    fun searchFts(query: String): Flow<List<Capture>>

    @Query("""
        SELECT * FROM capture
        WHERE type IN (:types)
        AND payload LIKE '%"done":false%'
        ORDER BY createdAt ASC
    """)
    fun getTodayFocus(types: List<CaptureType> = listOf(CaptureType.TASK, CaptureType.FOLLOWUP)): Flow<List<Capture>>

    @Query("SELECT * FROM capture WHERE embedding IS NOT NULL")
    suspend fun getAllWithEmbeddings(): List<Capture>

    @Query("DELETE FROM capture WHERE id = :id")
    suspend fun deleteById(id: Long)
}
