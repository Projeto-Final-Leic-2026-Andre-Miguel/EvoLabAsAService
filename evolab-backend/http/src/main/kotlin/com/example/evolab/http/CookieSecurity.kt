package com.example.evolab.http

import jakarta.servlet.http.HttpServletRequest

object CookieSecurity {
    fun shouldUseSecureCookie(request: HttpServletRequest): Boolean =
        request.isSecure || request.getHeader("X-Forwarded-Proto").equals("https", ignoreCase = true)
}
