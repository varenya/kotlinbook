package kotlinbook

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import kotlin.reflect.full.declaredMemberProperties


private val log = LoggerFactory.getLogger("kotlinbook.Main")

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
        get("/", webResponse {
            TextWebResponse("Hello, World!")
        })
        get("/param_test", webResponse {
            TextWebResponse("The param is: ${call.request.queryParameters["foo"]}")
        })
        get("/json_test", webResponse {
            JsonWebResponse(mapOf("foo" to "bar"))
        })
        get("/json_test_header", webResponse {
            JsonWebResponse(mapOf("foo" to "bar"))
                .header("X-Test-Header", "Just a test!")
        })
    }
}
fun main() {
    log.debug("Starting the application...")
    val env = System.getenv("KOTLIN_ENV") ?: "local"
    val appConfig = createAppConfig(env)
    val secretsRegex = "password|secret|key".toRegex(RegexOption.IGNORE_CASE)
    val dataSource = createAndMigrateDataSource(appConfig)
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
    dataSource.connection.use { conn ->
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT 1")
        }
    }
    embeddedServer(Netty, port = appConfig.httpPort, module = Application::createKtorApplication).start(wait = true)
}

