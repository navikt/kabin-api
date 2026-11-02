package no.nav.klage.config

import com.ninjasquad.springmockk.MockkBean
import no.nav.klage.api.controller.CommonController
import no.nav.klage.auth.test.AzureSecurityContractTest
import no.nav.klage.service.GosysOppgaveService
import no.nav.klage.service.RegistreringService
import no.nav.klage.util.TokenUtil
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@WebMvcTest(CommonController::class)
@Import(SecurityConfiguration::class)
class SecurityConfigurationTest : AzureSecurityContractTest() {
    @MockkBean(relaxed = true)
    lateinit var tokenUtil: TokenUtil

    @MockkBean(relaxed = true)
    lateinit var gosysOppgaveService: GosysOppgaveService

    @MockkBean(relaxed = true)
    lateinit var registreringService: RegistreringService

    override fun protectedRequest() =
        post("/calculatefrist")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"fromDate":"2023-07-10","varsletBehandlingstidUnits":2,"varsletBehandlingstidUnitTypeId":"1"}""")
}
