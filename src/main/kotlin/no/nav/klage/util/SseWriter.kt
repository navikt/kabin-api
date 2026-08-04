package no.nav.klage.util

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.io.PrintWriter

/**
 * Writes server-sent events to a [HttpServletResponse].
 *
 * The response is only touched on the first [send]: as long as nothing has been sent, [hasStarted] is
 * false and the caller can still let the normal error handling produce a regular (problem detail)
 * response body.
 */
class SseWriter(private val response: HttpServletResponse) {

    private var writer: PrintWriter? = null

    val hasStarted: Boolean
        get() = writer != null

    fun send(event: String, data: String) {
        val out = writer ?: start()
        out.write("event: $event\ndata: $data\n\n")
        out.flush()
    }

    private fun start(): PrintWriter {
        response.contentType = MediaType.TEXT_EVENT_STREAM_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().headerValue)
        //Tell proxies not to buffer, or the client sees nothing until everything is done.
        response.setHeader("X-Accel-Buffering", "no")
        return response.writer.also { writer = it }
    }
}
