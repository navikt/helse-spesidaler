package no.nav.helse.spesidaler.api

import java.time.LocalDate
import no.nav.helse.spesidaler.api.GjeldendeInntekter.GjeldendeInntekt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import javax.sql.DataSource
import no.nav.helse.spesidaler.api.Beløp.Daglig
import no.nav.helse.spesidaler.api.Beløp.Månedlig
import no.nav.helse.spesidaler.api.Beløp.Periodisert
import no.nav.helse.spesidaler.api.Beløp.Årlig
import no.nav.helse.spesidaler.api.Inntektsendringer.Inntektsendring
import no.nav.helse.spesidaler.api.Periode.Companion.til
import no.nav.helse.spesidaler.api.PeriodisertBeløpTest.Companion.daglig

internal class GjeldendeInntekterTest {
    private val orgnummer = Inntektskilde("999999999")
    private val personident = Personident("11111111111")

    @Test
    fun `ingen overlappende perioder`() {
        databaseTest {
            it.settInn(Daglig(10_00), 1.januar tilÅpen 31.januar)

            val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), it).inntekter
            val forventedeInntekter = setOf(GjeldendeInntekt(orgnummer, Periode(1.januar, 31.januar), Daglig(10_00)))

            assertEquals(forventedeInntekter, gjeldendeInntekter)
        }
    }

    @Test
    fun `omsluttet ny inntekt gir gammel snute og hale, men ny mage`() = databaseTest {

        it.settInn(Daglig(10_00), 1.januar tilÅpen 31.januar)
        it.settInn(Daglig(20_00), 10.januar tilÅpen 20.januar)

        val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), it).inntekter
        val forventedeInntekter = setOf(
            GjeldendeInntekt(orgnummer, Periode(1.januar, 9.januar), Daglig(10_00)),
            GjeldendeInntekt(orgnummer, Periode(10.januar, 20.januar), Daglig(20_00)),
            GjeldendeInntekt(orgnummer, Periode(21.januar, 31.januar), Daglig(10_00))
        )

        assertEquals(forventedeInntekter, gjeldendeInntekter)
    }

    @Test
    fun `håndterer også periodisert inntekt`() = databaseTest {

        it.settInn(Periodisert(40000_00, 1.januar til 31.januar), 1.januar tilÅpen 31.januar)
        it.settInn(Daglig(1000_00), 15.januar tilÅpen 28.januar)

        val forventetPeriodisertBeløp = Periodisert(40000_00, 1.januar til 31.januar)

        val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), it).inntekter
        val forventedeInntekter = setOf(
            GjeldendeInntekt(orgnummer, Periode(1.januar, 14.januar), forventetPeriodisertBeløp),
            GjeldendeInntekt(orgnummer, Periode(15.januar, 28.januar), Daglig(1000_00)),
            GjeldendeInntekt(orgnummer, Periode(29.januar, 31.januar), forventetPeriodisertBeløp),
        )
        assertEquals(forventedeInntekter, gjeldendeInntekter)
        assertEquals(1745_81.9397993311, forventetPeriodisertBeløp.daglig)
    }

    @Test
    fun `inntekter med forskjellig oppløsning`() = databaseTest {

        it.settInn(Årlig(4000000_00), 1.januar tilÅpen 31.januar)
        it.settInn(Månedlig(1000_00), 15.januar tilÅpen 28.januar)
        it.fjern(20.januar tilÅpen  20.januar)

        val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), it).inntekter
        val forventedeInntekter = setOf(
            GjeldendeInntekt(orgnummer, Periode(1.januar, 14.januar), Årlig(4000000_00)),
            GjeldendeInntekt(orgnummer, Periode(15.januar, 19.januar), Månedlig(1000_00)),
            GjeldendeInntekt(orgnummer, Periode(21.januar, 28.januar), Månedlig(1000_00)),
            GjeldendeInntekt(orgnummer, Periode(29.januar, 31.januar), Årlig(4000000_00))
        )

        assertEquals(forventedeInntekter, gjeldendeInntekter)
    }

    @Test
    fun `inntekter fra ulike kilder`() = databaseTest {
        val orgnummer1 = Inntektskilde("999999999")
        val orgnummer2 = Inntektskilde("111111111")

        it.settInn(Årlig(4000000_00), 1.januar tilÅpen 31.januar, inntektskilde = orgnummer1)
        it.settInn(Månedlig(1000_00), 15.januar tilÅpen 28.januar, inntektskilde = orgnummer2)

        val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), it).inntekter
        val forventedeInntekter = setOf(
            GjeldendeInntekt(orgnummer1, Periode(1.januar, 31.januar), Årlig(4000000_00)),
            GjeldendeInntekt(orgnummer2, Periode(15.januar, 28.januar), Månedlig(1000_00))
        )

        assertEquals(forventedeInntekter, gjeldendeInntekter)
    }

    @Test
    fun `å sette inntekten til 0 kroner er noe annet enn å fjerne inntekten`() = databaseTest {
        val orgnummer1 = Inntektskilde("999999999")
        val orgnummer2 = Inntektskilde("111111111")

        it.settInn(Daglig(100_00), 1.januar tilÅpen 31.januar, inntektskilde = orgnummer1)
        it.settInn(Daglig(100_00), 1.januar tilÅpen 31.januar, inntektskilde = orgnummer2)
        it.settInn(Daglig(0), 1.januar tilÅpen 31.januar, inntektskilde = orgnummer1)
        it.fjern(1.januar tilÅpen 31.januar, orgnummer2)

        val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), it).inntekter
        val forventedeInntekter = setOf(
            GjeldendeInntekt(orgnummer1, Periode(1.januar, 31.januar), Daglig(0)),
        )

        assertEquals(forventedeInntekter, gjeldendeInntekter)
    }

    @Test
    fun `flere inntekter fra ulike kilder`() = databaseTest {
        val orgnummer1 = Inntektskilde("999999999")
        val orgnummer2 = Inntektskilde("111111111")

        it.settInn(Årlig(4000000_00), 1.januar tilÅpen 31.januar, inntektskilde = orgnummer1)
        it.settInn(Daglig(10_00), 20.januar tilÅpen 23.januar, inntektskilde = orgnummer1)
        it.settInn(Månedlig(1000_00), 15.januar tilÅpen 28.januar, inntektskilde = orgnummer2)
        it.settInn(Månedlig(30_00), 20.januar tilÅpen 28.januar, inntektskilde = orgnummer2)

        val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), it).inntekter
        val forventedeInntekter = setOf(
            GjeldendeInntekt(orgnummer1, Periode(1.januar, 19.januar), Årlig(4000000_00)),
            GjeldendeInntekt(orgnummer1, Periode(20.januar, 23.januar), Daglig(10_00)),
            GjeldendeInntekt(orgnummer1, Periode(24.januar, 31.januar), Årlig(4000000_00)),

            GjeldendeInntekt(orgnummer2, Periode(15.januar, 19.januar), Månedlig(1000_00)),
            GjeldendeInntekt(orgnummer2, Periode(20.januar, 28.januar), Månedlig(30_00)),
        )
        assertEquals(forventedeInntekter, gjeldendeInntekter)
    }

    private fun DataSource.settInn(beløp: Beløp, åpenPeriode: ÅpenPeriode, inntektskilde: Inntektskilde = this@GjeldendeInntekterTest.orgnummer) {
        Inntektsendringer(
            personident = personident,
            inntektsendringer = listOf(Inntektsendring(
                kilde = inntektskilde,
                nullstill = emptyList(),
                inntekter = listOf(Inntektsendringer.Inntektsperiode(periode = åpenPeriode, beløp = beløp))
            )),
            dataSource = this
        )
    }
    private fun DataSource.fjern(åpenPeriode: ÅpenPeriode, inntektskilde: Inntektskilde = this@GjeldendeInntekterTest.orgnummer) {
        Inntektsendringer(
            personident = personident,
            inntektsendringer = listOf(Inntektsendring(
                kilde = inntektskilde,
                nullstill = listOf(åpenPeriode),
                inntekter = emptyList()
            )),
            dataSource = this
        )
    }

    private infix fun LocalDate.tilÅpen(tom: LocalDate?) = ÅpenPeriode(this, tom)
}
