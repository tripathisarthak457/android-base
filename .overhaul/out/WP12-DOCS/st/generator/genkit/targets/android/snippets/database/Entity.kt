package __PKG__.core.database.__NAME__

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * __ABOUT__
 */
@Entity(tableName = "__TABLE__")
data class __P__Entity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
)
