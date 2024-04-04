package no.nav.klage.service

import no.nav.klage.api.controller.view.PreviewAnkeSvarbrevInput
import no.nav.klage.api.controller.view.SearchPartInput
import no.nav.klage.clients.kabaljsontopdf.KabalJsonToPdfClient
import no.nav.klage.clients.kabaljsontopdf.domain.SvarbrevRequest
import no.nav.klage.kodeverk.Enhet
import no.nav.klage.kodeverk.Ytelse
import org.springframework.stereotype.Service

@Service
class PDFService(
    private val kabalJsonToPdfClient: KabalJsonToPdfClient,
    private val kabalApiService: KabalApiService,
) {

    fun getSvarbrevPDF(createAnkeInputView: PreviewAnkeSvarbrevInput): ByteArray {
        val sakenGjelder = kabalApiService.searchPart(SearchPartInput(createAnkeInputView.sakenGjelder.id))

        val klager = if (createAnkeInputView.klager != null) {
            kabalApiService.searchPart(SearchPartInput(createAnkeInputView.klager.id))
        } else null

        return kabalJsonToPdfClient.getSvarbrevPDF(
            SvarbrevRequest(
                title = createAnkeInputView.svarbrevInput.title,
                sakenGjelder = SvarbrevRequest.Part(
                    name = sakenGjelder.name,
                    fnr = sakenGjelder.id,
                ),
                klager = klager?.let {
                    SvarbrevRequest.Part(
                        name = it.name,
                        fnr = it.id,
                    )
                },
                enhetsnavn = Enhet.entries.find { it.navn == createAnkeInputView.svarbrevInput.enhetId }!!.beskrivelse,
                ytelsenavn = Ytelse.of(createAnkeInputView.ytelseId).navn.toSpecialCase(),
                fullmektigFritekst = createAnkeInputView.svarbrevInput.fullmektigFritekst,
                ankeReceivedDate = createAnkeInputView.mottattKlageinstans,
                behandlingstidInWeeks = createAnkeInputView.fristInWeeks,
            )
        )
    }

    private fun String.toSpecialCase(): String {
        val strings = this.split(" - ")
        return if (strings.size == 2) {
            strings[0].decapitalize() + " - " + strings[1].decapitalize()
        } else this
    }

    private fun String.decapitalize(): String {
        return if (!this.startsWith("NAV")) {
            this.replaceFirstChar(Char::lowercase)
        } else this
    }

}