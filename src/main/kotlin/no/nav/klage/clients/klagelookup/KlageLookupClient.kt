package no.nav.klage.clients.klagelookup

import no.nav.klage.exceptions.UserNotFoundException
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.logErrorResponse
import org.springframework.http.HttpHeaders
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono


@Component
class KlageLookupClient(
    private val klageLookupWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Retryable(
        excludes = [UserNotFoundException::class]
    )
    fun getUserInfo(
        navIdent: String,
    ): ExtendedUserResponse {
        return runWithTimingAndLogging {
            val token = getCorrectBearerToken()
            klageLookupWebClient.get()
                .uri("/users/$navIdent")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    token,
                )
                .exchangeToMono { response ->
                    if (response.statusCode().value() == 404) {
                        logger.debug("User $navIdent not found")
                        Mono.error(UserNotFoundException("User $navIdent not found"))
                    } else if (response.statusCode().isError) {
                        logErrorResponse(
                            response = response,
                            functionName = ::getUserInfo.name,
                            classLogger = logger,
                        )
                        response.createError()
                    } else {
                        response.bodyToMono<ExtendedUserResponse>()
                    }
                }
                .block() ?: throw RuntimeException("Could not get user info for $navIdent")
        }
    }

    fun <T> runWithTimingAndLogging(block: () -> T): T {
        val start = System.currentTimeMillis()
        try {
            return block.invoke()
        } finally {
            val end = System.currentTimeMillis()
            logger.debug("Time it took to call KlageLookup: ${end - start} millis")
        }
    }

    private fun getCorrectBearerToken(): String {
        return when (tokenUtil.getCurrentTokenType()) {
            TokenUtil.TokenType.OBO -> "Bearer ${tokenUtil.getOnBehalfOfTokenWithKlageLookupScope()}"
            TokenUtil.TokenType.CC, TokenUtil.TokenType.UNAUTHENTICATED -> "Bearer ${tokenUtil.getMaskinTilMaskinTokenWithKlageLookupScope()}"
        }
    }

}