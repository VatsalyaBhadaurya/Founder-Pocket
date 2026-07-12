package com.vatsalya.founderpocket.data.repository

import com.vatsalya.founderpocket.data.db.CaptureDao
import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.data.model.CaptureType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptureRepository @Inject constructor(private val dao: CaptureDao) {

    suspend fun save(capture: Capture): Long = dao.insert(capture)

    fun getAll(): Flow<List<Capture>> = dao.getAll()

    fun getTodayFocus(): Flow<List<Capture>> =
        dao.getTodayFocus(listOf(CaptureType.TASK, CaptureType.FOLLOWUP))

    fun searchKeyword(query: String): Flow<List<Capture>> =
        dao.searchFts("$query*")

    suspend fun getById(id: Long): Capture? = dao.getById(id)

    suspend fun getAllWithEmbeddings(): List<Capture> = dao.getAllWithEmbeddings()

    suspend fun updateEmbedding(id: Long, embedding: ByteArray) =
        dao.updateEmbedding(id, embedding)

    suspend fun update(capture: Capture) = dao.update(capture)

    suspend fun delete(id: Long) = dao.deleteById(id)
}
