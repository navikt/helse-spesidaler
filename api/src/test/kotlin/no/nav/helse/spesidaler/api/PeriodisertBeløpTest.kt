package no.nav.helse.spesidaler.api

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

internal class PeriodisertBeløpTest {

    @Test
    fun `Et periodisert beløp for tre år med forskjellig antall virkedager`() {
        val årsbeløp = 52_000_000
        val dagsbeløp = årsbeløp / 260
        assertEquals(200_000, dagsbeløp)

        with(2023) {
            val året = 1.januar(this) til 31.desember(this)
            val periodisertBeløp = GjeldendeInntekter.Beløp.Periodisert(årsbeløp, året)
            assertEquals(dagsbeløp, periodisertBeløp.daglig.ører)
            // Akkurat i 2023 så var det faktisk 260 dager, så da er det jo det samme
            assertEquals(260, året.virkedager)
            val utenVektlagtVirkedager = årsbeløp.toDouble() / året.virkedager
            assertEquals(200_000.0, utenVektlagtVirkedager)
        }
        with(2024) {
            val året = 1.januar(this) til 31.desember(this)
            val periodisertBeløp = GjeldendeInntekter.Beløp.Periodisert(årsbeløp, året)
            assertEquals(dagsbeløp, periodisertBeløp.daglig.ører)
            // Uten å vektlegge hvor mange virkedager det reelt var i 2024
            assertEquals(262, året.virkedager)
            val utenVektlagteVirkedager = årsbeløp.toDouble() / året.virkedager
            assertEquals(198_473.28244274808, utenVektlagteVirkedager)
        }
        with(2025) {
            val året = 1.januar(this) til 31.desember(this)
            val periodisertBeløp = GjeldendeInntekter.Beløp.Periodisert(årsbeløp, året)
            assertEquals(dagsbeløp, periodisertBeløp.daglig.ører)
            // Uten å vektlegge hvor mange virkedager det reelt er i 2025
            assertEquals(261, året.virkedager)
            val utenVektlagteVirkedager = årsbeløp.toDouble() / året.virkedager
            assertEquals(199_233.71647509577, utenVektlagteVirkedager)
        }
    }

    @Test
    fun `Et periodisert beløp over tre år med forskjellig antall virkedager`() {
        val årsbeløp = 52_000_000
        val dagsbeløp = årsbeløp / 260
        assertEquals(200_000, dagsbeløp)
        val `2023til2025` = 1.januar(2023) til 31.desember(2025)
        val periodisertBeløp = GjeldendeInntekter.Beløp.Periodisert(
            ører = årsbeløp * 3,
            periode = `2023til2025`
        )
        assertEquals(dagsbeløp, periodisertBeløp.daglig.ører)
        // Uten å vektlegge virkedager i de 3 årene
        assertEquals((260+262+261), `2023til2025`.virkedager)
        val utenVektlagteVirkedager = (årsbeløp * 3.0) / `2023til2025`.virkedager
        assertEquals(199_233.71647509577, utenVektlagteVirkedager)
    }
}
