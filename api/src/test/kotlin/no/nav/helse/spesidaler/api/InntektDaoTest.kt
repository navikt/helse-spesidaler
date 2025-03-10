package no.nav.helse.spesidaler.api

import com.github.navikt.tbd_libs.sql_dsl.connection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import no.nav.helse.spesidaler.api.Periode.Companion.til
import no.nav.helse.spesidaler.api.db.Db
import no.nav.helse.spesidaler.api.db.InntektDao.hent
import no.nav.helse.spesidaler.api.db.InntektDao.lagre

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
}
