package com.example.wp04.abstd.data.profile.di

import com.example.wp04.abstd.data.profile.DefaultProfileRepository
import com.example.wp04.abstd.data.profile.api.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface ProfileDataModule {

    @Binds
    fun bindProfileRepository(impl: DefaultProfileRepository): ProfileRepository
}
