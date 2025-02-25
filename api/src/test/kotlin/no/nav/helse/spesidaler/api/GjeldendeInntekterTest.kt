package no.nav.helse.spesidaler.api

import no.nav.helse.spesidaler.api.Beløp.Oppløsning.Daglig
import no.nav.helse.spesidaler.api.GjeldendeInntekter.Beløp.*
import no.nav.helse.spesidaler.api.GjeldendeInntekter.GjeldendeInntekt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GjeldendeInntekterTest {
    private val orgnummer = "999999999"
    private val personident = "11111111111"

    @Test
    fun `ingen overlappende perioder`() {
        databaseTest {
            val dao = InntektDao(it)

            settInn(1000, 1.januar, 31.januar, dao)

            val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), dao)
            val forventedeInntekter = setOf(GjeldendeInntekt(orgnummer, Periode(1.januar, 31.januar), Daglig(1000)))

            assertEquals(forventedeInntekter, gjeldendeInntekter)
        }
    }

    @Test
    fun `omsluttet ny inntekt gir gammel snute og hale, men ny mage`() = databaseTest {
        val dao = InntektDao(it)

        settInn(1000, 1.januar, 31.januar, dao)
        settInn(2000, 10.januar, 20.januar, dao)

        val gjeldendeInntekter = GjeldendeInntekter(personident, Periode(1.januar, 31.januar), dao)
        val forventedeInntekter = setOf(
            GjeldendeInntekt(orgnummer, Periode(1.januar, 9.januar), Daglig(1000)),
            GjeldendeInntekt(orgnummer, Periode(10.januar, 20.januar), Daglig(2000)),
            GjeldendeInntekt(orgnummer, Periode(21.januar, 31.januar), Daglig(1000))
        )

        assertEquals(forventedeInntekter, gjeldendeInntekter)
    }

    private fun settInn(ører: Int, fom: LocalDate, tom: LocalDate, dao: InntektDao) {
        dao.lagre(InntektInn(personident, orgnummer, Beløp(ører, Daglig), fom, tom))
    }

    private val Int.januar get() = LocalDate.of(2018, 1, this)
}
