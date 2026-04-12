package com.example.evolab.app.security

import com.example.evolab.domain.user.AuthProvider
import com.example.evolab.service.auxiliary.Success
import com.example.evolab.service.tokenService.TokenService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseCookie
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import pt.isel.http.argumentResolverandInterceptor.RequestTokenProcessor
import pt.isel.service.userService.UserAuthService

@Component
class OAuth2LoginSuccessHandler(
    private val userAuthService: UserAuthService,
    private val tokenService: TokenService,
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,  // The redirect request from the google auth, containg the code and state
        response: HttpServletResponse, // where we can send the cookie with the token and redirect to the frontend
        authentication: Authentication, // the user authentication object containing the user details from google
    ) {
        val oauthUser = authentication.principal as? OAuth2User

        if (oauthUser == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth principal not available")
            return
        }

        val email = oauthUser.attributes["email"] as? String
        val name = oauthUser.attributes["name"] as? String ?: email ?: "oauth-user"
        val providerId = oauthUser.attributes["sub"] as? String // sub = subject

        if (email.isNullOrBlank() || providerId.isNullOrBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required OAuth attributes")
            return
        }

        val userResult = userAuthService.createOAuthUser(name, email, AuthProvider.GOOGLE, providerId)
        if (userResult !is Success) {
            response.sendError(HttpServletResponse.SC_CONFLICT, "Could not create OAuth user")
            return
        }

        val tokenResult = tokenService.createTokenForUser(userResult.value)
        if (tokenResult !is Success) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Could not issue authentication token")
            return
        }

        val cookie =
            ResponseCookie.from(RequestTokenProcessor.COOKIE_NAME, tokenResult.value.tokenValue)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(RequestTokenProcessor.COOKIE_MAX_AGE.toLong())
                .sameSite("Lax")
                .build()

        response.addHeader("Set-Cookie", cookie.toString())
        // Redirecionamento explícito sempre de volta para o cliente Frontend no hostname correto
        // Caso faças deploy em Produção, deverás parametrizar a App URL, ex: enviroment variable FRONTEND_URL
        response.sendRedirect("http://localhost:5173/")
    }
}
