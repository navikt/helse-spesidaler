package no.nav.helse.spesidaler.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.sql.DataSource

internal class InntektDaoTest() {

    @Test
    fun `lagrer inntekt`() = databaseTest {
        val dao = InntektDao(it)

        val inntektInn = InntektInn(
            "fnr",
            "kilde",
            200,
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
                    200,
                    LocalDate.of(2018, 1, 1),
                    LocalDate.of(2018, 1, 31)
                )
            ),
            lagredeInntekter
        )
    }

    private fun databaseTest(testblokk: (DataSource) -> Unit) {
        val testDataSource = databaseContainer.nyTilkobling()
        try {
            testblokk(testDataSource.ds)
        } finally {
            databaseContainer.droppTilkobling(testDataSource)
        }
    }

}