package no.nav.klage.config

import no.nav.klage.util.getLogger
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.util.concurrent.TimeUnit
import javax.cache.CacheManager
import javax.cache.configuration.MutableConfiguration
import javax.cache.expiry.CreatedExpiryPolicy
import javax.cache.expiry.Duration

@EnableCaching
@Configuration
class CacheWithJCacheConfiguration(private val environment: Environment) : JCacheManagerCustomizer {

    companion object {

        const val GJELDER_CACHE = "gjelder"
        const val OPPGAVETYPE_CACHE = "oppgavetype"

        val cacheKeys =
            listOf(
                GJELDER_CACHE,
                OPPGAVETYPE_CACHE,
            )

        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    override fun customize(cacheManager: CacheManager) {
        cacheKeys.forEach { cacheName ->
            cacheManager.createCache(cacheName, cacheConfiguration(standardDuration()))
        }
    }

    private fun cacheConfiguration(duration: Duration) =
        MutableConfiguration<Any, Any>()
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(duration))
            .setStoreByValue(false)
            .setStatisticsEnabled(true)

    private fun standardDuration() =
        if (environment.activeProfiles.contains("prod-gcp")) {
            Duration(TimeUnit.HOURS, 8L)
        } else {
            Duration(TimeUnit.MINUTES, 10L)
        }

}