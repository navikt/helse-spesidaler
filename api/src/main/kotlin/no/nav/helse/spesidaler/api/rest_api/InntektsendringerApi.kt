package no.nav.helse.spesidaler.api.rest_api

import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import no.nav.helse.spesidaler.api.Beløp
import no.nav.helse.spesidaler.api.Inntektsendringer
import no.nav.helse.spesidaler.api.Inntektskilde
import no.nav.helse.spesidaler.api.Personident
import no.nav.helse.spesidaler.api.ÅpenPeriode
import tools.jackson.databind.JsonNode
import java.time.LocalDate
import javax.sql.DataSource
import kotlin.math.roundToInt

internal fun Route.InntektsendringerApi(dataSource: () -> DataSource) {
    post("/inntektsendringer") {
        val request = call.requestJson()
        val personident = Personident(request["fødselsnummer"].asString())
        val inntektsendringer =
            request.path("inntektsendringer").values().map { inntektsendring ->
                Inntektsendringer.Inntektsendring(
                    kilde = Inntektskilde(inntektsendring["inntektskilde"].asString()),
                    nullstill =
                        inntektsendring.path("nullstill").values().map { nullstillingsperiode ->
                            nullstillingsperiode.åpenPeriode()
                        },
                    inntekter =
                        inntektsendring.path("inntekter").values().map { inntektsperiode ->
                            Inntektsendringer.Inntektsperiode(
                                periode = inntektsperiode.åpenPeriode(),
                                beløp = inntektsperiode.beløp(),
                            )
                        },
                )
            }
        val førsteDato = inntektsendringer.førsteDato() ?: throw BadRequestException("Fant ingen inntektsendringer i requesten.")
        Inntektsendringer(personident, inntektsendringer, dataSource())
        call.respondJson("""{"fom": "$førsteDato"}""", Created)
    }
}

private fun JsonNode.åpenPeriode() =
    ÅpenPeriode(
        fom = LocalDate.parse(path("fom").asString()),
        tom = path("tom").takeIf { it.isString }?.let { LocalDate.parse(it.asString()) },
    )

private fun JsonNode.ører() = this.takeIf { it.isNumber }?.asDouble()?.let { (it * 100.0).roundToInt() }

private fun JsonNode.beløp(): Beløp {
    val beløp =
        listOfNotNull(
            path("dagsbeløp").ører()?.let { Beløp.Daglig(it) },
            path("månedsbeløp").ører()?.let { Beløp.Månedlig(it) },
            path("årsbeløp").ører()?.let { Beløp.Årlig(it) },
            path("periodebeløp").ører()?.let { Beløp.Periodisert(it, åpenPeriode().lukketPeriode) },
        )
    if (beløp.size == 1) return beløp.single()
    if (beløp.isEmpty()) error("Det er ikke opplyst om noen beløp i ${toString()}!")
    error("Det er opplyst om fler beløp i samme periode! ${beløp.joinToString { "${it::class.simpleName}" }}")
}

private fun Inntektsendringer.Inntektsendring.førsteDato() = listOfNotNull(nullstill.minOfOrNull { it.fom }, inntekter.minOfOrNull { it.periode.fom }).minOrNull()

private fun List<Inntektsendringer.Inntektsendring>.førsteDato() = mapNotNull { it.førsteDato() }.minOrNull()
