package no.nav.klage.clients.azure

import com.fasterxml.jackson.annotation.JsonIgnoreProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class AzureUserList(val value: List<AzureUser>?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AzureUser(
    val onPremisesSamAccountName: String,
    val displayName: String,
    val givenName: String,
    val surname: String,
    val userPrincipalName: String,
    val streetAddress: String,
)