package com.example.evolab.http.controllers

import com.example.evolab.domain.user.AuthenticatedUser
import com.example.evolab.http.model.user.CreateLocalUserInput
import com.example.evolab.http.model.user.CreateOAuthUserInput
import com.example.evolab.http.model.user.CreateTokenInput
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.Failure
import com.example.evolab.service.auxiliary.Success
import com.example.evolab.service.tokenService.TokenError
import com.example.evolab.service.tokenService.TokenService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseCookie
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.service.userService.UserAuthService
import pt.isel.service.userService.UserError

@RestController
class UserController(
	private val userService: UserAuthService,
	private val tokenService: TokenService,
) {

	@PostMapping("/api/users/local")
	fun createLocalUser(
		@RequestBody input: CreateLocalUserInput,
	): ResponseEntity<*> {
		return when (val result = userService.createLocalUser(input.name, input.email, input.password)) {
			is Success -> {
				val user = result.value
				ResponseEntity
					.status(HttpStatus.CREATED)
					.body(
						mapOf(
							"id" to user.id,
							"name" to user.name,
							"email" to user.email,
						),
					)
			}

			is Failure->
				when (result.value) {
					UserError.AlreadyUsedEmailAddress -> ResponseEntity.status(HttpStatus.CONFLICT).build<Unit>()
					UserError.InsecurePassword -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build<Unit>()
					else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Unit>()
				}
		}
	}



	@PostMapping("/api/users/token")
	fun createToken(
        @RequestBody input: CreateTokenInput,
	): ResponseEntity<*> {
		return when (val tokenInfo = tokenService.createToken(input.email, input.password)) {
			is Success -> {
				val cookie = ResponseCookie.from(pt.isel.http.argumentResolverandInterceptor.RequestTokenProcessor.COOKIE_NAME, tokenInfo.value.tokenValue)
					.httpOnly(true)
					.secure(false)
					.path("/")
					.maxAge(pt.isel.http.argumentResolverandInterceptor.RequestTokenProcessor.COOKIE_MAX_AGE.toLong())
					.sameSite("Lax")
					.build()

				ResponseEntity
					.status(HttpStatus.OK)
					.header(HttpHeaders.SET_COOKIE, cookie.toString())
					.body(mapOf("tokenValue" to tokenInfo.value.tokenValue))
			}

			is Failure -> {
				when (tokenInfo.value) {
					TokenError.InvalidCredentials -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Unit>()
					else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Unit>()
				}
			}
		}
	}

	@PostMapping("/api/users/token/generate")
	fun generateTokenForAuthenticatedUser(
		authenticatedUser: AuthenticatedUser
	): ResponseEntity<*> {
		return when (val tokenInfo = tokenService.createTokenForUser(authenticatedUser.user)) {
			is Success -> {
				ResponseEntity
					.status(HttpStatus.OK)
					.body(mapOf("tokenValue" to tokenInfo.value.tokenValue))
			}
		 is Failure -> {
				ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Unit>()
			}
		}
	}

	@PostMapping("/api/logout")
	fun logout(user: AuthenticatedUser): ResponseEntity<*> {
		return when (tokenService.revokeToken(user.token)) {
			is Success -> {
				val clearCookie = ResponseCookie.from(pt.isel.http.argumentResolverandInterceptor.RequestTokenProcessor.COOKIE_NAME, "")
					.httpOnly(true)
					.secure(false)
					.path("/")
					.maxAge(0)
					.build()

				ResponseEntity
					.status(HttpStatus.OK)
					.header(HttpHeaders.SET_COOKIE, clearCookie.toString())
					.build<Unit>()
			}
		 is Failure -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Unit>()
		}
	}

	@DeleteMapping("/api/users/{id}")
	fun deleteUser(
		@PathVariable id: Int,
	): ResponseEntity<*> {
		return when (val result = userService.deleteUser(id)) {
			is Success -> ResponseEntity.status(HttpStatus.OK).build<Unit>()
		 is Failure ->
				when (result.value) {
					UserError.UserNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).build<Unit>()
					UserError.ErrorDeletingUser -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Unit>()
					else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Unit>()
				}
		}
	}

	@GetMapping("/api/users")
	fun getAllUsers(): ResponseEntity<*> {
		val users = userService.getAllUsers()
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(users)
	}

	@GetMapping("/api/me")
	fun userHome(userAuthenticatedUser: AuthenticatedUser): ResponseEntity<*> {
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				mapOf(
					"id" to userAuthenticatedUser.user.id,
					"name" to userAuthenticatedUser.user.name,
					"email" to userAuthenticatedUser.user.email,
					"authProvider" to userAuthenticatedUser.user.authProvider,
					"createdAt" to userAuthenticatedUser.user.createdAt.toString()
			)
		)
	}
}
