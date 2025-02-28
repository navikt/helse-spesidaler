package no.nav.helse.spesidaler.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate

internal fun Route.api(inntektertjeneste: Inntektertjeneste) {
    post("/api/inntekter/gjeldende") {
        val request = call.receiveNullable<GjeldendeInntekterRequest>()
            ?: throw BadRequestException("Mangler fødselsnummer eller fom")
        val gjeldendeInntekter = inntektertjeneste.hentGjeldendeInntekter(request)

        val gjeldendeInntekterResponse = GjeldendeInntekterResponse(
            fødselsnummer = request.fødselsnummer,
            gjeldendeInntekter = gjeldendeInntekter.map {
                GjeldendeInntekterResponse.GjeldendeInntekt(
                    kilde = it.kilde,
                    fom = it.periode.start,
                    tom = it.periode.endInclusive,
                    ører = it.beløp.ører,
                    oppløsning = when (it.beløp) {
                        is GjeldendeInntekter.Beløp.Daglig -> GjeldendeInntekterResponse.GjeldendeInntekt.Oppløsning.Daglig
                        is GjeldendeInntekter.Beløp.Månedlig -> GjeldendeInntekterResponse.GjeldendeInntekt.Oppløsning.Månedlig
                        is GjeldendeInntekter.Beløp.Periodisert -> GjeldendeInntekterResponse.GjeldendeInntekt.Oppløsning.Årlig
                        is GjeldendeInntekter.Beløp.Årlig -> GjeldendeInntekterResponse.GjeldendeInntekt.Oppløsning.Periodisert
                    }
                )
            }
        )
        call.respond(HttpStatusCode.OK, gjeldendeInntekterResponse)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GjeldendeInntekterRequest(
    val fødselsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate
) {
    init {
        require(tom >= fom) { "Tom må være større eller lik fom" }
    }
}

data class GjeldendeInntekterResponse(
    val fødselsnummer: String,
    val gjeldendeInntekter: List<GjeldendeInntekt>
) {
    data class GjeldendeInntekt(
        val kilde: String,
        val fom: LocalDate,
        val tom: LocalDate,
        val ører: Int,
        val oppløsning: Oppløsning
    ) {
        enum class Oppløsning {
            Daglig, Månedlig, Årlig, Periodisert
        }
    }
}
