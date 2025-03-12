package no.nav.helse.spesidaler.api

import kotlin.test.assertEquals
import no.nav.helse.spesidaler.api.Periode.Companion.til
import org.junit.jupiter.api.Test

internal class PeriodisertBeløpTest {

    @Test
    fun `Et periodisert beløp for tre år med forskjellig antall virkedager`() {
        val årsbeløp = 520000_00
        val dagsbeløp = årsbeløp / 260.0
        assertEquals(2000_00.0, dagsbeløp)

        with(2023) {
            val året = 1.januar(this) til 31.desember(this)
            val periodisertBeløp = Beløp.Periodisert(årsbeløp, året)
            assertEquals(dagsbeløp, periodisertBeløp.daglig)
            // Akkurat i 2023 så var det faktisk 260 dager, så da er det jo det samme
            assertEquals(260, året.virkedager)
            assertEquals(2000_00.0, periodisertBeløp.dagligUtenVektlagtVirkedager)
        }
        with(2024) {
            val året = 1.januar(this) til 31.desember(this)
            val periodisertBeløp = Beløp.Periodisert(årsbeløp, året)
            assertEquals(dagsbeløp, BeregnetDaglig(periodisertBeløp).ører)
            // Uten å vektlegge hvor mange virkedager det reelt var i 2024
            assertEquals(262, året.virkedager)
            assertEquals(198_473.28244274808, periodisertBeløp.dagligUtenVektlagtVirkedager)
        }
        with(2025) {
            val året = 1.januar(this) til 31.desember(this)
            val periodisertBeløp = Beløp.Periodisert(årsbeløp, året)
            assertEquals(dagsbeløp, periodisertBeløp.daglig)
            // Uten å vektlegge hvor mange virkedager det reelt er i 2025
            assertEquals(261, året.virkedager)
            assertEquals(1992_33.71647509577, periodisertBeløp.dagligUtenVektlagtVirkedager)
        }
    }

    @Test
    fun `Et periodisert beløp over tre år med forskjellig antall virkedager`() {
        val årsbeløp = 520000_00
        val dagsbeløp = årsbeløp / 260.0
        assertEquals(2000_00.0, dagsbeløp)
        val `2023til2025` = 1.januar(2023) til 31.desember(2025)
        val periodisertBeløp = Beløp.Periodisert(
            ører = årsbeløp * 3,
            periode = `2023til2025`
        )
        assertEquals(dagsbeløp, periodisertBeløp.daglig)
        // Uten å vektlegge virkedager i de 3 årene
        assertEquals((260 + 262 + 261), `2023til2025`.virkedager)
        assertEquals(1992_33.71647509577, periodisertBeløp.dagligUtenVektlagtVirkedager)
    }

    @Test
    fun `Et mindre rundt beløp`() {
        val periodebeløp = Beløp.Periodisert(5333_00, 22.januar(2024) til 29.januar(2024))
        assertEquals(6, periodebeløp.periode.virkedager)
        assertEquals(895_67.05128205128, periodebeløp.daglig)
        assertEquals(888_83.33333333333, periodebeløp.dagligUtenVektlagtVirkedager)
    }

    internal companion object {
        val Beløp.Periodisert.daglig get() = BeregnetDaglig(this).ører
        val Beløp.Periodisert.dagligUtenVektlagtVirkedager get() = ører / periode.virkedager.toDouble()
    }
}
