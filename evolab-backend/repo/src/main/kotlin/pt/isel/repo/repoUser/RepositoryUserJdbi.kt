package pt.isel.repo.repoUser

import org.jdbi.v3.core.Handle
import pt.isel.domain.AuthProvider
import pt.isel.domain.User
import org.jdbi.v3.core.kotlin.mapTo
import org.slf4j.LoggerFactory

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
                .bind("password_validation", passwordValidation.validationInfo)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .one()

        return User(id, name, email, passwordValidation)
    }

    override fun createOAuthUser(
        name: String,
        email: String,
        provider: AuthProvider,
        providerId: String,
    ): User {
        val id =
            handle
                .createUpdate(UserSql.CREATE_USER)
                .bind("name", name)
                .bind("email", email)
                .bind("password_validation", passwordValidation.validationInfo)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .one()

        return User(id, name, email, passwordValidation)
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
    ): User? = TODO("Implement user lookup by provider and provider id")

    override fun findById(id: Int): User? =
        handle
            .createQuery(UserSql.FIND_BY_ID)
            .bind("id", id)
            .mapTo<User>()
            .findOne()
            .orElse(null)

    override fun count(): Long =
        handle.createQuery("SELECT COUNT(*) FROM dbo.users")
            .mapTo<Long>()
            .one()

}