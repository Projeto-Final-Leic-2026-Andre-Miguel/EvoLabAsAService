package com.example.evolab.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.example.evolab", "pt.isel"])
class EvoLabApplication

fun main(args: Array<String>) {
    runApplication<EvoLabApplication>(*args)
}

