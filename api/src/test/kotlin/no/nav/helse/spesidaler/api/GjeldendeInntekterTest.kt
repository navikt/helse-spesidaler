package no.nav.helse.spesidaler.api

import no.nav.helse.spesidaler.api.GjeldendeInntekter.GjeldendeInntekt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.helse.spesidaler.api.Beløp.Daglig
import no.nav.helse.spesidaler.api.Beløp.Månedlig
import no.nav.helse.spesidaler.api.Beløp.Periodisert
import no.nav.helse.spesidaler.api.Beløp.Årlig
import no.nav.helse.spesidaler.api.db.Db
import no.nav.helse.spesidaler.api.db.InntektDao

internal class GjeldendeInntekterTest {
    private val orgnummer = Inntektskilde("999999999")
    private val personident = Personident("11111111111")

    @Test
    fun `ingen overlappende perioder`() {
        databaseTest {
            val dao = InntektDao(it)

            settInn(Db.Daglig(1000), 1.januar, 31.januar, dao)

            val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), dao).inntekter
            val forventedeInntekter = setOf(GjeldendeInntekt(orgnummer, Periode(1.januar, 31.januar), Daglig(1000)))

            assertEquals(forventedeInntekter, gjeldendeInntekter)
        }
    }

    @Test
    fun `omsluttet ny inntekt gir gammel snute og hale, men ny mage`() = databaseTest {
        val dao = InntektDao(it)

        settInn(Db.Daglig(1000), 1.januar, 31.januar, dao)
        settInn(Db.Daglig(2000), 10.januar, 20.januar, dao)

        val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), dao).inntekter
        val forventedeInntekter = setOf(
            GjeldendeInntekt(orgnummer, Periode(1.januar, 9.januar), Daglig(1000)),
            GjeldendeInntekt(orgnummer, Periode(10.januar, 20.januar), Daglig(2000)),
            GjeldendeInntekt(orgnummer, Periode(21.januar, 31.januar), Daglig(1000))
        )

        assertEquals(forventedeInntekter, gjeldendeInntekter)
    }

    @Test
    fun `håndterer også periodisert inntekt`() = databaseTest {
        val dao = InntektDao(it)

        settInn(Db.Periodisert(4_000_000), 1.januar, 31.januar, dao)
        settInn(Db.Daglig(100_000), 15.januar, 28.januar, dao)

        val forventetPeriodisertBeløp = Periodisert(4_000_000, 1.januar til 31.januar)

        val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), dao).inntekter
        val forventedeInntekter = setOf(
            GjeldendeInntekt(orgnummer, Periode(1.januar, 14.januar), forventetPeriodisertBeløp),
            GjeldendeInntekt(orgnummer, Periode(15.januar, 28.januar), Daglig(100_000)),
            GjeldendeInntekt(orgnummer, Periode(29.januar, 31.januar), forventetPeriodisertBeløp),
        )
        assertEquals(forventedeInntekter, gjeldendeInntekter)
        assertEquals(Daglig(174582), forventetPeriodisertBeløp.daglig)
    }

    @Test
    fun `inntekter med forskjellig oppløsning`() = databaseTest {
        val dao = InntektDao(it)

        settInn(Db.Årlig(400_000_000), 1.januar, 31.januar, dao)
        settInn(Db.Månedlig(100_000), 15.januar, 28.januar, dao)
        fjern(20.januar, 20.januar, dao)

        val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), dao).inntekter
        val forventedeInntekter = setOf(
            GjeldendeInntekt(orgnummer, Periode(1.januar, 14.januar), Årlig(400_000_000)),
            GjeldendeInntekt(orgnummer, Periode(15.januar, 19.januar), Månedlig(100_000)),
            GjeldendeInntekt(orgnummer, Periode(21.januar, 28.januar), Månedlig(100_000)),
            GjeldendeInntekt(orgnummer, Periode(29.januar, 31.januar), Årlig(400_000_000))
        )

        assertEquals(forventedeInntekter, gjeldendeInntekter)
    }

    @Test
    fun `inntekter fra ulike kilder`() = databaseTest {
        val dao = InntektDao(it)

        val orgnummer1 = Inntektskilde("999999999")
        val orgnummer2 = Inntektskilde("111111111")

        settInn(Db.Årlig(400_000_000), 1.januar, 31.januar, dao, inntektskilde = orgnummer1)
        settInn(Db.Månedlig(100_000), 15.januar, 28.januar, dao, inntektskilde = orgnummer2)

        val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), dao).inntekter
        val forventedeInntekter = setOf(
            GjeldendeInntekt(orgnummer1, Periode(1.januar, 31.januar), Årlig(400_000_000)),
            GjeldendeInntekt(orgnummer2, Periode(15.januar, 28.januar), Månedlig(100_000))
        )

        assertEquals(forventedeInntekter, gjeldendeInntekter)
    }

    @Test
    fun `å sette inntekten til 0 kroner er noe annet enn å fjerne inntekten`() = databaseTest {
        val dao = InntektDao(it)

        val orgnummer1 = Inntektskilde("999999999")
        val orgnummer2 = Inntektskilde("111111111")

        settInn(Db.Daglig(100_00), 1.januar, 31.januar, dao, inntektskilde = orgnummer1)
        settInn(Db.Daglig(100_00), 1.januar, 31.januar, dao, inntektskilde = orgnummer2)
        settInn(Db.Daglig(0), 1.januar, 31.januar, dao, inntektskilde = orgnummer1)
        fjern(1.januar, 31.januar, dao, orgnummer2)

        val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), dao).inntekter
        val forventedeInntekter = setOf(
            GjeldendeInntekt(orgnummer1, Periode(1.januar, 31.januar), Daglig(0)),
        )

        assertEquals(forventedeInntekter, gjeldendeInntekter)
    }

    @Test
    fun `flere inntekter fra ulike kilder`() = databaseTest {
        val dao = InntektDao(it)

        val orgnummer1 = Inntektskilde("999999999")
        val orgnummer2 = Inntektskilde("111111111")

        settInn(Db.Årlig(400_000_000), 1.januar, 31.januar, dao, inntektskilde = orgnummer1)
        settInn(Db.Daglig(1000), 20.januar, 23.januar, dao, inntektskilde = orgnummer1)

        settInn(Db.Månedlig(100_000), 15.januar, 28.januar, dao, inntektskilde = orgnummer2)
        settInn(Db.Månedlig(3000), 20.januar, 28.januar, dao, inntektskilde = orgnummer2)

        val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), dao).inntekter
        val forventedeInntekter = setOf(
            GjeldendeInntekt(orgnummer1, Periode(1.januar, 19.januar), Årlig(400_000_000)),
            GjeldendeInntekt(orgnummer1, Periode(20.januar, 23.januar), Daglig(1000)),
            GjeldendeInntekt(orgnummer1, Periode(24.januar, 31.januar), Årlig(400_000_000)),

            GjeldendeInntekt(orgnummer2, Periode(15.januar, 19.januar), Månedlig(100_000)),
            GjeldendeInntekt(orgnummer2, Periode(20.januar, 28.januar), Månedlig(3000)),
        )

        assertEquals(forventedeInntekter, gjeldendeInntekter)
    }

    private fun settInn(beløp: Db.Beløp, fom: LocalDate, tom: LocalDate, dao: InntektDao, inntektskilde: Inntektskilde = this.orgnummer) {
        dao.lagre(Db.InntektInn(personident, inntektskilde, beløp, ÅpenPeriode(fom, tom)))
    }
    private fun fjern(fom: LocalDate, tom: LocalDate?, dao: InntektDao, inntektskilde: Inntektskilde = this.orgnummer) {
        dao.fjern(Db.FjernInntekt(personident, inntektskilde, ÅpenPeriode(fom, tom)))
    }
}
