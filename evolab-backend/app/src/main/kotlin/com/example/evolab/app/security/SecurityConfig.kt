package com.example.evolab.app.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
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
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/oauth2/**",
                        "/login/**",
                        "/api/users/local",
                        "/api/users/token",
                    ).permitAll()
                    .anyRequest().permitAll() // trocar para authenticated() depois de validarmos isto
            }
            .oauth2ResourceServer { it.disable() }
            .oauth2Login { oauth2 ->
                oauth2.successHandler(oAuth2LoginSuccessHandler)
            }

        return http.build()
    }
}
