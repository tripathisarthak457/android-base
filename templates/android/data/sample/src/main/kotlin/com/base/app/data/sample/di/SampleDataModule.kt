package com.base.app.data.sample.di

import com.base.app.data.sample.api.SampleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
// <opt:database>
import com.base.app.core.database.AppDatabase
import com.base.app.core.database.sample.SampleDao
import com.base.app.data.sample.OfflineFirstSampleRepository
import dagger.Provides
// </opt:database>
// <opt-else:database>
//~ import com.base.app.data.sample.NetworkSampleRepository
// </opt-else:database>

@Module
@InstallIn(SingletonComponent::class)
internal interface SampleDataModule {
    // <opt:database>

    @Binds
    fun bindSampleRepository(impl: OfflineFirstSampleRepository): SampleRepository

    companion object {
        @Provides
        fun provideSampleDao(database: AppDatabase): SampleDao = database.samples()
    }
    // </opt:database>
    // <opt-else:database>
    //~
    //~     @Binds
    //~     fun bindSampleRepository(impl: NetworkSampleRepository): SampleRepository
    // </opt-else:database>
}
