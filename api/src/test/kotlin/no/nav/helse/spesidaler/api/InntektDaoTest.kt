package no.nav.helse.spesidaler.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import no.nav.helse.spesidaler.api.db.Db
import no.nav.helse.spesidaler.api.db.InntektDao

internal class InntektDaoTest {

    @Test
    fun `lagrer inntekt`() = databaseTest {
        val dao = InntektDao(it)

        val inntektInn = Db.InntektInn(
            "fnr",
            "kilde",
            Db.Daglig(200),
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
                Db.InntektUt(
                    1,
                    "kilde",
                    Db.Daglig(200),
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
            Db.InntektInn(
                "fnr",
                "kilde",
                Db.Daglig(200),
                LocalDate.of(2018, 1, 31),
                LocalDate.of(2018, 1, 1)
            )
        }
    }

    @Test
    fun `tom kan ikke være null når beløpet er periodisert`() {
        assertThrows<IllegalArgumentException> {
            Db.InntektInn(
                "fnr",
                "kilde",
                Db.Periodisert(200),
                LocalDate.of(2018, 1, 1),
                null
            )
        }
    }

    @Test
    fun `beløp kan ikke være mindre enn 0`() {
        assertThrows<IllegalArgumentException> {
            Db.InntektInn(
                "fnr",
                "kilde",
                Db.Daglig(-100),
                LocalDate.of(2018, 1, 1),
                LocalDate.of(2018, 1, 31)
            )
        }
    }
}
