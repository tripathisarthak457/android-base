package com.base.app.data.profile.di

import com.base.app.data.profile.DefaultProfileRepository
import com.base.app.data.profile.api.ProfileRepository
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
