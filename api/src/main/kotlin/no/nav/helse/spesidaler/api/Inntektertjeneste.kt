package no.nav.helse.spesidaler.api

import javax.sql.DataSource
import no.nav.helse.spesidaler.api.Periode.Companion.til

internal class Inntektertjeneste(private val dataSource: DataSource) {

    fun hentGjeldendeInntekter(request: GjeldendeInntekterRequest): Set<GjeldendeInntekter.GjeldendeInntekt> {
        return GjeldendeInntekter(Personident(request.fødselsnummer), request.fom til request.tom, dataSource).inntekter
    }
}
