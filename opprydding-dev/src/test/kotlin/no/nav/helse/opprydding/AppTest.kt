package no.nav.helse.opprydding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import kotliquery.queryOf
import kotliquery.sessionOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AppTest : DataSourceBuilderTest() {
    private lateinit var testRapid: TestRapid
    private lateinit var personRepository: PersonRepository

    @BeforeEach
    fun beforeEach() {
        testRapid = TestRapid()
        personRepository = PersonRepository(testDataSource.ds)
        SlettPersonRiver(testRapid, personRepository)
    }

    @Test
    fun `slettemelding medfører at person slettes fra databasen`() {
        val fødselsnummer = "123"
        opprettTestdata(fødselsnummer)
        assertEquals(1, finnInntekt(fødselsnummer))

        testRapid.sendTestMessage(slettemelding(fødselsnummer))

        assertEquals(0, finnInntekt(fødselsnummer))
    }

    @Test
    fun `sletter kun aktuelt fnr`() {
        opprettTestdata("123")
        opprettTestdata("1234")

        testRapid.sendTestMessage(slettemelding("123"))

        assertEquals(1, finnInntekt("1234"))

        assertEquals(0, finnInntekt("123"))
    }

    private fun slettemelding(fødselsnummer: String) =
        JsonMessage.newMessage("slett_person", mapOf("fødselsnummer" to fødselsnummer)).toJson()

    private fun opprettTestdata(fødselsnummer: String) {
        opprettInntekt(fødselsnummer)
        assertEquals(1, finnInntekt(fødselsnummer))
    }

    private fun finnInntekt(fødselsnummer: String): Int {
        return sessionOf(testDataSource.ds).use { session ->
            session.run(queryOf("SELECT COUNT(1) FROM inntekt WHERE fnr = ?", fødselsnummer).map { it.int(1) }.asSingle)
        } ?: 0
    }

    private fun opprettInntekt(fødselsnummer: String) {
        val query = "INSERT INTO inntekt (fnr) VALUES (?)"
        sessionOf(testDataSource.ds).use { it.run(queryOf(query, fødselsnummer).asUpdate) }
    }

}
