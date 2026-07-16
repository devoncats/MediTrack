package com.devoncats.meditrack.di

import android.content.Context
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.dao.EmergencyContactDao
import com.devoncats.meditrack.data.local.dao.MedicationDao
import com.devoncats.meditrack.data.local.dao.MedicationLogDao
import com.devoncats.meditrack.data.local.dao.ScheduleDao
import com.devoncats.meditrack.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MediTrackDatabase =
        MediTrackDatabase.getInstance(context)

    @Provides
    fun provideUserDao(database: MediTrackDatabase): UserDao = database.userDao()

    @Provides
    fun provideMedicationDao(database: MediTrackDatabase): MedicationDao = database.medicationDao()

    @Provides
    fun provideScheduleDao(database: MediTrackDatabase): ScheduleDao = database.scheduleDao()

    @Provides
    fun provideMedicationLogDao(database: MediTrackDatabase): MedicationLogDao = database.medicationLogDao()

    @Provides
    fun provideEmergencyContactDao(database: MediTrackDatabase): EmergencyContactDao = database.emergencyContactDao()
}
