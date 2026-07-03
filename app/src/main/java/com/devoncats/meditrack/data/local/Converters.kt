package com.devoncats.meditrack.data.local

import androidx.room.TypeConverter
import com.devoncats.meditrack.data.local.entity.MedicationLogStatus
import com.devoncats.meditrack.data.local.entity.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = UserRole.valueOf(value)

    @TypeConverter
    fun fromMedicationLogStatus(status: MedicationLogStatus): String = status.name

    @TypeConverter
    fun toMedicationLogStatus(value: String): MedicationLogStatus = MedicationLogStatus.valueOf(value)
}
