package no.nav.helse.spesidaler.api

internal class Inntektertjeneste(private val inntektDao: InntektDao) {

    fun  hentGjeldendeInntekter(request: GjeldendeInntekterRequest): Set<GjeldendeInntekter.GjeldendeInntekt> {
        return GjeldendeInntekter(request.fødselsnummer, request.fom til request.tom, inntektDao).inntekter
    }
}
