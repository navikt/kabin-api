package no.nav.klage.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun getLogger(forClass: Class<*>): Logger = LoggerFactory.getLogger(forClass)

fun getSecureLogger(): Logger = LoggerFactory.getLogger("secure")

fun getAuditLogger(): Logger = LoggerFactory.getLogger("audit")

fun logMethodDetails(methodName: String, innloggetIdent: String, logger: Logger) {
    logger.debug(
        "{} is requested by ident {}",
        methodName,
        innloggetIdent,
    )
}