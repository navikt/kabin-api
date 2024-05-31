package no.nav.klage.api.controller

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.klage.api.controller.view.CalculateFristInput
import no.nav.klage.service.*
import no.nav.klage.util.AuditLogger
import no.nav.klage.util.TokenUtil
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
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
    lateinit var pdfService: PDFService

    @MockkBean
    lateinit var auditLogger: AuditLogger

    @MockkBean
    lateinit var oppgaveService: OppgaveService

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private val calculateFristInput = CalculateFristInput(
        fromDate = LocalDate.of(2023, 7, 10),
        fristInWeeks = 2
    )

    @BeforeEach
    fun setup() {
        every { tokenUtil.getCurrentIdent() } returns "W132204"
    }

    @Test
    fun calculateFrist() {
        mockMvc.perform(
            post("/calculatefrist").content(mapper.writeValueAsString(calculateFristInput))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("\"2023-07-24\""))
    }
}