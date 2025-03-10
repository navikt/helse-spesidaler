package no.nav.helse.spesidaler.api

import java.time.LocalDate
import java.time.Year
import kotlin.math.roundToInt
import no.nav.helse.spesidaler.api.Periode.Companion.til
import no.nav.helse.spesidaler.api.Periode.Companion.virkedager

@JvmInline value class Personident(val id: String) {
    init { check(id.isNotBlank()) { "Ugyldig personident!" }}
}
@JvmInline value class Inntektskilde(val id: String) {
    init { check(id.isNotBlank()) { "Ugyldig inntektskilde!" }}
}

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

data class ÅpenPeriode(
    val fom: LocalDate,
    val tom: LocalDate?
) {
    init {
        tom?.let { Periode(fom, it) } // Validerer perioden om tom er satt
    }
    fun lukketPeriode() = fom til checkNotNull(tom) { "Åpen periode har ikke 'tom' satt og kan således ikke gjøres om til en lukket periode!" }
}
