package io.example.audit.core.database.order

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One order row as last fetched.
 * `data/order` maps it to its model.
 */
@Entity(tableName = "order")
data class OrderEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
