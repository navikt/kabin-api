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

        return kabalJsonToPdfClient.getSvarbrevPDF(
            SvarbrevRequest(
                title = createAnkeInputView.svarbrevInput.title,
                sakenGjelder = SvarbrevRequest.SakenGjelder(
                    name = sakenGjelder.name,
                    fnr = sakenGjelder.id,
                ),
                enhetsnavn = Enhet.entries.find { it.navn == createAnkeInputView.svarbrevInput.enhetId }!!.beskrivelse,
                ytelsenavn = Ytelse.of(createAnkeInputView.ytelseId).navn,
                fullmektigFritekst = createAnkeInputView.svarbrevInput.fullmektigFritekst,
                ankeReceivedDate = createAnkeInputView.mottattKlageinstans,
                behandlingstidInWeeks = createAnkeInputView.fristInWeeks,
            )
        )
    }
}