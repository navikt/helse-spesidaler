package no.nav.helse.spesidaler.api

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.naisful.test.TestContext
import com.github.navikt.tbd_libs.naisful.test.naisfulTestApp
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ApiTest {
    private val inntektertjeneste = mockk<Inntektertjeneste>()

    @BeforeEach
    fun clearMocks() {
        io.mockk.clearMocks(inntektertjeneste)
    }

    @Test
    fun `hent inntekt`() = e2e(inntektertjeneste) {
        every {
            inntektertjeneste.hentGjeldendeInntekter(eq(GjeldendeInntekterRequest("fnr", 1.januar, 31.januar)))
        } returns setOf(
            GjeldendeInntekter.GjeldendeInntekt(
            kilde = "orgnr",
            periode = 10.januar til 31.januar,
            beløp = GjeldendeInntekter.Beløp.Månedlig(40000)
        ))

        client.post("/api/inntekter/gjeldende") {
            contentType(Json)
            setBody(mapOf(
                "fødselsnummer" to "fnr",
                "fom" to 1.januar,
                "tom" to 31.januar,
            ))
        }.also { response ->
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<GjeldendeInntekterResponse>()
            assertEquals("fnr", responseBody.fødselsnummer)
            val inntekt = responseBody.gjeldendeInntekter.first()
            assertEquals("orgnr", inntekt.kilde)
            assertEquals(10.januar, inntekt.fom)
            assertEquals(31.januar, inntekt.tom)
            assertEquals(40000, inntekt.ører)
            assertEquals(GjeldendeInntekterResponse.GjeldendeInntekt.Oppløsning.Månedlig, inntekt.oppløsning)
        }
    }

    @Test
    fun `hent inntekt, men på en dum måte`() = e2e(inntektertjeneste) {
        client.post("/api/inntekter/gjeldende") {
            contentType(Json)
            setBody(mapOf(
                "fødselsnummer" to "fnr",
                "fom" to 1.januar,
            ))
        }.also { response ->
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    private fun e2e(inntektertjeneste: Inntektertjeneste, testblokk: suspend TestContext.() -> Unit) {
        naisfulTestApp(
            testApplicationModule = {
                routing {
                    api(inntektertjeneste)
                }
            },
            objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule()),
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            testblokk = testblokk
        )
    }
}
