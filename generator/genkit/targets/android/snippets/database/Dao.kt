package __PKG__.core.database.__NAME__

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Reads are flows that follow every write; writes suspend. */
@Dao
interface __P__Dao {

    @Query("SELECT * FROM `__TABLE__` ORDER BY position")
    fun observeAll(): Flow<List<__P__Entity>>

    @Query("SELECT * FROM `__TABLE__` WHERE id = :id")
    suspend fun byId(id: String): __P__Entity?

    @Upsert
    suspend fun upsertAll(rows: List<__P__Entity>)

    @Query("DELETE FROM `__TABLE__` WHERE id NOT IN (:ids)")
    suspend fun deleteAllExcept(ids: List<String>)

    /**
     * Makes the table the server's list in one transaction, so an observer never sees it half
     * written: rows the server no longer has go, the rest are written over in place.
     */
    @Transaction
    suspend fun replaceAll(rows: List<__P__Entity>) {
        deleteAllExcept(rows.map { it.id })
        upsertAll(rows)
    }
}
