package no.nav.helse.spesidaler.api

import com.github.navikt.tbd_libs.sql_dsl.connection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import javax.sql.DataSource
import no.nav.helse.spesidaler.api.Periode.Companion.til
import no.nav.helse.spesidaler.api.db.Db
import no.nav.helse.spesidaler.api.db.InntektDao.hent
import no.nav.helse.spesidaler.api.db.InntektDao.lagre
import org.junit.jupiter.api.Assertions.assertNull

internal class InntektDaoTest {

    @Test
    fun `lagrer inntekt`() = databaseTest {

        val inntektInn = Db.InntektInn(
            personident = Personident("fnr"),
            kilde = Inntektskilde("kilde"),
            beløp = Db.Daglig(200),
            periode = ÅpenPeriode(
                fom = LocalDate.of(2018, 1, 1),
                tom = LocalDate.of(2018, 1, 31)
            )
        )

        it.connection { lagre(inntektInn) }

        val lagredeInntekter = it.connection { hent(
            personident = Personident("fnr"),
            periode = LocalDate.of(2018, 1, 1) til LocalDate.of(2018, 1, 31)
        )}

        assertEquals(
            listOf(
                Db.InntektUt(
                    løpenummer = 1,
                    kilde = Inntektskilde("kilde"),
                    beløp = Db.Daglig(200),
                    periode = ÅpenPeriode(
                        fom = LocalDate.of(2018, 1, 1),
                        tom = LocalDate.of(2018, 1, 31)
                    )
                )
            ),
            lagredeInntekter
        )
    }

    @Test
    fun `tom kan ikke være før fom`() {
        assertThrows<IllegalArgumentException> {
            Db.InntektInn(
                personident = Personident("fnr"),
                kilde = Inntektskilde("kilde"),
                beløp = Db.Daglig(200),
                periode = ÅpenPeriode(
                    fom = LocalDate.of(2018, 1, 31),
                    tom = LocalDate.of(2018, 1, 1)
                )
            )
        }
    }

    @Test
    fun `tom kan ikke være null når beløpet er periodisert`() {
        assertThrows<IllegalArgumentException> {
            Db.InntektInn(
                personident = Personident("fnr"),
                kilde = Inntektskilde("kilde"),
                beløp = Db.Periodisert(200),
                periode = ÅpenPeriode(
                    fom = LocalDate.of(2018, 1, 1),
                    tom = null
                )
            )
        }
    }

    @Test
    fun `beløp kan ikke være mindre enn 0`() {
        assertThrows<IllegalArgumentException> {
            Db.InntektInn(
                personident = Personident("fnr"),
                kilde = Inntektskilde("kilde"),
                beløp = Db.Daglig(-100),
                periode = ÅpenPeriode(
                    fom = LocalDate.of(2018, 1, 1),
                    tom = LocalDate.of(2018, 1, 31)
                )
            )
        }
    }

    @Test
    fun `overlappende inntekt som starter før perioden`() = databaseTest {
        val personident = Personident("fnr")
        val kilde = Inntektskilde("kilde")
        val periode = 2.januar til 31.januar

        val starterFør = Db.InntektInn(
            personident = personident,
            kilde = kilde,
            beløp = Db.Daglig(200),
            periode = ÅpenPeriode(
                fom = 1.januar,
                tom = 31.januar
            )
        )

        val inntekt = it.lagrOgHent(periode, starterFør)
        assertEquals(inntekt, starterFør.ut())
    }

    @Test
    fun `overlappende inntekt som slutter etter perioden`() = databaseTest {
        val personident = Personident("fnr")
        val kilde = Inntektskilde("kilde")
        val periode = 1.januar til 30.januar

        val slutterEtter = Db.InntektInn(
            personident = personident,
            kilde = kilde,
            beløp = Db.Daglig(200),
            periode = ÅpenPeriode(
                fom = 1.januar,
                tom = 31.januar
            )
        )
        val inntekt = it.lagrOgHent(periode, slutterEtter)
        assertEquals(inntekt, slutterEtter.ut())
    }

    @Test
    fun `spør om inntekter i et hull`() = databaseTest {
        val personident = Personident("fnr")
        val kilde = Inntektskilde("kilde")

        val før = Db.InntektInn(
            personident = personident,
            kilde = kilde,
            beløp = Db.Daglig(200),
            periode = ÅpenPeriode(
                fom = 1.januar,
                tom = 31.januar
            )
        )

        val etter = Db.InntektInn(
            personident = personident,
            kilde = kilde,
            beløp = Db.Daglig(200),
            periode = ÅpenPeriode(
                fom = 1.mars,
                tom = 31.mars
            )
        )
        val inntekt = it.lagrOgHent(1.februar til 28.februar, før, etter)
        assertNull(inntekt)
    }

    private fun Db.InntektInn.ut(løpenummer: Long = 1) = Db.InntektUt(
        løpenummer = løpenummer,
        kilde = kilde,
        beløp = beløp,
        periode = periode
    )

    private fun DataSource.lagrOgHent(periode: Periode, vararg inntekt: Db.InntektInn) = connection {
        inntekt::forEach { lagre(it) }
        hent(inntekt.first().personident, periode)
    }.also { check(it.size in 0..1) }.singleOrNull()
}
