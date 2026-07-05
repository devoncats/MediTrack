package com.devoncats.meditrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.devoncats.meditrack.data.local.dao.EmergencyContactDao
import com.devoncats.meditrack.data.local.dao.MedicationDao
import com.devoncats.meditrack.data.local.dao.MedicationLogDao
import com.devoncats.meditrack.data.local.dao.ScheduleDao
import com.devoncats.meditrack.data.local.dao.UserDao
import com.devoncats.meditrack.data.local.entity.EmergencyContactEntity
import com.devoncats.meditrack.data.local.entity.MedicationEntity
import com.devoncats.meditrack.data.local.entity.MedicationLogEntity
import com.devoncats.meditrack.data.local.entity.ScheduleEntity
import com.devoncats.meditrack.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        MedicationEntity::class,
        ScheduleEntity::class,
        MedicationLogEntity::class,
        EmergencyContactEntity::class
    ],
    version = 1,
    exportSchema = true
)

@TypeConverters(Converters::class)
abstract class MediTrackDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun medicationDao(): MedicationDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun medicationLogDao(): MedicationLogDao
    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {
        const val DATABASE_NAME = "meditrack.db"

        @Volatile
        private var instance: MediTrackDatabase? = null

        fun getInstance(context: Context): MediTrackDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MediTrackDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
    }
}
