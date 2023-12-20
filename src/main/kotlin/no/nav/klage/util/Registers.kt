package no.nav.klage.util

import no.nav.klage.kodeverk.Fagsystem

enum class AnkemulighetSource(val fagsystem: Fagsystem){
    INFOTRYGD(Fagsystem.IT01),
    KABAL(Fagsystem.KABAL);

    companion object {
        fun of(fagsystem: Fagsystem): AnkemulighetSource {
            return values().firstOrNull { it.fagsystem == fagsystem }
                ?: throw IllegalArgumentException("No AnkemulighetSource with fagsystem $fagsystem exists")
        }
    }
}
