package no.nav.helse.spesidaler.api

import com.github.navikt.tbd_libs.sql_dsl.connection
import java.sql.PreparedStatement
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.sql.DataSource
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import org.postgresql.util.PSQLException

internal class InntektTableTest {

    @Test
    fun `går ikke å lagre en ugyldig periode`() = databaseTest {
        @Language("PostgreSQL")
        val sql = """
            INSERT INTO inntekt (personident, kilde, beløp_ører, beløp_oppløsning, fom, tom) 
            VALUES ('1', 'org1', 1000, 'Daglig'::oppløsning, '2024-01-31'::date, '2024-01-30'::date)
        """
        it.assertConstraint(sql, "gyldig_periode")
    }

    @Test
    fun `går ikke å lagre et periodisert beløp uten tom`() = databaseTest {
        @Language("PostgreSQL")
        val sql = """
            INSERT INTO inntekt (personident, kilde, beløp_ører, beløp_oppløsning, fom, tom) 
            VALUES ('1', 'org1', 1000, 'Periodisert'::oppløsning, '2024-01-01'::date, null)
        """
        it.assertConstraint(sql, "gyldig_oppløsning")
    }

    @Test
    fun `går ikke å legge til en ugyldig oppløsning`() = databaseTest {
        @Language("PostgreSQL")
        val sql = """
            INSERT INTO inntekt (personident, kilde, beløp_ører, beløp_oppløsning, fom, tom) 
            VALUES ('1', 'org1', 1000, 'Tertial'::oppløsning, '2024-01-01'::date, '2024-01-31'::date)
        """
        it.assertPSQLException(sql, """invalid input value for enum "oppløsning": "Tertial"""")
    }

    private fun DataSource.assertConstraint(sql: String, constraint: String) =
        assertPSQLException(sql, """new row for relation "inntekt" violates check constraint "$constraint"""")

    private fun DataSource.assertPSQLException(sql: String, forventetFeilmelding: String){
        val feilmelding = assertThrows<PSQLException> {
            connection { prepareStatement(sql).use(PreparedStatement::execute) }
        }.message ?: "n/a"
        assertTrue(feilmelding.contains(forventetFeilmelding)) { "Nei, feilen var faktisk $feilmelding" }
    }
}
