package com.devoncats.meditrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

    companion object {
        const val DATABASE_NAME = "meditrack.db"
    }
}
