package no.nav.klage.clients.pdl.grahql

data class PersonGraphqlQuery(
    val query: String,
    val variables: IdentVariables
)

data class IdentVariables(
    val ident: String,
    val grupper: List<IdentGruppe>,
)

enum class IdentGruppe{
    FOLKEREGISTERIDENT,
    AKTORID,
}



fun hentIdenterQuery(ident: String): PersonGraphqlQuery {
    val query =
        PersonGraphqlQuery::class.java.getResource("/pdl/hentIdenter.graphql").cleanForGraphql()
    return PersonGraphqlQuery(query, IdentVariables(ident = ident, grupper = listOf(IdentGruppe.FOLKEREGISTERIDENT, IdentGruppe.AKTORID)))
}