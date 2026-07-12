package com.vatsalya.founderpocket.di

import android.content.Context
import androidx.room.Room
import com.vatsalya.founderpocket.data.db.AppDatabase
import com.vatsalya.founderpocket.data.db.CaptureDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "founder_pocket.db")
            .build()

    @Provides
    fun provideCaptureDao(db: AppDatabase): CaptureDao = db.captureDao()
}
