package com.devoncats.meditrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
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

        // v1 -> v2: tags each medication log with the schedule it belongs to, so the
        // missed-dose worker can evaluate the right dose when a medication has more than
        // one schedule (previously it only had medicationId to go on).
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medication_logs ADD COLUMN scheduleId INTEGER DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_medication_logs_scheduleId ON medication_logs(scheduleId)")
            }
        }

        @Volatile
        private var instance: MediTrackDatabase? = null

        fun getInstance(context: Context): MediTrackDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MediTrackDatabase::class.java,
                    DATABASE_NAME
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
