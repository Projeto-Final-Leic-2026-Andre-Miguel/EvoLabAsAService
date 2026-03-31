package pt.isel.http.argumentResolverandInterceptor

import com.example.evolab.domain.user.AuthenticatedUser
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.server.ResponseStatusException

@Component
class AuthenticatedUserArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter) = parameter.parameterType == AuthenticatedUser::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? {
        val request =
            webRequest.getNativeRequest(HttpServletRequest::class.java)
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required")
        return getUserFrom(request)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required")
    }

    companion object {
        private const val KEY = "AuthenticatedUserArgumentResolver"

        fun addUserTo(
            user: AuthenticatedUser,
            request: HttpServletRequest,
        ) = request.setAttribute(KEY, user)

        fun getUserFrom(request: HttpServletRequest): AuthenticatedUser? =
            request.getAttribute(KEY)?.let {
                it as? AuthenticatedUser
            }
    }
}
