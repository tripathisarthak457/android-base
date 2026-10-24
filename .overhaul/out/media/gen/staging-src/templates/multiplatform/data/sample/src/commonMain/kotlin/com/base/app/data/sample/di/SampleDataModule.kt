package com.base.app.data.sample.di

import com.base.app.data.sample.api.SampleRepository
import com.base.app.data.sample.remote.SampleApi
import org.koin.dsl.module
// <opt:database>
import com.base.app.data.sample.OfflineFirstSampleRepository
// </opt:database>
// <opt-else:database>
//~ import com.base.app.data.sample.NetworkSampleRepository
// </opt-else:database>

val sampleDataModule = module {
    single { SampleApi(get()) }
    // <opt:database>
    single<SampleRepository> { OfflineFirstSampleRepository(get(), get(), get()) }
    // </opt:database>
    // <opt-else:database>
    //~     single<SampleRepository> { NetworkSampleRepository(get()) }
    // </opt-else:database>
}
