package com.example.evolab.repo.repoProject

import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.domain.project.Project
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo

class RepositoryProjectJdbi(
    private val handle: Handle,
) : RepositoryProject {

    override fun createProject(
        userId: Int,
        name: String,
        description: String?,
        configId: Int?,
        initialProgram: String?,
        evaluatorCode: String?,
        status: EvolutionStatus,
    ): Project {
        val id =
            handle
                .createQuery(ProjectSql.CREATE_PROJECT)
                .bind("userId", userId)
                .bind("name", name)
                .bind("description", description)
                .bind("configId", configId)
                .bind("initialProgram", initialProgram)
                .bind("evaluatorCode", evaluatorCode)
                .bind("status", status.name)
                .mapTo<Int>()
                .one()

        return findById(id) ?: error("Project with id '$id' was created but could not be loaded")
    }



    override fun findAllByUserId(userId: Int): List<Project> =
        handle
            .createQuery(ProjectSql.FIND_ALL_BY_USER_ID)
            .bind("userId", userId)
            .map { rs, _ -> rs.toProject() }
            .list()

    override fun findAllByConfigId(configId: Int): List<Project> =
        handle
            .createQuery(ProjectSql.FIND_ALL_BY_CONFIG_ID)
            .bind("configId", configId)
            .map { rs, _ -> rs.toProject() }
            .list()

    override fun findAllByStatus(status: EvolutionStatus): List<Project> =
        handle
            .createQuery(ProjectSql.FIND_ALL_BY_STATUS)
            .bind("status", status.name)
            .map { rs, _ -> rs.toProject() }
            .list()

    override fun findAllByName(name: String): List<Project> =
        handle
            .createQuery(ProjectSql.FIND_ALL_BY_NAME)
            .bind("name", name)
            .map { rs, _ -> rs.toProject() }
            .list()

    override fun findById(id: Int): Project? =
        handle
            .createQuery(ProjectSql.FIND_BY_ID)
            .bind("id", id)
            .map { rs, _ -> rs.toProject() }
            .findOne()
            .orElse(null)

    override fun findAll(): List<Project> =
        handle
            .createQuery(ProjectSql.FIND_ALL)
            .map { rs, _ -> rs.toProject() }
            .list()

    override fun save(entity: Project) {
        handle
            .createUpdate(ProjectSql.SAVE)
            .bind("id", entity.id)
            .bind("userId", entity.userId)
            .bind("configId", entity.configId)
            .bind("name", entity.name)
            .bind("description", entity.description)
            .bind("initialProgram", entity.initialProgram)
            .bind("evaluatorCode", entity.evaluatorCode)
            .bind("status", entity.status.name)
            .execute()
    }

    override fun deleteById(id: Int): Boolean =
        handle
            .createUpdate(ProjectSql.DELETE_BY_ID)
            .bind("id", id)
            .execute() > 0

    override fun clear() {
        handle
            .createUpdate(ProjectSql.CLEAR)
            .execute()
    }

    private fun java.sql.ResultSet.toProject(): Project =
        Project(
            id = getInt("id"),
            userId = getInt("userId"),
            configId = getInt("configId").let { if (wasNull()) null else it },
            name = getString("name"),
            description = getString("description"),
            initialProgram = getString("initialProgram"),
            evaluatorCode = getString("evaluatorCode"),
            status = EvolutionStatus.valueOf(getString("status")),
            createdAt = getTimestamp("createdAt").toInstant(),
        )
}


