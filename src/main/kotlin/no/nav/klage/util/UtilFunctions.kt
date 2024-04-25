package no.nav.klage.util

import no.nav.klage.clients.saf.graphql.Datotype
import no.nav.klage.clients.saf.graphql.Journalpost
import no.nav.klage.clients.saf.graphql.Journalposttype
import no.nav.klage.clients.saf.graphql.Journalstatus
import java.time.LocalDateTime

fun canChangeAvsenderInJournalpost(
    journalpost: Journalpost,
): Boolean {
    val datoJournalfoert =
        journalpost.relevanteDatoer?.find { it.datotype == Datotype.DATO_JOURNALFOERT }?.dato
    val cannotChange = journalpost.journalposttype == Journalposttype.I
            && journalpost.journalstatus == Journalstatus.JOURNALFOERT
            && datoJournalfoert?.isBefore(LocalDateTime.now().minusYears(1)) == true

    return !cannotChange
}