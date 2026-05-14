package com.example.evolab.app.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.beans.factory.annotation.Value
import com.example.evolab.service.security.EncryptionService
import com.example.evolab.service.security.EncryptionServiceImpl

@Configuration
class SecurityConfig(
    private val oAuth2LoginSuccessHandler: OAuth2LoginSuccessHandler,
) {

    @Bean
    fun encryptionService(
        @Value("\${encryption.secret-key}") secretKey: String
    ): EncryptionService {
        return EncryptionServiceImpl(secretKey)
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val csrfRequestHandler = CsrfTokenRequestAttributeHandler()
        csrfRequestHandler.setCsrfRequestAttributeName("_csrf")

        http
            .csrf { csrf ->
                csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(csrfRequestHandler)
                    .ignoringRequestMatchers(
                        "/oauth2/**",
                        "/login/**",
                        "/api/users/local",
                        "/api/users/token",
                    )
            }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(
                        "/oauth2/**",
                        "/login/**",
                        "/api/users/local",
                        "/api/users/token",
                    ).permitAll()
                    .anyRequest().permitAll()
            }
            .oauth2ResourceServer { it.disable() }
            .oauth2Login { oauth2 ->
                oauth2.successHandler(oAuth2LoginSuccessHandler)
            }
            .headers { headers ->
                headers.contentTypeOptions { }
                headers.frameOptions { frameOptions -> frameOptions.deny() }
                headers.referrerPolicy { referrerPolicy ->
                    referrerPolicy.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                }
                headers.httpStrictTransportSecurity { hsts ->
                    hsts.includeSubDomains(true).maxAgeInSeconds(31536000)
                }
            }
            .addFilterAfter(CsrfCookieFilter(), BasicAuthenticationFilter::class.java)

        return http.build()
    }
}
