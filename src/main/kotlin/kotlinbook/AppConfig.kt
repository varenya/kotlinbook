package kotlinbook

import com.typesafe.config.ConfigFactory

data class WebappConfig(
    val httpPort: Int,
    val dbUser: String,
    val dbPassword: String,
    val dbUrl: String
)

fun createAppConfig(env: String) =
    ConfigFactory
        .parseResources("app-$env.conf")
        .withFallback(ConfigFactory.parseResources("app.conf"))
        .resolve()
        .let {
            WebappConfig(
                httpPort = it.getInt("httpPort"),
                dbPassword = it.getString("dbPassword"),
                dbUrl = it.getString("dbUrl"),
                dbUser = it.getString("dbUser")
            )
        }

