package kotlinbook

import kotliquery.TransactionalSession
import kotliquery.sessionOf
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test
import kotlin.test.assertNotEquals

val testAppConfig = createAppConfig("test")
val testDataSource = createDataSource(testAppConfig)

fun testTx(handler: (dbSess: TransactionalSession) -> Unit) {
    sessionOf(testDataSource, returnGeneratedKey = true).use { dbSession ->
        dbSession.transaction { dbSessTx ->
            {
                try {
                    handler(dbSessTx)
                } finally {
                    dbSessTx.connection.rollback()
                }
            }
        }
    }
}

class UserTest {
    @Test
    fun testCreateUser() {
        testTx { dbSession ->
            val userAId = createUser(
                dbSession,
                email = "augustlilleaas@me.com",
                name = "August Lilleaas",
                passwordText = "1234"
            )
            val userBId = createUser(
                dbSession,
                email = "august@augustl.com",
                name = "August Lilleaas",
                passwordText = "1234"
            )

            assertNotEquals(userAId, userBId)
        }
    }

    @Test
    fun testListUsers() {
        testTx { dbSess ->
            val userAId = createUser(
                dbSess,
                email = "augustlilleaas@me.com",
                name = "August Lilleaas",
                passwordText = "1234"
            )
            val userBId = createUser(
                dbSess,
                email = "august@augustl.com",
                name = "August Lilleaas",
                passwordText = "1234"
            )
            val users = listUsers(dbSess)
            assertEquals(2, users.size)
            assertNotNull(users.find { it.id == userAId })
            assertNotNull(users.find { it.id == userBId })
        }
    }

    @Test
    fun testGetUser() {
        testTx { dbSess ->
            val userId = createUser(
                dbSess,
                email = "augustlilleaas@me.com",
                name = "August Lilleaas",
                passwordText = "1234",
                tosAccepted = true
            )
            assertNull(getUser(dbSess, -9000))
            val user = getUser(dbSess, userId)
            assertNotNull(user)
            assertEquals(user?.email, "augustlilleaas@me.com")
        }
    }
}