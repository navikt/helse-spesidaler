package no.nav.helse.spesidaler.api.rest_api

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import java.time.LocalDate
import javax.sql.DataSource
import kotlin.math.roundToInt
import no.nav.helse.spesidaler.api.Beløp
import no.nav.helse.spesidaler.api.Inntektsendringer
import no.nav.helse.spesidaler.api.Inntektskilde
import no.nav.helse.spesidaler.api.Personident
import no.nav.helse.spesidaler.api.ÅpenPeriode

internal fun Route.InntektsendringerApi(dataSource: () -> DataSource) {
    post("/inntektsendringer") {
        val request = call.requestJson()
        val personident = Personident(request["fødselsnummer"].asText())
        val inntektsendringer = request.path("inntektsendringer").map { inntektsendring ->
            Inntektsendringer.Inntektsendring(
                kilde = Inntektskilde(inntektsendring["inntektskilde"].asText()),
                nullstill = inntektsendring.path("nullstill").map { nullstillingsperiode ->
                    nullstillingsperiode.åpenPeriode()
                },
                inntekter = inntektsendring.path("inntekter").map { inntektsperiode ->
                    Inntektsendringer.Inntektsperiode(
                        periode = inntektsperiode.åpenPeriode(),
                        beløp = inntektsperiode.beløp()
                    )
                }
            )
        }
        val førsteDato = inntektsendringer.førsteDato() ?: throw BadRequestException("Fant ingen inntektsendringer i requesten.")
        Inntektsendringer(personident, inntektsendringer, dataSource())
        call.respondJson("""{"fom": "$førsteDato"}""", Created)
    }
}

private fun JsonNode.åpenPeriode() = ÅpenPeriode(
    fom = LocalDate.parse(path("fom").asText()),
    tom = path("tom").takeIf { it.isTextual }?.let { LocalDate.parse(it.asText()) }
)
private fun JsonNode.ører() = this.takeIf { it.isNumber }?.asDouble()?.let { (it * 100.0).roundToInt() }
private fun JsonNode.beløp(): Beløp {
    val inntekter = listOfNotNull(
        path("dagsbeløp").ører()?.let { Beløp.Daglig(it) },
        path("månedsbeløp").ører()?.let { Beløp.Månedlig(it)},
        path("årsbeløp").ører()?.let { Beløp.Årlig(it) },
        path("periodebeløp").ører()?.let { Beløp.Periodisert(it, åpenPeriode().lukketPeriode()) }
    )
    check(inntekter.size == 1) { "Det er opplyst om ${inntekter.size} beløp, forventet bare ett!"}
    return inntekter.single()
}
private fun Inntektsendringer.Inntektsendring.førsteDato() = listOfNotNull(nullstill.minOfOrNull { it.fom }, inntekter.minOfOrNull { it.periode.fom }).minOrNull()
private fun List<Inntektsendringer.Inntektsendring>.førsteDato() =  mapNotNull { it.førsteDato() }.minOrNull()
