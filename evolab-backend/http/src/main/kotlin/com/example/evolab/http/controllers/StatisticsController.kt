package com.example.evolab.http.controllers

import com.example.evolab.domain.user.AuthenticatedUser
import com.example.evolab.service.auxiliary.Failure
import com.example.evolab.service.auxiliary.Success
import com.example.evolab.service.statistics.StatisticsService
import com.example.evolab.service.statistics.StatisticsServiceErrors
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/statistics")
class StatisticsController(
    private val statisticsService: StatisticsService,
) {
    @GetMapping("/me")
    fun getMyStatistics(authenticatedUser: AuthenticatedUser): ResponseEntity<*> =
        when (val result = statisticsService.getStatisticsForUser(authenticatedUser.user.id)) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }

    private fun mapServiceErrors(error: StatisticsServiceErrors): ResponseEntity<*> =
        when (error) {
            is StatisticsServiceErrors.UserNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(error.message)
        }
}
