package com.base.app.data.sample.di

// <opt:database>
import com.base.app.core.database.AppDatabase
import com.base.app.core.database.sample.SampleDao
// </opt:database>
// <opt:!database>import com.base.app.data.sample.NetworkSampleRepository
// <opt:database>
import com.base.app.data.sample.OfflineFirstSampleRepository
// </opt:database>
import com.base.app.data.sample.api.SampleRepository
import dagger.Binds
import dagger.Module
// <opt:database>
import dagger.Provides
// </opt:database>
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

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
