package __PKG__.core.database.__NAME__

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Reads are flows that follow every write; writes suspend. */
@Dao
interface __P__Dao {

    @Query("SELECT * FROM __TABLE__")
    fun observeAll(): Flow<List<__P__Entity>>

    @Query("SELECT * FROM __TABLE__ WHERE id = :id")
    suspend fun byId(id: String): __P__Entity?

    @Upsert
    suspend fun upsertAll(rows: List<__P__Entity>)

    @Query("DELETE FROM __TABLE__")
    suspend fun deleteAll()

    /** Makes the table the server's list in one transaction, so an observer never sees it half written. */
    @Transaction
    suspend fun replaceAll(rows: List<__P__Entity>) {
        deleteAll()
        upsertAll(rows)
    }
}
