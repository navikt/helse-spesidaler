package no.nav.helse.spesidaler.api.rest_api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate
import javax.sql.DataSource
import no.nav.helse.spesidaler.api.Beløp
import no.nav.helse.spesidaler.api.GjeldendeInntekter
import no.nav.helse.spesidaler.api.Periode.Companion.til
import no.nav.helse.spesidaler.api.Personident
import org.intellij.lang.annotations.Language

private val objectmapper = jacksonObjectMapper()
internal suspend fun ApplicationCall.requestJson() = objectmapper.readTree(receiveText())
internal suspend fun ApplicationCall.respondJson(@Language("JSON") json: String, statusCode: HttpStatusCode) = respondText(text = json, contentType = Json, status = statusCode)

internal fun Route.InntekterForBeregningApi(dataSource: () -> DataSource) {
    post("/inntekter-for-beregning") {
        val request = call.requestJson()
        val personident = Personident(request["fødselsnummer"].asText())
        val periode = request.path("InntekterForBeregning").let {
            LocalDate.parse(it.path("fom").asText()) til LocalDate.parse(it.path("tom").asText())
        }
        val gjeldendeInntekter = GjeldendeInntekter(personident, periode, dataSource())
            .inntekter
            .map { objectmapper.createObjectNode().apply {
                this.put("inntektskilde", it.kilde.id)
                this.put("fom", "${it.periode.start}")
                this.put("tom", "${it.periode.endInclusive}")
                val (beløpKey, beløpValue) = it.beløp.toJson()
                this.put(beløpKey, beløpValue)
            } }

        val responseBody = objectmapper.createObjectNode().apply {
            this.putArray("inntekter").apply {
                this.addAll(gjeldendeInntekter)
            }
        }.toString()
        call.respondJson(responseBody, OK)
    }
}

private fun Beløp.toJson() = when (this) {
    is Beløp.Daglig -> "daglig" to ører / 100.0
    is Beløp.Månedlig -> "månedlig" to ører / 100.0
    is Beløp.Periodisert -> "daglig" to daglig.ører / 100.0
    is Beløp.Årlig -> "årlig" to ører / 100.0
}
