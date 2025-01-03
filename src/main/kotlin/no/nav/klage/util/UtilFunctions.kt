package no.nav.klage.util

import no.nav.klage.clients.saf.graphql.Datotype
import no.nav.klage.clients.saf.graphql.Journalpost
import no.nav.klage.clients.saf.graphql.Journalposttype
import no.nav.klage.clients.saf.graphql.Journalstatus
import no.nav.klage.kodeverk.TimeUnitType
import java.time.LocalDate
import java.time.LocalDateTime

fun canChangeAvsenderInJournalpost(
    journalpost: Journalpost,
): Boolean {
    if (journalpost.journalposttype != Journalposttype.I) {
        return false
    }

    //Avsender på digitalt innsendt journalpost kan ikke endres
    if (journalpost.kanal in listOf("NAV_NO", "NAV_NO_CHAT", "ALTINN", "EESSI")) {
        return false
    }

    val datoJournalfoert =
        journalpost.relevanteDatoer?.find { it.datotype == Datotype.DATO_JOURNALFOERT }?.dato

    val cannotChangeForInngaaende = journalpost.journalstatus == Journalstatus.JOURNALFOERT
            && datoJournalfoert?.isBefore(LocalDateTime.now().minusYears(1)) == true

    return !cannotChangeForInngaaende
}

fun calculateFrist(
    fromDate: LocalDate,
    units: Long,
    unitType: TimeUnitType
): LocalDate {
    return when(unitType) {
        TimeUnitType.WEEKS -> fromDate.plusWeeks(units)
        TimeUnitType.MONTHS -> fromDate.plusMonths(units)
    }
}