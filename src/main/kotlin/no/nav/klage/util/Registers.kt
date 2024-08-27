package no.nav.klage.util

import no.nav.klage.kodeverk.Fagsystem

enum class MulighetSource(val fagsystem: Fagsystem){
    INFOTRYGD(Fagsystem.IT01),
    KABAL(Fagsystem.KABAL);

    companion object {
        fun of(fagsystem: Fagsystem): MulighetSource {
            return entries.firstOrNull { it.fagsystem == fagsystem }
                ?: throw IllegalArgumentException("No AnkemulighetSource with fagsystem $fagsystem exists")
        }
    }
}
