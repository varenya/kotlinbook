package kotlinbook

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import kotlin.random.Random
import kotlin.reflect.full.declaredMemberProperties

data class WebappConfig(val httpPort: Int)


sealed class WebResponse {
    abstract val statusCode: Int
    abstract val headers: Map<String, List<String>>
    abstract fun copyResponse(
        statusCode: Int,
        headers: Map<String, List<String>>
    ): WebResponse

    fun header(headerName: String, headerValue: String) =
        header(headerName, listOf(headerValue))

    fun headers(): Map<String, List<String>> =
        headers.map {
            it.key.lowercase() to it.value
        }.fold(mapOf()) { res, (k, v) ->
            res.plus(Pair(k, res.getOrDefault(k, listOf()).plus(v)))
        }

    fun header(headerName: String, headerValue: List<String>) =
        copyResponse(
            statusCode, headers.plus(
                Pair(
                    headerName, headers.getOrDefault(headerName, listOf()).plus(headerValue)
                )
            )
        )
}

fun String.getRandomLetter() =
    this[Random.nextInt(this.length)]


data class TextWebResponse(
    val body: String,
    override val statusCode: Int = 200,
    override val headers: Map<String, List<String>> = mapOf()
) : WebResponse() {
    override fun copyResponse(statusCode: Int, headers: Map<String, List<String>>) = copy(body, statusCode, headers)
}

data class JsonWebResponse(
    val body: Any?,
    override val statusCode: Int = 200,
    override val headers: Map<String, List<String>> = mapOf()
) : WebResponse() {
    override fun copyResponse(statusCode: Int, headers: Map<String, List<String>>) =
        copy(body, statusCode, headers)
}

fun createAppConfig(env: String) =
    ConfigFactory
        .parseResources("app-$env.conf")
        .withFallback(ConfigFactory.parseResources("app.conf"))
        .resolve()
        .let {
            WebappConfig(httpPort = it.getInt("httpPort"))
        }

private val log = LoggerFactory.getLogger("kotlinbook.Main")

fun main() {

    log.debug("Starting the application...")
    val env = System.getenv("KOTLIN_ENV") ?: "local"
    val appConfig = createAppConfig(env)
    val secretsRegex = "password|secret|key".toRegex(RegexOption.IGNORE_CASE)

    log.debug("Configuration loaded successfully: ${
        WebappConfig::class.declaredMemberProperties
            .sortedBy { it.name }
            .map {
                if (secretsRegex.containsMatchIn(it.name)) {
                    "${it.name} = ${it.get(appConfig).toString().take(2)}*****"
                } else {
                    "${it.name} = ${it.get(appConfig)}"
                }
            }.joinToString(separator = "\n")
    }"
    )
    embeddedServer(Netty, port = appConfig.httpPort) { createKtorApplication() }.start(wait = true)
}

fun Application.createKtorApplication() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            kotlinbook.log.error("An unknown error occurred", cause)
            call.respondText(
                text = "500: $cause", status = HttpStatusCode.InternalServerError
            )
        }
    }
    routing {
        get("/") {
            call.respondText("Hello World")
        }
    }
}