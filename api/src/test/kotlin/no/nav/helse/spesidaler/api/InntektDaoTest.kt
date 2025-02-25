package no.nav.helse.spesidaler.api

import no.nav.helse.spesidaler.api.Beløp.Oppløsning.Daglig
import no.nav.helse.spesidaler.api.Beløp.Oppløsning.Periodisert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class InntektDaoTest {

    @Test
    fun `lagrer inntekt`() = databaseTest {
        val dao = InntektDao(it)

        val inntektInn = InntektInn(
            "fnr",
            "kilde",
            Beløp(200, Daglig),
            LocalDate.of(2018, 1, 1),
            LocalDate.of(2018, 1, 31)
        )

        dao.lagre(inntektInn)

        val lagredeInntekter = dao.hent(
            "fnr",
            LocalDate.of(2018, 1, 1) til LocalDate.of(2018, 1, 31)
        )

        assertEquals(
            listOf(
                InntektUt(
                    1,
                    "kilde",
                    Beløp(200, Daglig),
                    LocalDate.of(2018, 1, 1),
                    LocalDate.of(2018, 1, 31)
                )
            ),
            lagredeInntekter
        )
    }

    @Test
    fun `tom kan ikke være før fom`() {
        assertThrows<IllegalArgumentException> {
            InntektInn(
                "fnr",
                "kilde",
                Beløp(200, Daglig),
                LocalDate.of(2018, 1, 31),
                LocalDate.of(2018, 1, 1)
            )
        }
    }

    @Test
    fun `tom kan ikke være null når beløpet er periodisert`() {
        assertThrows<IllegalArgumentException> {
            InntektInn(
                "fnr",
                "kilde",
                Beløp(200, Periodisert),
                LocalDate.of(2018, 1, 1),
                null
            )
        }
    }

    @Test
    fun `beløp kan ikke være mindre enn 0`() {
        assertThrows<IllegalArgumentException> {
            InntektInn(
                "fnr",
                "kilde",
                Beløp(-100, Daglig),
                LocalDate.of(2018, 1, 1),
                LocalDate.of(2018, 1, 31)
            )
        }
    }
}
