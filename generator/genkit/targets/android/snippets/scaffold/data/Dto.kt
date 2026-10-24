package __PKG__.data.__NAME__.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The wire format, exactly as the backend sends it. */
@Serializable
internal data class __P__Dto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String = "",
)
