package no.nav.klage.util

import java.time.LocalDate

/**
 * Saksnummeret Trygderetten gir en anke. It consists of the four digit year the case was registered in
 * Trygderetten, directly followed by a running counter, e.g. 2026123 (year 2026, counter 123).
 *
 * The value is split into saksaar and sakssekvensnummer further down the chain (kabal-document puts them
 * in the avtalemelding), so the four leading digits must always be the year.
 */
object TrygderettenSaksnummer {
    /** The four leading digits are the year, and at least one digit of counter must follow. */
    private val PATTERN = Regex("^\\d{5,}$")

    /** Trygderetten has no case numbers older than this, so anything before it is a typo. */
    private const val EARLIEST_YEAR = 1970

    fun isValid(
        value: String,
        today: LocalDate = LocalDate.now(),
    ): Boolean {
        if (!PATTERN.matches(value)) {
            return false
        }

        // The whole saksnummer must fit in an Int, which is what the receiving systems use.
        if (value.toIntOrNull() == null) {
            return false
        }

        val year = value.take(4).toInt()
        if (year < EARLIEST_YEAR || year > today.year + 1) {
            return false
        }

        // A counter of only zeroes is not a real case.
        return value.drop(4).toInt() > 0
    }
}
