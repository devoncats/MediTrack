package com.devoncats.meditrack.domain.usecase

import com.devoncats.meditrack.services.AlarmScheduler
import javax.inject.Inject

class PostponeDoseUseCase @Inject constructor(private val alarmScheduler: AlarmScheduler) {
    operator fun invoke(scheduleId: Long, medicationId: Long, logId: Long) {
        alarmScheduler.postpone(scheduleId, medicationId, logId)
    }
}
