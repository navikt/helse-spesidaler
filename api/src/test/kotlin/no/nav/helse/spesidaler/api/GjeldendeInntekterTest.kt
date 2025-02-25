package no.nav.helse.spesidaler.api

import no.nav.helse.spesidaler.api.Beløp.Oppløsning.Daglig
import no.nav.helse.spesidaler.api.Beløp.Oppløsning.Periodisert
import no.nav.helse.spesidaler.api.GjeldendeInntekter.Beløp.*
import no.nav.helse.spesidaler.api.GjeldendeInntekter.GjeldendeInntekt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class GjeldendeInntekterTest {
    private val orgnummer = "999999999"
    private val personident = "11111111111"

    @Test
    fun `ingen overlappende perioder`() {
        databaseTest {
            val dao = InntektDao(it)

            settInn(1000, 1.januar, 31.januar, dao)

            val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), dao).inntekter
            val forventedeInntekter = setOf(GjeldendeInntekt(orgnummer, Periode(1.januar, 31.januar), Daglig(1000)))

            assertEquals(forventedeInntekter, gjeldendeInntekter)
        }
    }

    @Test
    fun `omsluttet ny inntekt gir gammel snute og hale, men ny mage`() = databaseTest {
        val dao = InntektDao(it)

        settInn(1000, 1.januar, 31.januar, dao)
        settInn(2000, 10.januar, 20.januar, dao)

        val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), dao).inntekter
        val forventedeInntekter = setOf(
            GjeldendeInntekt(orgnummer, Periode(1.januar, 9.januar), Daglig(1000)),
            GjeldendeInntekt(orgnummer, Periode(10.januar, 20.januar), Daglig(2000)),
            GjeldendeInntekt(orgnummer, Periode(21.januar, 31.januar), Daglig(1000))
        )

        assertEquals(forventedeInntekter, gjeldendeInntekter)
    }

    @Test
    fun `håndterer foreløpig ikke periodisert inntekt`() = databaseTest {
        val dao = InntektDao(it)

        settInn(4_000_000, 1.januar, 31.januar, dao, Periodisert)
        settInn(100_000, 15.januar, 28.januar, dao, Daglig)

        assertThrows<IllegalStateException> {
            GjeldendeInntekter(personident, Periode(1.januar, 31.januar), dao).inntekter
        }
    }

    @Test
    fun `inntekter med forskjellig oppløsning`() = databaseTest {
        val dao = InntektDao(it)

        settInn(400_000_000, 1.januar, 31.januar, dao, Beløp.Oppløsning.Årlig)
        settInn(100_000, 15.januar, 28.januar, dao, Beløp.Oppløsning.Månedlig)
        settInn(0, 20.januar, 20.januar, dao, Beløp.Oppløsning.Daglig)

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

        val orgnummer1 = "999999999"
        val orgnummer2 = "111111111"

        settInn(400_000_000, 1.januar, 31.januar, dao, Beløp.Oppløsning.Årlig, orgnummer = orgnummer1)
        settInn(100_000, 15.januar, 28.januar, dao, Beløp.Oppløsning.Månedlig, orgnummer = orgnummer2)

        val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), dao).inntekter
        val forventedeInntekter = setOf(
            GjeldendeInntekt(orgnummer1, Periode(1.januar, 31.januar), Årlig(400_000_000)),
            GjeldendeInntekt(orgnummer2, Periode(15.januar, 28.januar), Månedlig(100_000))
        )

        assertEquals(forventedeInntekter, gjeldendeInntekter)
    }

    @Test
    fun `flere inntekter fra ulike kilder`() = databaseTest {
        val dao = InntektDao(it)

        val orgnummer1 = "999999999"
        val orgnummer2 = "111111111"

        settInn(400_000_000, 1.januar, 31.januar, dao, Beløp.Oppløsning.Årlig, orgnummer = orgnummer1)
        settInn(1000, 20.januar, 23.januar, dao, Beløp.Oppløsning.Daglig, orgnummer = orgnummer1)

        settInn(100_000, 15.januar, 28.januar, dao, Beløp.Oppløsning.Månedlig, orgnummer = orgnummer2)
        settInn(3000, 20.januar, 28.januar, dao, Beløp.Oppløsning.Månedlig, orgnummer = orgnummer2)

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

    private fun settInn(ører: Int, fom: LocalDate, tom: LocalDate, dao: InntektDao, oppløsning: Beløp.Oppløsning = Daglig, orgnummer: String = this.orgnummer) {
        dao.lagre(InntektInn(personident, orgnummer, Beløp(ører, oppløsning), fom, tom))
    }

    private val Int.januar get() = LocalDate.of(2018, 1, this)
}
