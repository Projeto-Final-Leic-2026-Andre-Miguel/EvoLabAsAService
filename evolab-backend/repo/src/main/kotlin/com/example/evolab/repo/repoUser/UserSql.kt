package com.example.evolab.repo.repoUser

object UserSql {
    const val FIND_ALL = """
        SELECT * FROM users ORDER BY id
    """

    const val CREATE_USER = """
        INSERT INTO users (name, email, password_hash)
        VALUES (:name, :email, :password_hash)
        RETURNING id
    """

    const val CREATE_OAUTH_USER = """
        INSERT INTO users (name, email, password_hash, auth_provider, provider_id)
        VALUES (:name, :email, :password_hash, :auth_provider, :provider_id)
        RETURNING id
    """

    const val FIND_BY_EMAIL = """
        SELECT * FROM users WHERE email = :email
    """

    const val FIND_BY_ID = """
        SELECT * FROM users WHERE id = :id
    """

    const val FIND_BY_PROVIDER = """
        SELECT * FROM users WHERE auth_provider = :provider AND provider_id = :providerId
    """

    const val FIND_BY_TOKEN_VALIDATION = """
        SELECT u.*
        FROM users u
        JOIN tokens t ON t.user_id = u.id
        WHERE t.token_validation = :tokenValidation
    """

    const val SAVE = """
        UPDATE users
        SET name = :name,
            email = :email,
            password_hash = :passwordHash,
            auth_provider = :authProvider,
            provider_id = :providerId
        WHERE id = :id
    """

    const val DELETE_BY_ID = """
        DELETE FROM users WHERE id = :id
    """

    const val CLEAR = """
        DELETE FROM users
    """

}