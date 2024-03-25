package no.nav.klage.service

import no.nav.klage.api.controller.view.SvarbrevInput
import no.nav.klage.dokument.clients.kabaljsontopdf.KabalJsonToPdfClient
import org.springframework.stereotype.Service

@Service
class PDFService(
    private val kabalJsonToPdfClient: KabalJsonToPdfClient
) {

    fun getSvarbrevPDF(svarbrevInput: SvarbrevInput): ByteArray {
        return kabalJsonToPdfClient.getSvarbrevPDF()
    }
}