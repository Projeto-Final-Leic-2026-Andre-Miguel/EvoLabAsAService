package pt.isel.http.argumentResolverandInterceptor

import com.example.evolab.domain.user.AuthenticatedUser
import com.example.evolab.service.auxiliary.Either
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import com.example.evolab.service.tokenService.TokenService

@Component
class RequestTokenProcessor(
    val tokenService: TokenService,
) {
    fun processAuthorizationHeaderValue(authorizationValue: String?): AuthenticatedUser? {
        if (authorizationValue == null) {
            return null
        }
        val parts = authorizationValue.trim().split(" ")
        if (parts.size != 2) {
            return null
        }
        if (parts[0].lowercase() != SCHEME) {
            return null
        }
        return when (val result = tokenService.getUserByToken(parts[1])) {
            is Either.Right ->
                AuthenticatedUser(
                    result.value,
                    parts[1],
                )
            is Either.Left -> null
        }
    }

    fun processAuthorizationCookie(request: HttpServletRequest): AuthenticatedUser? {
        val cookies = request.cookies ?: return null
        val tokenCookie = cookies.find { it.name == COOKIE_NAME } ?: return null

        return when (val result = tokenService.getUserByToken(tokenCookie.value)) {
            is Either.Right -> AuthenticatedUser(result.value, tokenCookie.value)
            is Either.Left -> null
        }
    }



    companion object {
        const val SCHEME = "bearer"
        const val COOKIE_NAME = "auth-token"
        const val COOKIE_MAX_AGE = 3600
    }
}
