package com.example.evolab.service.statistics

import com.example.evolab.domain.statistics.UserStatistics
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.auxiliary.success
import jakarta.inject.Named

@Named
class StatisticsServiceImp(
    private val trxManager: TransactionManager,
) : StatisticsService {
    override fun getStatisticsForUser(userId: Int): Either<StatisticsServiceErrors, UserStatistics> =
        trxManager.run {
            repoUsers.findById(userId)
                ?: return@run failure(StatisticsServiceErrors.UserNotFound("User with id '$userId' was not found"))

            success(repoStatistics.getOrCreate(userId))
        }
}
