package no.nav.klage.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TrygderettenSaksnummerTest {
    private val today = LocalDate.of(2027, 3, 1)

    @Test
    fun `accepts a saksnummer from the current year`() {
        assertThat(TrygderettenSaksnummer.isValid(value = "2027123", today = today)).isTrue
    }

    @Test
    fun `accepts a saksnummer from an earlier year`() {
        assertThat(TrygderettenSaksnummer.isValid(value = "19701", today = today)).isTrue
    }

    @Test
    fun `accepts the largest saksnummer that fits in an Int`() {
        assertThat(TrygderettenSaksnummer.isValid(value = Int.MAX_VALUE.toString(), today = LocalDate.of(2147, 1, 1))).isTrue
    }

    @Test
    fun `accepts a saksnummer from next year, since Trygderetten may be ahead of us`() {
        assertThat(TrygderettenSaksnummer.isValid(value = "2028123", today = today)).isTrue
    }

    @Test
    fun `rejects a saksnummer from a year too far ahead`() {
        assertThat(TrygderettenSaksnummer.isValid(value = "2029123", today = today)).isFalse
    }

    @Test
    fun `rejects a saksnummer from before Trygderetten started using this format`() {
        assertThat(TrygderettenSaksnummer.isValid(value = "1969123", today = today)).isFalse
    }

    @Test
    fun `rejects a saksnummer without a counter`() {
        assertThat(TrygderettenSaksnummer.isValid(value = "2027", today = today)).isFalse
    }

    @Test
    fun `rejects a counter that is zero`() {
        assertThat(TrygderettenSaksnummer.isValid(value = "20270", today = today)).isFalse
    }

    @Test
    fun `rejects a saksnummer that does not fit in an Int`() {
        assertThat(TrygderettenSaksnummer.isValid(value = "2147483648", today = LocalDate.of(2147, 1, 1))).isFalse
        assertThat(TrygderettenSaksnummer.isValid(value = "202712345678", today = today)).isFalse
    }

    @Test
    fun `rejects a saksnummer that is not a number`() {
        assertThat(TrygderettenSaksnummer.isValid(value = "2027-123", today = today)).isFalse
        assertThat(TrygderettenSaksnummer.isValid(value = " 2027123 ", today = today)).isFalse
        assertThat(TrygderettenSaksnummer.isValid(value = "", today = today)).isFalse
    }
}
