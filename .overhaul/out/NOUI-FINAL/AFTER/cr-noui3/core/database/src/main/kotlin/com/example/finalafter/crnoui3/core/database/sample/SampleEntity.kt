package com.example.finalafter.crnoui3.core.database.sample

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** A sample as last fetched. `data/sample` maps it to and from its own model. */
@Entity(tableName = "samples")
data class SampleEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val body: String,
    /** Where the row sat in the server's list, so the list reads back in the order it came. */
    val position: Int,
)
