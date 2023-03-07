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
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.serialization.json.*
import model.Config
import model.Framework
import model.Principal
import model.Token
import model.name
import org.w3c.dom.url.URL
import org.w3c.dom.url.URLSearchParams

class GlubotAuthentication(
    val authEndpointURL: URL,
    val redirectUri: String,
    val tokenStore: Store<Token?>,
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

suspend fun loadConfig(): Config {
    val configEndpoint = http("/")
    val response = configEndpoint.get("config.json")
    return Json.decodeFromString(Config.serializer(), response.body())
}

fun main() {
    val frameworkStore = storeOf(Framework("fritz2"))
    val name = frameworkStore.map(Framework.name())
//    val config = loadConfig()
    val config = Config()

    val tokenEndpointUri = "${config.openIdConnectBaseUri}/token"
    val authEndpointUri = "${config.openIdConnectBaseUri}/auth"

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
                                it.append("redirect_uri", config.redirectUri)
                                it.append("client_id", config.openIdClientId)
                                it.append("client_secret", config.openIdClientSecret)
                                it
                            }.toString()
                    )
                    .contentType("application/x-www-form-urlencoded")
                    .post()

                val tokenJson = response.body()
                localStorage.setItem("klicky", tokenJson)
                window.location.href = url.let {
                    it.searchParams.delete("session_state")
                    it.searchParams.delete("code")
                    it
                }.toString()
                val token = Json.decodeFromString(Token.serializer(), tokenJson)
                token
            } else if(localStorage.getItem("klicky") != null) {
                val tokenJson = localStorage.getItem("klicky")!!
                val token = Json.decodeFromString(Token.serializer(), tokenJson)
                token
            } else {
                it
            }
        }
    }

    val glubotAuthentication = GlubotAuthentication(
        authEndpointURL = URL(authEndpointUri),
        redirectUri = config.redirectUri,
        tokenStore = tokenStore,
    )

    val userStore = object : RootStore<String>("") {

        val users = http(config.hiveUri + "/hello").use(glubotAuthentication)

        val loadAllUsers = handle {
            try {
                console.info("Loading...")
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