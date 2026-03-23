package com.example.evolab.domain.config

import java.time.Instant


data class Config(
   val id : Int,
   val userId : Int,
   val llmCredentialsId : Int,
   val modelName : String,
   val maxIter : Int,
   val checkPointInterval : Int,
   val additionalParams : Map<String, String>,
   val createdAt : Instant

)
