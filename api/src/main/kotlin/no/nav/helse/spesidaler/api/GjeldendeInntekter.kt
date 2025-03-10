package no.nav.helse.spesidaler.api

import no.nav.helse.spesidaler.api.Periode.Companion.periodeOrNull
import no.nav.helse.spesidaler.api.db.Db
import no.nav.helse.spesidaler.api.db.InntektDao

internal class GjeldendeInntekter(personident: Personident, periode: Periode, dao: InntektDao) {
    val inntekter = gjeldendeInntekter(personident, periode, dao)

    data class GjeldendeInntekt(val kilde: Inntektskilde, val periode: Periode, val beløp: Beløp)

    private companion object {
        private data class AktuellInntekt(
            val periode: Periode,
            val inntektUt: Db.InntektUt
        )
        private fun gjeldendeInntekter(personident: Personident, periode: Periode, dao: InntektDao) = dao.hent(personident, periode)
            // Må første gruppere på inntektskilden ettersom de må vurderes hver for seg
            .groupBy { it.kilde }
            // Sorterer inntektene på løpenummeret slik at vi ser på det nyeste informasjonen først
            .mapValues { (_, inntekterUt) ->
                inntekterUt.sortedByDescending { inntektUt ->
                    inntektUt.løpenummer
                }
            }
            // Finner den overlappende delen av inntekten som er aktuell
            .mapValues { (_, inntekterUt) ->
                inntekterUt.mapNotNull { inntektUt ->
                    val overlapp = periodeOrNull(fom = inntektUt.periode.fom, tom = inntektUt.periode.tom ?: periode.endInclusive)?.overlappendePeriode(periode)
                    if (overlapp == null) null
                    else AktuellInntekt(overlapp, inntektUt)
                }
            }
            // Slår sammen alle inntektene
            // her er det viktig at vi ser på den nyeste inntekten først (som gjort i steg 2) og kun legger til perioder vi _ikke_ har hørt om før
            .mapValues { (_, overlappendeInntekter) ->
                overlappendeInntekter.fold(emptyList<AktuellInntekt>()) { sammenslått, aktuell->
                    val nyePerioder = aktuell.periode.uten(sammenslått.map { it.periode })
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
                            is Db.Periodisert -> Beløp.Periodisert(beløpIØrer, it.inntektUt.periode.lukketPeriode())
                        }
                    )
                }
            }
            .toSet()
    }
}
