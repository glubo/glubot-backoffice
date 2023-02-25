package model

import dev.fritz2.core.Lenses
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Put your model data classes in here to use it on js and jvm side

@Lenses
@Serializable
data class Framework(
    val name: String,
) {
    companion object
}

// This class holds the information of the principal currently authenticated
@Lenses
@Serializable
data class Principal(
    val name: String,
    val roles: List<String> = emptyList(),
    val token: String,
) {
    companion object
}

@Lenses
@Serializable
data class Token(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Int,
    @SerialName("id_token")
    val idToken: String,
    @SerialName("not-before-policy")
    val notBeforePolicy: Int,
    @SerialName("refresh_expires_in")
    val refreshExpiresIn: Int,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("scope")
    val scope: String,
    @SerialName("session_state")
    val sessionState: String,
    @SerialName("token_type")
    val tokenType: String,
) {
    companion object
}

@Lenses
@Serializable
data class Config(
    val openIdConnectBaseUri: String = "https://keycloak.glubo.cz/realms/glubot/protocol/openid-connect",
    val openIdClientId: String = "postman",
    val openIdClientSecret: String = "",
    val redirectUri: String = "http://localhost:8081/",
    val hiveUri: String = "http://localhost:8080",
) {
    companion object
}
