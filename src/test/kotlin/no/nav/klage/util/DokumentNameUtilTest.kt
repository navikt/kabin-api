package no.nav.klage.util

import no.nav.klage.domain.entities.RegistreringDokument
import no.nav.klage.exceptions.IllegalInputException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class DokumentNameUtilTest {

    @Test
    fun `renaming keeps the current extension`() {
        assertThat(renameKeepingExtension(currentName = "gammelt.pdf", newName = "nytt"))
            .isEqualTo("nytt.pdf")
    }

    @Test
    fun `renaming does not duplicate an unchanged extension`() {
        assertThat(renameKeepingExtension(currentName = "gammelt.pdf", newName = "nytt.pdf"))
            .isEqualTo("nytt.pdf")
    }

    @Test
    fun `renaming ignores an extension supplied by the client`() {
        assertThat(renameKeepingExtension(currentName = "gammelt.pdf", newName = "nytt.jpg"))
            .isEqualTo("nytt.pdf")
    }

    @Test
    fun `a client supplied extension never survives the document being finished`() {
        val renamed = renameKeepingExtension(currentName = "gammelt", newName = "nytt.exe")
        assertThat(withExtension(name = renamed, extension = "pdf")).isEqualTo("nytt.exe.pdf")
    }

    @Test
    fun `renaming a document without an extension keeps it without one`() {
        assertThat(renameKeepingExtension(currentName = "gammelt", newName = "nytt"))
            .isEqualTo("nytt")
    }

    @Test
    fun `renaming keeps parts of a name that only look like an extension`() {
        assertThat(renameKeepingExtension(currentName = "gammelt.pdf", newName = "Rapport v1.2"))
            .isEqualTo("Rapport v1.2.pdf")
    }

    @Test
    fun `renaming to a blank name fails`() {
        assertThatThrownBy { renameKeepingExtension(currentName = "gammelt.pdf", newName = "  ") }
            .isInstanceOf(IllegalInputException::class.java)
    }

    @Test
    fun `renaming to a too long name fails`() {
        assertThatThrownBy {
            renameKeepingExtension(currentName = "gammelt.pdf", newName = "a".repeat(200))
        }.isInstanceOf(IllegalInputException::class.java)
    }

    @Test
    fun `converting to pdf replaces the extension`() {
        assertThat(withExtension(name = "bilde.jpg", extension = "pdf")).isEqualTo("bilde.pdf")
    }

    @Test
    fun `converting to pdf adds an extension when there is none`() {
        assertThat(withExtension(name = "bilde", extension = "pdf")).isEqualTo("bilde.pdf")
    }

    @Test
    fun `converting to pdf truncates instead of failing on a too long name`() {
        val result = withExtension(name = "a".repeat(250), extension = "pdf")
        assertThat(result).hasSize(RegistreringDokument.MAX_NAME_LENGTH)
        assertThat(result).endsWith(".pdf")
    }
}
