package __PKG__.data.__NAME__.di

import __PKG__.data.__NAME__.Default__P__Repository
import __PKG__.data.__NAME__.api.__P__Repository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface __P__DataModule {

    @Binds
    fun bind__P__Repository(impl: Default__P__Repository): __P__Repository
}
