package kotlinbook

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.core.*
import kotliquery.queryOf
import kotliquery.sessionOf
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.io.use
import kotlin.reflect.full.declaredMemberProperties


private val log = LoggerFactory.getLogger("kotlinbook.Main")

fun Application.createKtorApplication(dataSource: DataSource) {
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
        get("/db_test", webResponseDb(dataSource) { dbSession ->
            JsonWebResponse(dbSession.single(queryOf("SELECT 1"), ::mapFromRow))
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
    embeddedServer(
        Netty,
        port = appConfig.httpPort
    ) {
        createKtorApplication(dataSource)
    }.start(wait = true)
}

