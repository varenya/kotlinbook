package kotlinbook

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotliquery.queryOf
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.reflect.full.declaredMemberProperties


private val log = LoggerFactory.getLogger("kotlinbook.Main")

suspend fun handleCoroutineTest(dbSession: kotliquery.Session) = coroutineScope {
    val client = HttpClient(CIO)
    val randomNumberRequest = async {
        client.get("http://localhost:9876/random_number")
            .bodyAsText()
    }
    val reverseRequest = async {
        client.post("http://localhost:9876/reverse") {
            setBody(randomNumberRequest.await())
        }.bodyAsText()
    }
    val queryOperation = async {
        val pingPong = client.get("http://localhost:9876/ping")
            .bodyAsText()
        dbSession.single(
            queryOf("SELECT count(*) c from user_t WHERE email != ?", pingPong),
            ::mapFromRow
        )
    }

    TextWebResponse(
        """
        Random number: ${randomNumberRequest.await()}
        Reversed : ${reverseRequest.await()}
        Query : ${queryOperation.await()}
    """.trimIndent()
    )
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

    embeddedServer(Netty, port = 9876, module = Application::fakeServer).start(wait = false)
    embeddedServer(Netty, port = appConfig.httpPort) {
        createKtorApplication(dataSource)
    }.start(wait = true)
}


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
        get("/coroutines", webResponseDb(dataSource) { dbSession ->
            handleCoroutineTest(dbSession)
        })
    }
}

fun Application.fakeServer() {
    routing {
        get("/random_number", webResponse {
            val num = (200L..2000L).random()
            delay(num)
            TextWebResponse(num.toString())
        })
        get("/ping", webResponse {
            TextWebResponse("pong")
        })
        post("/reverse", webResponse {
            TextWebResponse(call.receiveText().reversed())
        })
    }
}