package com.example.evolab.app.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.filter.OncePerRequestFilter

class CsrfCookieFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val csrfToken =
            request.getAttribute(CsrfToken::class.java.name) as? CsrfToken
                ?: request.getAttribute("_csrf") as? CsrfToken

        csrfToken?.token
        filterChain.doFilter(request, response)
    }
}
