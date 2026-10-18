package com.example.wp04.base.data.profile.di

import com.example.wp04.base.data.profile.DefaultProfileRepository
import com.example.wp04.base.data.profile.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ProfileDataModule {
    @Binds
    fun bindProfileRepository(impl: DefaultProfileRepository): ProfileRepository
}
