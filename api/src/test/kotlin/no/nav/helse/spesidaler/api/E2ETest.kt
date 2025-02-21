package no.nav.helse.spesidaler.api

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.naisful.NaisEndpoints
import com.github.navikt.tbd_libs.naisful.test.TestContext
import com.github.navikt.tbd_libs.naisful.test.naisfulTestApp
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.junit.jupiter.api.Test

class E2ETest {

    @Test
    fun `starter app`() {
        spesidalerTestApp() {}
    }

    private fun spesidalerTestApp(testblokk: suspend TestContext.() -> Unit) {
        naisfulTestApp(
            testApplicationModule = {
                routing {
                    api()
                }
            },
            objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule()),
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            naisEndpoints = NaisEndpoints.Default,
            callIdHeaderName = "callId",
            testblokk = testblokk
        )
    }
}

//suspend fun TestContext.sendPersonRequest(ident: String): HttpResponse {
//    return client.post("/api/person") {
//        contentType(ContentType.Application.Json)
//        setBody(IdentRequest(ident = ident))
//    }
