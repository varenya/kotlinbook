package kotlinbook

import kotliquery.Session
import kotliquery.queryOf
import java.nio.ByteBuffer
import java.time.OffsetDateTime
import java.time.ZonedDateTime

fun createUser(
    dbSession: Session,
    email: String,
    name: String,
    passwordText: String,
    tosAccepted: Boolean = false
): Long {
    val userId = dbSession.updateAndReturnGeneratedKey(
        queryOf(
            """
            INSERT INTO user_t
            (email, name, tos_accepted, password_hash)
            VALUES (:email, :name, :tosAccepted, :passwordHash)
            """.trimIndent(),
            mapOf(
                "email" to email,
                "name" to name,
                "tosAccepted" to tosAccepted,
                "passwordHash" to passwordText.toByteArray(Charsets.UTF_8)
            )
        )
    )
    return userId!!
}

data class User(
    val id: Long,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val email: String,
    val tosAccepted: Boolean,
    val name: String?,
    val passwordHash: ByteBuffer
) {
    companion object {
        fun fromRow(row: Map<String, Any?>) = User(
            id = row["id"] as Long,
            createdAt = (row["created_at"] as OffsetDateTime).toZonedDateTime(),
            updatedAt = (row["updated_at"] as OffsetDateTime).toZonedDateTime(),
            email = row["email"] as String,
            name = row["name"] as? String,
            tosAccepted = row["tos_accepted"] as Boolean,
            passwordHash = ByteBuffer.wrap(
                row["password_hash"] as ByteArray
            )
        )
    }
}

fun getUser(dbSession: Session, id: Long): User? {
    return dbSession
        .single(
            queryOf("SELECT * FROM user_t WHERE id ?", id),
            ::mapFromRow
        )
        ?.let(User::fromRow)
}

fun listUsers(dbSession: Session) =
    dbSession.list(queryOf("SELECT * FROM user_t"), ::mapFromRow)
        .map(User::fromRow)