package com.example.evolab.service.statistics

sealed class StatisticsServiceErrors {
    data class UserNotFound(val message: String) : StatisticsServiceErrors()
}
