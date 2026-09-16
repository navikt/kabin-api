package no.nav.klage.util

import no.nav.klage.kodeverk.Type

fun Type?.isAnke(): Boolean = this == Type.ANKE_FOER_2027 || this == Type.ANKE_ETTER_2027

val ankeTypes = listOf(Type.ANKE_FOER_2027, Type.ANKE_ETTER_2027)
