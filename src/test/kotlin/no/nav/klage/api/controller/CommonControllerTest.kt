package no.nav.klage.api.controller

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.klage.api.controller.view.CalculateFristInput
import no.nav.klage.service.*
import no.nav.klage.util.AuditLogger
import no.nav.klage.util.TokenUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(CommonController::class)
@ActiveProfiles("local")
class CommonControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var tokenUtil: TokenUtil

    @MockkBean
    lateinit var documentService: DocumentService

    @MockkBean
    lateinit var kabalApiService: KabalApiService

    @MockkBean
    lateinit var dokArkivService: DokArkivService

    @MockkBean
    lateinit var registreringService: RegistreringService

    @MockkBean
    lateinit var auditLogger: AuditLogger

    @MockkBean
    lateinit var gosysOppgaveService: GosysOppgaveService

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private val calculateFristInputWeeks = CalculateFristInput(
        fromDate = LocalDate.of(2023, 7, 10),
        varsletBehandlingstidUnits = 2,
        varsletBehandlingstidUnitTypeId = "1",
        varsletBehandlingstidUnitType = null
    )

    private val calculateFristInputMonths = CalculateFristInput(
        fromDate = LocalDate.of(2023, 7, 10),
        varsletBehandlingstidUnits = 6,
        varsletBehandlingstidUnitTypeId = "2",
        varsletBehandlingstidUnitType = null
    )

    @BeforeEach
    fun setup() {
        every { tokenUtil.getCurrentIdent() } returns "W132204"
    }

    @Test
    fun calculateFristWeeks() {
        mockMvc.perform(
            post("/calculatefrist").content(mapper.writeValueAsString(calculateFristInputWeeks))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("\"2023-07-24\""))
    }

    @Test
    fun calculateFrist() {
        mockMvc.perform(
            post("/calculatefrist").content(mapper.writeValueAsString(calculateFristInputMonths))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("\"2024-01-10\""))
    }
}