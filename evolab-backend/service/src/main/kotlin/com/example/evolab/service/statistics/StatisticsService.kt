package com.example.evolab.service.statistics

import com.example.evolab.domain.statistics.UserStatistics
import com.example.evolab.service.auxiliary.Either

interface StatisticsService {
    fun getStatisticsForUser(userId: Int): Either<StatisticsServiceErrors, UserStatistics>
}
