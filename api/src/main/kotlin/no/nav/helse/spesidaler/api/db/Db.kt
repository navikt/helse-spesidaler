package no.nav.helse.spesidaler.api.db

import java.time.LocalDate

internal object Db {
    data class FjernInntekt(
        val personident: String,
        val kilde: String,
        val fom: LocalDate,
        val tom: LocalDate?
    ) {
        init {
            require((tom ?: fom) >= fom) { "Ugyldig input $fom til $tom" }
        }
    }

    data class InntektInn(
        val personident: String,
        val kilde: String,
        val beløp: Beløp,
        val fom: LocalDate,
        val tom: LocalDate?,
    ) {
        init {
            require((tom ?: fom) >= fom) { "Ugyldig input $fom til $tom" }
            if (beløp is Periodisert) require(tom != null) { "For periodiserte inntekter må det settes en tom" }
            require(beløp.ører >= 0) { "Beløp kan ikke være mindre enn 0" }
        }
    }

    data class InntektUt(
        val løpenummer: Long,
        val kilde: String,
        val beløp: Beløp?,
        val fom: LocalDate,
        val tom: LocalDate?,
    )

    sealed interface Beløp {
        val ører: Int
        val oppløsning: String
        companion object {
            fun gjenopprett(ører: Int, oppløsning: String) = when (oppløsning) {
                "Daglig" -> Daglig(ører)
                "Månedlig" -> Månedlig(ører)
                "Årlig" -> Årlig(ører)
                "Periodisert" -> Periodisert(ører)
                else -> error("Kjenner ikke til oppløsning $oppløsning")
            }
        }
    }
    data class Daglig(override val ører: Int): Beløp {
        override val oppløsning = "Daglig"
    }
    data class Månedlig(override val ører: Int): Beløp {
        override val oppløsning = "Månedlig"
    }
    data class Årlig(override val ører: Int): Beløp {
        override val oppløsning = "Årlig"
    }
    data class Periodisert(override val ører: Int): Beløp {
        override val oppløsning = "Periodisert"
    }
}
