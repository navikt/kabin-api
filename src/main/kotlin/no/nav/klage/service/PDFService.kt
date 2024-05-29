package no.nav.klage.service

import no.nav.klage.api.controller.view.PreviewAnkeSvarbrevInput
import no.nav.klage.api.controller.view.SearchPartInput
import no.nav.klage.clients.KabalInnstillingerClient
import no.nav.klage.clients.kabaljsontopdf.KabalJsonToPdfClient
import no.nav.klage.clients.kabaljsontopdf.domain.SvarbrevRequest
import no.nav.klage.kodeverk.Enhet
import no.nav.klage.kodeverk.Ytelse
import org.springframework.stereotype.Service

@Service
class PDFService(
    private val kabalJsonToPdfClient: KabalJsonToPdfClient,
    private val kabalApiService: KabalApiService,
    private val kabalInnstillingerClient: KabalInnstillingerClient,
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
                ytelsenavn = Ytelse.of(createAnkeInputView.ytelseId).navn,
                fullmektigFritekst = createAnkeInputView.svarbrevInput.fullmektigFritekst,
                ankeReceivedDate = createAnkeInputView.mottattKlageinstans,
                behandlingstidInWeeks = createAnkeInputView.fristInWeeks,
                avsenderEnhetId = kabalInnstillingerClient.getBrukerdata().ansattEnhet.id
            )
        )
    }
}