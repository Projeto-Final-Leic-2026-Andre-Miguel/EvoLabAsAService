package com.example.evolab.repo.repoUser

import com.example.evolab.repo.Repository
import com.example.evolab.domain.user.AuthProvider
import com.example.evolab.domain.user.User


interface RepositoryUser : Repository<User> {
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