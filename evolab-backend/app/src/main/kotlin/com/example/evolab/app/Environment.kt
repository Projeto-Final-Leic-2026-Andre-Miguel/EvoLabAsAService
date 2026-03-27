package com.example.evolab.app

object Environment {


    fun getUrl() = System.getenv(DB_URL) ?: throw Exception("DB_URL environment variable is not set")

    const val DB_URL = "DB_URL"

}