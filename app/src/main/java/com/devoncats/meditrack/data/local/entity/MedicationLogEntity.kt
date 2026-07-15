package com.devoncats.meditrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.devoncats.meditrack.domain.model.MedicationLogStatus

@Entity(
    tableName = "medication_logs",
    indices = [Index(value = ["medicationId"]), Index(value = ["scheduleId"])],
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)

data class MedicationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    // Nullable/defaulted so a log's origin schedule is known (fixes the missed-dose
    // worker evaluating the wrong schedule's dose when a medication has more than one
    // schedule); null only for rows migrated from schema v1 that predate this column.
    val scheduleId: Long? = null,
    val scheduledDatetime: Long,
    val confirmedAt: Long?,
    val status: MedicationLogStatus
)
