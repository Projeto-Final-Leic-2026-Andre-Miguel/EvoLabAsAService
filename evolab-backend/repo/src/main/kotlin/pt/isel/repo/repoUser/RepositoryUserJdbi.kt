package pt.isel.repo.repoUser

import org.jdbi.v3.core.Handle
import pt.isel.domain.AuthProvider
import pt.isel.domain.User
import org.jdbi.v3.core.kotlin.mapTo
import java.sql.Types
import java.time.Instant

class RepositoryUserJdbi(
    private val handle: Handle,
) : RepositoryUser {
    override fun createLocalUser(
        name: String,
        email: String,
        passwordHash: String,
    ): User {

        val id =
            handle
                .createUpdate(UserSql.CREATE_USER)
                .bind("name", name)
                .bind("email", email)
                .bind("password_hash", passwordHash)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .one()

        return User(
            id, name, email, passwordHash,
            authProvider = AuthProvider.LOCAL,
            providerId = null,
            createdAt = Instant.now()
        )
    }

    override fun createOAuthUser(
        name: String,
        email: String,
        provider: AuthProvider,
        providerId: String,
    ): User {
        val id =
            handle
                .createUpdate(UserSql.CREATE_OAUTH_USER)
                .bind("name", name)
                .bind("email", email)
                .bindNull("password_hash", Types.VARCHAR)
                .bind("auth_provider", provider)
                .bind("provider_id", providerId)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .one()

        return User(
            id, name, email, null,
            authProvider = provider,
            providerId = providerId,
            createdAt = Instant.now()
        )
    }

    override fun findByEmail(email: String): User? =
        handle
            .createQuery(UserSql.FIND_BY_EMAIL)
            .bind("email", email)
            .mapTo<User>()
            .findOne()
            .orElse(null)

    override fun findByProvider(
        provider: AuthProvider,
        providerId: String,
    ): User? =

        handle
            .createQuery(UserSql.FIND_BY_PROVIDER)
            .bind("provider", provider)
            .bind("providerId", providerId)
            .mapTo<User>()
            .findOne()
            .orElse(null)

    override fun findById(id: Int): User? =
        handle
            .createQuery(UserSql.FIND_BY_ID)
            .bind("id", id)
            .mapTo<User>()
            .findOne()
            .orElse(null)

    override fun findAll(): List<User> =
        handle
            .createQuery(UserSql.FIND_ALL)
            .mapTo<User>()
            .list()

    override fun save(entity: User) {
        TODO("Not yet implemented")
    }

    override fun deleteById(id: Int): Boolean =
        handle
            .createUpdate(UserSql.DELETE_BY_ID)
            .bind("id", id)
            .execute() > 0

    override fun clear() {
        handle
            .createUpdate(UserSql.CLEAR)
            .execute()
    }

    override fun count(): Long =
        handle.createQuery("SELECT COUNT(*) FROM users")
            .mapTo<Long>()
            .one()

}