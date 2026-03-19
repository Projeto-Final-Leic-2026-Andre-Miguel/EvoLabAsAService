package pt.isel.repo.repoUser

import pt.isel.domain.AuthProvider
import pt.isel.domain.User
import pt.isel.repo.Repository

//import java.security.AuthProvider Será que rende

interface RepositoryUser : Repository<User>{
    fun createLocalUser(
        name: String,
        email: String,
        passwordHash: String,
    ): User

    fun createOAuthUser(
        name: String,
        email: String,
        provider: AuthProvider,   // GOOGLE, etc.
        providerId: String,
    ): User

    fun findByEmail(email: String): User?

    fun findByProvider(provider: AuthProvider, providerId: String): User?

    fun count(): Long
}