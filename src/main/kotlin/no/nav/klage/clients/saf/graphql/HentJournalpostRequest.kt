package no.nav.klage.clients.saf.graphql

data class HentJournalpostGraphqlQuery(
    val query: String,
    val variables: JournalpostVariables
)

data class JournalpostVariables(val journalpostId: String)

fun hentJournalpostQuery(
    journalpostId: String
): HentJournalpostGraphqlQuery {
    val journalpostProperties = HentJournalpostGraphqlQuery::class.java.getResource("/saf/journalpostProperties.txt")
        .readText()
    val query =
        HentJournalpostGraphqlQuery::class.java.getResource("/saf/hentJournalpost.graphql")
            .readText()
            .replace("<replace>", journalpostProperties)
            .replace("[\n\r]", "")
    return HentJournalpostGraphqlQuery(query, JournalpostVariables(journalpostId))
}