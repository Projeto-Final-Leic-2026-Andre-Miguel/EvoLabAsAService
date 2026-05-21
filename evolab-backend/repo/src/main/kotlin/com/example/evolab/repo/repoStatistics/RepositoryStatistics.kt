package com.example.evolab.repo.repoStatistics

import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.domain.statistics.UserStatistics
import com.example.evolab.repo.Repository
import java.time.Instant

interface RepositoryStatistics : Repository<UserStatistics> {
    fun findByUserId(userId: Int): UserStatistics?

    fun getOrCreate(userId: Int): UserStatistics

    fun incrementProjectsCreated(
        userId: Int,
        projectId: Int,
        projectName: String,
        projectCreatedAt: Instant,
    ): UserStatistics

    fun incrementProjectsExecuted(userId: Int): UserStatistics

    fun incrementProjectOutcome(userId: Int, status: EvolutionStatus): UserStatistics

    fun incrementCredentialsCreated(userId: Int): UserStatistics

    fun incrementConfigsCreated(userId: Int): UserStatistics
}
