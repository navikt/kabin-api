package no.nav.klage.util

import no.nav.klage.exceptions.IllegalInputException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class DokumentNameUtilTest {

    @Test
    fun `renaming takes the name exactly as given`() {
        assertThat(validateDokumentName("nytt")).isEqualTo("nytt")
    }

    @Test
    fun `renaming can change the extension`() {
        assertThat(validateDokumentName("nytt.jpg")).isEqualTo("nytt.jpg")
    }

    @Test
    fun `renaming trims surrounding whitespace`() {
        assertThat(validateDokumentName("  nytt.pdf  ")).isEqualTo("nytt.pdf")
    }

    @Test
    fun `renaming to a blank name fails`() {
        assertThatThrownBy { validateDokumentName("  ") }
            .isInstanceOf(IllegalInputException::class.java)
    }

    @Test
    fun `renaming to a too long name fails`() {
        assertThatThrownBy { validateDokumentName("a".repeat(200)) }
            .isInstanceOf(IllegalInputException::class.java)
    }

    @Test
    fun `serving a document adds pdf to a name with another extension`() {
        assertThat(withPdfExtension(name = "bilde.jpg")).isEqualTo("bilde.jpg.pdf")
    }

    @Test
    fun `serving a document adds pdf when there is no extension`() {
        assertThat(withPdfExtension(name = "bilde")).isEqualTo("bilde.pdf")
    }

    @Test
    fun `serving a document keeps an existing pdf extension`() {
        assertThat(withPdfExtension(name = "bilde.pdf")).isEqualTo("bilde.pdf")
    }

    @Test
    fun `serving a document keeps an existing pdf extension regardless of case`() {
        assertThat(withPdfExtension(name = "bilde.PDF")).isEqualTo("bilde.PDF")
    }

    @Test
    fun `serving a document keeps a long name`() {
        assertThat(withPdfExtension(name = "a".repeat(250)))
            .isEqualTo("a".repeat(250) + ".pdf")
    }
}
