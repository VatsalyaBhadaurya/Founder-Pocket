package com.vatsalya.founderpocket.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.data.model.CaptureFts

@Database(
    entities = [Capture::class, CaptureFts::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
}
