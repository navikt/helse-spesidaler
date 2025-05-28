package no.nav.helse.spesidaler.opprydding_dev

import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import javax.sql.DataSource

internal class PersonRepository(private val dataSource: DataSource) {
    internal fun slett(fødselsnummer: String) {
        sessionOf(dataSource).use { session ->
            session.transaction {
                it.slettInntekter(fødselsnummer)
            }
        }
    }

    private fun Session.slettInntekter(fødselsnummer: String) {
        val query = "DELETE FROM inntekt WHERE personident = ?"
        run(queryOf(query, fødselsnummer).asExecute)
    }
}
