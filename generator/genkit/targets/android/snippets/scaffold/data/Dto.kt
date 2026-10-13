package __PKG__.data.__NAME__.remote

import __PKG__.data.__NAME__.api.__P__Item
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The wire format, exactly as the backend sends it. */
@Serializable
internal data class __P__Dto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String = "",
)

internal fun __P__Dto.toDomain(): __P__Item =
    __P__Item(id = id, title = title.trim())
