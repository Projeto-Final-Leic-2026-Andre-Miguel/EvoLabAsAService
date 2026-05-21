package com.example.evolab.repo.repoStatistics

import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.domain.statistics.UserStatistics
import org.jdbi.v3.core.Handle

class RepositoryStatisticsJdbi(
    private val handle: Handle,
) : RepositoryStatistics {
    override fun findByUserId(userId: Int): UserStatistics? =
        handle
            .createQuery(StatisticsSql.FIND_BY_USER_ID)
            .bind("userId", userId)
            .map { rs, _ -> rs.toUserStatistics() }
            .findOne()
            .orElse(null)

    override fun getOrCreate(userId: Int): UserStatistics {
        handle
            .createUpdate(StatisticsSql.ENSURE_ROW)
            .bind("userId", userId)
            .execute()

        return findByUserId(userId) ?: error("Statistics for user '$userId' were created but could not be loaded")
    }

    override fun incrementProjectsCreated(
        userId: Int,
        projectId: Int,
        projectName: String,
        projectCreatedAt: java.time.Instant,
    ): UserStatistics {
        handle
            .createUpdate(StatisticsSql.INCREMENT_PROJECTS_CREATED)
            .bind("userId", userId)
            .bind("projectId", projectId)
            .bind("projectName", projectName)
            .bind("projectCreatedAt", projectCreatedAt)
            .execute()

        return getOrCreate(userId)
    }

    override fun incrementProjectsExecuted(userId: Int): UserStatistics {
        handle
            .createUpdate(StatisticsSql.INCREMENT_PROJECTS_EXECUTED)
            .bind("userId", userId)
            .execute()

        return getOrCreate(userId)
    }

    override fun incrementProjectOutcome(userId: Int, status: EvolutionStatus): UserStatistics {
        val sql =
            when (status) {
                EvolutionStatus.COMPLETED -> StatisticsSql.INCREMENT_PROJECT_SUCCESS
                EvolutionStatus.FAILED -> StatisticsSql.INCREMENT_PROJECT_FAILURE
                else -> return getOrCreate(userId)
            }

        handle
            .createUpdate(sql)
            .bind("userId", userId)
            .execute()

        return getOrCreate(userId)
    }

    override fun incrementCredentialsCreated(userId: Int): UserStatistics {
        handle
            .createUpdate(StatisticsSql.INCREMENT_CREDENTIALS_CREATED)
            .bind("userId", userId)
            .execute()

        return getOrCreate(userId)
    }

    override fun incrementConfigsCreated(userId: Int): UserStatistics {
        handle
            .createUpdate(StatisticsSql.INCREMENT_CONFIGS_CREATED)
            .bind("userId", userId)
            .execute()

        return getOrCreate(userId)
    }

    override fun findById(id: Int): UserStatistics? = findByUserId(id)

    override fun findAll(): List<UserStatistics> =
        handle
            .createQuery(StatisticsSql.FIND_ALL)
            .map { rs, _ -> rs.toUserStatistics() }
            .list()

    override fun save(entity: UserStatistics) {
        handle
            .createUpdate(StatisticsSql.SAVE)
            .bind("userId", entity.userId)
            .bind("projectsCreated", entity.projectsCreated)
            .bind("projectsExecuted", entity.projectsExecuted)
            .bind("projectsSucceeded", entity.projectsSucceeded)
            .bind("projectsFailed", entity.projectsFailed)
            .bind("credentialsCreated", entity.credentialsCreated)
            .bind("configsCreated", entity.configsCreated)
            .bind("lastProjectId", entity.lastProjectId)
            .bind("lastProjectName", entity.lastProjectName)
            .bind("lastProjectCreatedAt", entity.lastProjectCreatedAt)
            .bind("updatedAt", entity.updatedAt)
            .execute()
    }

    override fun deleteById(id: Int): Boolean =
        handle
            .createUpdate(StatisticsSql.DELETE_BY_ID)
            .bind("userId", id)
            .execute() > 0

    override fun clear() {
        handle.createUpdate(StatisticsSql.CLEAR).execute()
    }

    private fun java.sql.ResultSet.toUserStatistics(): UserStatistics =
        UserStatistics(
            userId = getInt("userId"),
            projectsCreated = getInt("projectsCreated"),
            projectsExecuted = getInt("projectsExecuted"),
            projectsSucceeded = getInt("projectsSucceeded"),
            projectsFailed = getInt("projectsFailed"),
            credentialsCreated = getInt("credentialsCreated"),
            configsCreated = getInt("configsCreated"),
            lastProjectId = getInt("lastProjectId").let { if (wasNull()) null else it },
            lastProjectName = getString("lastProjectName"),
            lastProjectCreatedAt = getTimestamp("lastProjectCreatedAt")?.toInstant(),
            updatedAt = getTimestamp("updatedAt").toInstant(),
        )
}
