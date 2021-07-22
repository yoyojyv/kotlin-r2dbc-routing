package me.jerry.example.r2dbc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KotlinR2dbcRoutingApplication

fun main(args: Array<String>) {
	runApplication<KotlinR2dbcRoutingApplication>(*args)
}
