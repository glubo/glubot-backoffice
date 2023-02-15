package app

import dev.fritz2.core.RootStore
import dev.fritz2.core.Store
import dev.fritz2.core.Window
import dev.fritz2.core.render
import dev.fritz2.core.storeOf
import dev.fritz2.remote.Authentication
//import dev.fritz2.remote.FetchException
import dev.fritz2.remote.Request
import dev.fritz2.remote.http
import kotlinx.browser.window
import kotlinx.serialization.json.*
import model.Framework
import model.Principal
import model.Token
import model.name
import org.w3c.dom.url.URL
import org.w3c.dom.url.URLSearchParams

class GlubotAuthentication(
    val authEndpointURL: URL,
    val redirectUri: String,
    val tokenStore: Store<Token?>
) : Authentication<Principal>() {

    override fun addAuthentication(request: Request, principal: Principal?): Request {
        console.log("ENRICH")
        return request.header(
            "Authorization",
            "Bearer ${tokenStore.current?.accessToken}"
        )
    }

    override fun authenticate() {
        console.log("AUTHENTICATE")
        val url = authEndpointURL
        url.searchParams.append("response_type", "code")
        url.searchParams.append("client_id", "postman")
        url.searchParams.append("scope", "openid")
        url.searchParams.append("redirect_uri", redirectUri)
        window.location.href = url.href
    }
}

fun main() {
    val frameworkStore = storeOf(Framework("fritz2"))
    val name = frameworkStore.map(Framework.name())
    val openIdConnectBaseUri = "https://keycloak.glubo.cz/realms/glubot/protocol/openid-connect"
    val redirectUri = "http://localhost:8081/"

    val tokenEndpointUri = "$openIdConnectBaseUri/token"
    val authEndpointUri = "$openIdConnectBaseUri/auth"

    val tokenStore = object : RootStore<Token?>(null) {
        val checkState = handle { it ->
            val url = URL(window.location.href)
            if (url.searchParams.has("session_state") && url.searchParams.has("code")) {
                // TODO: check session_state
                val code = url.searchParams.get("code") ?: "wtf"
                val tokenEndpoint = http(tokenEndpointUri)
                val response = tokenEndpoint
                    .body(
                        URLSearchParams()
                        .let {
                            it.append("grant_type", "authorization_code")
                            it.append("code", code)
                            it.append("redirect_uri", redirectUri)
                            it.append("client_id", "postman")
                            it.append("client_secret", "")
                            it
                        }.toString()
                    )
                    .contentType("application/x-www-form-urlencoded")
                    .post()
                Json.decodeFromString(Token.serializer(), response.body())
            } else {
                it
            }
        }
    }

    val glubotAuthentication = GlubotAuthentication(
        authEndpointURL = URL(authEndpointUri),
        redirectUri = redirectUri,
        tokenStore = tokenStore,
    )

    val userStore = object : RootStore<String>("") {

        val users = http("http://localhost:8080/hello").use(glubotAuthentication)

        val loadAllUsers = handle {
            try {
                users.get().body()
//            } catch (e: FetchException) {
//                e.toString() + ": " + e.toString()
            } catch (e: Throwable) {
                console.error("unknown" + e.printStackTrace() + ": " + e.toString())
                it
            }
        }
    }


    render {
        Window.loads handledBy tokenStore.checkState
        p {
            +"This site an uses: "
            b { name.data.renderText() }
            button {
                +"load"
                clicks handledBy userStore.loadAllUsers
            }
        }

        h1 {
            +"daata:"
        }

        pre {
            code {
                userStore.data.renderText()
            }
        }
    }
}