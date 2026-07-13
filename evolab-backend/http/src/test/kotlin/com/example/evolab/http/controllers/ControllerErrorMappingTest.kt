package com.example.evolab.http.controllers

import com.example.evolab.domain.user.AuthProvider
import com.example.evolab.domain.user.AuthenticatedUser
import com.example.evolab.domain.user.User
import com.example.evolab.http.model.config.UpdateConfigInput
import com.example.evolab.http.model.problem.Problem
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.configService.ConfigError
import com.example.evolab.service.configService.ConfigService
import com.example.evolab.service.project.ProjectService
import com.example.evolab.service.project.ProjectServiceErrors
import java.lang.reflect.Proxy
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ControllerErrorMappingTest {
    @Test
    fun `config update reports checkpoint above max iterations clearly`() {
        val configService = proxy<ConfigService> { method ->
            if (method.name == "updateConfig") failure(ConfigError.CheckpointIntervalExceedsMaxIterations) else unused()
        }
        val controller = ConfigController(configService, proxy { unused() })

        val response = controller.updateConfig(
            id = 3,
            input = UpdateConfigInput("gpt-4o-mini", 4, 5, emptyMap()),
            authenticatedUser = authenticatedUser(),
        )

        assertEquals(400, response.statusCode.value())
        assertEquals(
            "Checkpoint interval cannot be greater than the maximum number of iterations.",
            (response.body as Problem).detail,
        )
    }

    @Test
    fun `active project delete returns conflict with clear message`() {
        val projectService = proxy<ProjectService> { method ->
            if (method.name == "deleteProject") {
                failure(ProjectServiceErrors.InvalidProjectStatus("A project with an active execution cannot be deleted."))
            } else {
                unused()
            }
        }
        val controller = ProjectController(projectService)

        val response = controller.deleteProject(3, authenticatedUser())

        assertEquals(409, response.statusCode.value())
        assertEquals(
            "A project with an active execution cannot be deleted.",
            (response.body as Problem).detail,
        )
    }

    private fun authenticatedUser() =
        AuthenticatedUser(
            user = User(1, "User", "user@test.dev", null, AuthProvider.LOCAL, "LOCAL", Instant.now()),
            token = "token",
        )

    private inline fun <reified T> proxy(crossinline handler: (java.lang.reflect.Method) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            when (method.name) {
                "toString" -> "${T::class.simpleName}TestProxy"
                "hashCode" -> 1
                "equals" -> false
                else -> handler(method)
            }
        } as T

    private fun unused(): Nothing = error("Unexpected service call")
}
