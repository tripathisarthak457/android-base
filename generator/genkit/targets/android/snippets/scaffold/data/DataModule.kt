package __PKG__.data.__NAME__.di

import __PKG__.data.__NAME__.__IMPL__
import __PKG__.data.__NAME__.api.__P__Repository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface __P__DataModule {

    @Binds
__FIT__
    fun bind__P__Repository(impl: __IMPL__): __P__Repository
__ELSE__
    fun bind__P__Repository(
        impl: __IMPL__,
    ): __P__Repository
__END_FIT__
}
