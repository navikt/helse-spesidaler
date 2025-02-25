package no.nav.helse.spesidaler.api

import java.time.Year
import kotlin.math.roundToInt
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
        data class Periodisert(override val ører: Int, val periode: Periode): Beløp {
            /** Et Nav-år har 260 virkedager, men ekte år varierer mellom 260 og 262 virkedager.
                Her vektes dagene utifra hvor mange virkedager det reelt er i året/årene
                perioden dekker. Da beregnes et daglig beløp som videre kan tolkes som 1/260 Nav-dag **/
            private val vektlagteVirkedager = periode
                .groupBy { Year.of(it.year) }
                .mapValues { (_, dager) -> dager.min() til dager.max() }
                .map { (år, periode) -> (260.0 / år.virkedager) * periode.virkedager }
                .reduce(Double::plus)
            val daglig = Daglig((ører / vektlagteVirkedager).roundToInt())
        }
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
                        Periodisert -> GjeldendeInntekt(inntektUt.kilde, overlapp, Beløp.Periodisert(beløp, inntektUt.fom til checkNotNull(inntektUt.tom) {
                            "En periodisert inntekt må ha en lukket periode (tom != null)"
                        }))
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
