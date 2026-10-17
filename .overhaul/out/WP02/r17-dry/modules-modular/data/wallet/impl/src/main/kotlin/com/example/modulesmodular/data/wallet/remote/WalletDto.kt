package com.example.modulesmodular.data.wallet.remote

import com.example.modulesmodular.data.wallet.api.WalletItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The wire format, exactly as the backend sends it. */
@Serializable
internal data class WalletDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String = "",
)

internal fun WalletDto.toDomain(): WalletItem = WalletItem(id = id, title = title.trim())
