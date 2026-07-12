package com.vatsalya.founderpocket.data.db

import androidx.room.TypeConverter
import com.vatsalya.founderpocket.data.model.CaptureType
import java.nio.ByteBuffer

class Converters {

    @TypeConverter
    fun fromCaptureType(type: CaptureType): String = type.name

    @TypeConverter
    fun toCaptureType(value: String): CaptureType = CaptureType.valueOf(value)

    @TypeConverter
    fun fromFloatArray(array: FloatArray?): ByteArray? {
        if (array == null) return null
        val buffer = ByteBuffer.allocate(array.size * 4)
        array.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    @TypeConverter
    fun toFloatArray(bytes: ByteArray?): FloatArray? {
        if (bytes == null) return null
        val buffer = ByteBuffer.wrap(bytes)
        return FloatArray(bytes.size / 4) { buffer.getFloat() }
    }
}
