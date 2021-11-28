package com.frost.runningapp.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.room.Room
import com.frost.runningapp.R
import com.frost.runningapp.db.RunningDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideRunningDatabase(
        @ApplicationContext app: Context
    ) = Room.databaseBuilder(app, RunningDatabase::class.java, app.getString(R.string.database)).build()

    @Singleton
    @Provides
    fun provideRunDao(db: RunningDatabase) = db.getRunDao()

    @Singleton
    @Provides
    fun provideSharedPref(@ApplicationContext app: Context) =
        app.getSharedPreferences(app.getString(R.string.SHARED_PREFERENCES), MODE_PRIVATE)

    @Singleton
    @Provides
    fun providesName(sharedPreferences: SharedPreferences) =
        sharedPreferences.getString("KEY_FIRST_TIME_NAME", "") ?: ""

    @Singleton
    @Provides
    fun providesWeight(sharedPreferences: SharedPreferences) =
        sharedPreferences.getFloat("KEY_FIRST_TIME_WEIGHT", 80f)

    @Singleton
    @Provides
    fun providesToggle(sharedPreferences: SharedPreferences) =
        sharedPreferences.getBoolean("KEY_FIRST_TIME_TOGGLE", true)
}