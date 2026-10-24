package __PKG__.data.__NAME__.di

import __PKG__.core.database.AppDatabase
import __PKG__.core.database.__NAME__.__P__Dao
import __PKG__.data.__NAME__.OfflineFirst__P__Repository
import __PKG__.data.__NAME__.api.__P__Repository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface __P__DataModule {

    @Binds
__FIT__
    fun bind__P__Repository(impl: OfflineFirst__P__Repository): __P__Repository
__ELSE__
    fun bind__P__Repository(
        impl: OfflineFirst__P__Repository,
    ): __P__Repository
__END_FIT__

    companion object {
        @Provides
__FIT__
        fun provide__P__Dao(database: AppDatabase): __P__Dao = database.__CAMEL__()
__ELSE__
        fun provide__P__Dao(database: AppDatabase): __P__Dao =
            database.__CAMEL__()
__ELSE__
        fun provide__P__Dao(
            database: AppDatabase,
        ): __P__Dao = database.__CAMEL__()
__END_FIT__
    }
}
