package com.example.evolab.domain.statistics

import java.time.Instant

data class UserStatistics(
    val userId: Int,
    val projectsCreated: Int,
    val projectsExecuted: Int,
    val projectsSucceeded: Int,
    val projectsFailed: Int,
    val credentialsCreated: Int,
    val configsCreated: Int,
    val lastProjectId: Int?,
    val lastProjectName: String?,
    val lastProjectCreatedAt: Instant?,
    val updatedAt: Instant,
)
