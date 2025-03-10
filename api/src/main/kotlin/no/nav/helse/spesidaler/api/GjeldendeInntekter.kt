package no.nav.helse.spesidaler.api

import java.time.Year
import kotlin.math.roundToInt
import no.nav.helse.spesidaler.api.Periode.Companion.trim
import no.nav.helse.spesidaler.api.db.Db
import no.nav.helse.spesidaler.api.db.InntektDao

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
        private data class AktuellInntekt(
            val periode: Periode,
            val inntektUt: Db.InntektUt
        )
        private fun gjeldendeInntekter(personident: String, periode: Periode, dao: InntektDao) = dao.hent(personident, periode)
            // Må første grupper på inntektskilden ettersom de må vurderes hver for seg
            .groupBy { it.kilde }
            // Sorterer inntektene på løpenummeret slik at vi ser på det nyeste først
            .mapValues { (_, inntekterUt) ->
                inntekterUt.sortedByDescending { inntektUt ->
                    inntektUt.løpenummer
                }
            }
            // Finner den overlappende delen av inntekten som er aktuell
            .mapValues { (_, inntekterUt) ->
                inntekterUt.map { inntektUt ->
                    val overlapp = Periode(inntektUt.fom, inntektUt.tom ?: periode.endInclusive).overlappendePeriode(periode) ?: error("Denne overlapper jo ikke?")
                    AktuellInntekt(overlapp, inntektUt)
                }
            }
            // Slår sammen alle inntektene
            // her er det viktig at vi ser på den nyeste inntekten først (som gjort i steg 1) og kun legger til perioder vi _ikke_ har hørt om før
            .mapValues { (_, overlappendeInntekter) ->
                overlappendeInntekter.fold(emptyList<AktuellInntekt>()) { sammenslått, aktuell->
                    val nyePerioder = sammenslått.map { it.periode }.sortedBy { it.start }.trim(aktuell.periode)
                    sammenslått + nyePerioder.map { aktuell.copy(periode = it) }
                }
            }
            // Mapper om resultetet til en liste med gjeldedende inntekter
            // Nå som vi er ferdig med å tolke per inntektskilde kan vi flatmappe det til én liste.
            // Fjerner de delene som ikke har noe beløp (beløp = null) - de er å anse som fjernet (som er noe annet enn å sette 0,-)
            .flatMap { (_, aktuelleInntekter) ->
                aktuelleInntekter.mapNotNull {
                    val beløpIØrer = it.inntektUt.beløp?.ører
                    if (beløpIØrer == null) null
                    else GjeldendeInntekt(
                        kilde = it.inntektUt.kilde,
                        periode = it.periode,
                        beløp = when (it.inntektUt.beløp) {
                            is Db.Daglig -> Beløp.Daglig(beløpIØrer)
                            is Db.Månedlig -> Beløp.Månedlig(beløpIØrer)
                            is Db.Årlig -> Beløp.Årlig(beløpIØrer)
                            is Db.Periodisert -> Beløp.Periodisert(beløpIØrer, it.inntektUt.fom til checkNotNull(it.inntektUt.tom) {
                                "En periodisert inntekt må ha en lukket periode (tom != null)"
                            })
                        }
                    )
                }
            }
            .toSet()
    }
}
