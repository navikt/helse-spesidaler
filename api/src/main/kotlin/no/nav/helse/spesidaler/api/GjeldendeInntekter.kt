package no.nav.helse.spesidaler.api

import no.nav.helse.spesidaler.api.Beløp.Oppløsning.*
import no.nav.helse.spesidaler.api.Periode.Companion.trim

internal class GjeldendeInntekter(personident: String, periode: Periode, dao: InntektDao) {
    val inntekter = gjeldendeInntekter(personident, periode, dao)

    data class GjeldendeInntekt(val kilde: String, val periode: Periode, val beløp: Beløp)

    sealed interface Beløp {
        val ører: Int
        data class Daglig(override val ører: Int): Beløp
        data class Månedlig(override val ører: Int): Beløp
        data class Årlig(override val ører: Int): Beløp
    }

    private companion object {
        private fun gjeldendeInntekter(personident: String, periode: Periode, dao: InntektDao) = dao.hent(personident, periode)
            .groupBy { it.kilde }
            .mapValues { (_, inntekterUt) ->
                inntekterUt.sortedByDescending { inntektUt ->
                    inntektUt.løpenummer
                }
            }
            .mapValues { (_, inntekterUt) ->
                inntekterUt.map { inntektUt ->
                    val overlapp = Periode(inntektUt.fom, inntektUt.tom ?: periode.endInclusive).overlappendePeriode(periode) ?: error("Denne overlapper jo ikke?")
                    val beløp = inntektUt.beløp.ører
                    when (inntektUt.beløp.oppløsning) {
                        Daglig -> GjeldendeInntekt(inntektUt.kilde, overlapp, Beløp.Daglig(beløp))
                        Månedlig -> GjeldendeInntekt(inntektUt.kilde, overlapp, Beløp.Månedlig(beløp))
                        Årlig -> GjeldendeInntekt(inntektUt.kilde, overlapp, Beløp.Årlig(beløp))
                        Periodisert -> error("Vi vet ikke om vi skal ha, eller hvordan vi skal håndtere, periodiserte inntekter")
                    }
                }
            }
            .mapValues { (_, gjeldendeInntekter) ->
                gjeldendeInntekter.fold(emptyList<GjeldendeInntekt>()) { sammenslått, aktuell->
                    val nyePerioder = sammenslått.map { it.periode }
                        .sortedBy { it.start }
                        .trim(aktuell.periode)
                    sammenslått + nyePerioder.map {
                        aktuell.copy(periode = it)
                    }
                }
            }
            .values
            .flatten()
            .filterNot { it.beløp.ører == 0 }
            .toSet()
    }
}
