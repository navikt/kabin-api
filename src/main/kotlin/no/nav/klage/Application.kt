package no.nav.klage

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.resilience.annotation.EnableResilientMethods

@SpringBootApplication
@EnableResilientMethods
class Application

fun main() {
    runApplication<Application>()
}