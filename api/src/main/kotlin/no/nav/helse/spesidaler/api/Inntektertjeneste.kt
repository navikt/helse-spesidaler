package no.nav.helse.spesidaler.api

import no.nav.helse.spesidaler.api.Periode.Companion.til
import no.nav.helse.spesidaler.api.db.InntektDao

internal class Inntektertjeneste(private val inntektDao: InntektDao) {

    fun hentGjeldendeInntekter(request: GjeldendeInntekterRequest): Set<GjeldendeInntekter.GjeldendeInntekt> {
        return GjeldendeInntekter(Personident(request.fødselsnummer), request.fom til request.tom, inntektDao).inntekter
    }
}
