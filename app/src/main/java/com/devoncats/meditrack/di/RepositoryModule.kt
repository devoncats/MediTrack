package com.devoncats.meditrack.di

import com.devoncats.meditrack.data.repository.EmergencyContactRepositoryImpl
import com.devoncats.meditrack.data.repository.MedicationRepositoryImpl
import com.devoncats.meditrack.data.repository.UserRepositoryImpl
import com.devoncats.meditrack.domain.repository.EmergencyContactRepository
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    abstract fun bindMedicationRepository(impl: MedicationRepositoryImpl): MedicationRepository

    @Binds
    abstract fun bindEmergencyContactRepository(impl: EmergencyContactRepositoryImpl): EmergencyContactRepository
}
