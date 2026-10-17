package dev.aaa1115910.biliapi.http.entity.video

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GaiaVgateRegisterData(
    val token: String = "",
    val geetest: GeetestData = GeetestData()
) {
    @Serializable
    data class GeetestData(
        val gt: String = "",
        val challenge: String = ""
    )
}

@Serializable
data class GaiaVgateValidateData(
    @SerialName("is_valid")
    val isValid: Int = 0,
    @SerialName("grisk_id")
    val griskId: String = ""
)
