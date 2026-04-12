package com.example.evolab.service.jobExecution

import com.example.evolab.domain.project.Project
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.auxiliary.success
import jakarta.inject.Named
import kotlinx.coroutines.channels.Channel

private const val CAPACITY = 100

@Named
class JobQueue {

    private val queue = Channel<Project>(CAPACITY)

    fun enqueue(project: Project): Either<String, Unit> {
        val result = queue.trySend(project)

        return if (result.isSuccess) {
            success(Unit)
        } else {
            failure("A fila esta cheia. O sistema esta sobrecarregado. Project ID: ${project.id}")
        }
    }

    suspend fun dequeue(): Project {
        return queue.receive()
    }
}
