package com.vatsalya.founderpocket.data.model

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "capture")
data class Capture(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,                    // System.currentTimeMillis()
    val type: CaptureType,
    val body: String,
    val payload: String = "{}",             // JSON, type-specific fields
    // context envelope — all opt-in
    val lat: Double? = null,
    val lng: Double? = null,
    val placeLabel: String? = null,
    val sourceApp: String? = null,
    val photoUri: String? = null,
    val audioUri: String? = null,
    val tags: String = "[]",                // JSON array of strings
    // retrieval
    val ftsText: String = "",               // denormalised for FTS4
    val embedding: ByteArray? = null        // serialised FloatArray, written async
)

@Fts4(contentEntity = Capture::class)
@Entity(tableName = "capture_fts")
data class CaptureFts(val ftsText: String)
