package pt.isel.repointerface

import pt.isel.domain.User


interface RepositoryUser {
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
    fun findById(id: Int): User?
    fun count(): Long
}
