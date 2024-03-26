package no.nav.klage.service

import no.nav.klage.api.controller.view.CreateAnkeInputView
import no.nav.klage.api.controller.view.SearchPartInput
import no.nav.klage.clients.kabalapi.KabalApiClient
import no.nav.klage.clients.kabaljsontopdf.KabalJsonToPdfClient
import no.nav.klage.clients.kabaljsontopdf.domain.SvarbrevRequest
import no.nav.klage.kodeverk.Enhet
import no.nav.klage.util.ValidationUtil
import org.springframework.stereotype.Service

@Service
class PDFService(
    private val kabalJsonToPdfClient: KabalJsonToPdfClient,
    private val validationUtil: ValidationUtil,
    private val kabalApiClient: KabalApiClient,
) {

    fun getSvarbrevPDF(createAnkeInputView: CreateAnkeInputView): ByteArray {
        val ankeInput = validationUtil.validateCreateAnkeInputView(createAnkeInputView)

        if (createAnkeInputView.svarbrevInput == null) {
            throw RuntimeException("SvarbrevInput is null")
        }

        val sakenGjelder = kabalApiClient.searchPart(SearchPartInput(createAnkeInputView.sakenGjelder!!.id))

        return kabalJsonToPdfClient.getSvarbrevPDF(
            SvarbrevRequest(
                sakenGjelder = SvarbrevRequest.SakenGjelder(
                    name = sakenGjelder.name ?: throw RuntimeException("Name is null"),
                    fnr = sakenGjelder.id,
                ),
                enhetsnavn = Enhet.valueOf(createAnkeInputView.svarbrevInput.enhetId).beskrivelse,
                fullmektigFritekst = createAnkeInputView.svarbrevInput.fullmektigFritekst,
                ankeReceivedDate = ankeInput.mottattKlageinstans,
                behandlingstidInWeeks = ankeInput.fristInWeeks,
            )
        )
    }
}