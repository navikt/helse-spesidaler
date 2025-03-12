package no.nav.helse.spesidaler.api.rest_api

import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import kotlin.test.assertEquals
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class SpesidalerAsyncApiTest: RestApiTest() {

    @Test
    fun `lagring henting av inntekter`() = spesidalerTestApp {
        inntektsendringer(
            requestBody = LeggTil,
            assertResponse = { status, responseBody ->
                assertEquals(Created, status)
                assertJsonEquals("""{"fom": "2018-01-10"}""", responseBody)
            }
        )

        inntekterForBeregning(
            requestBody = """
            { 
              "fødselsnummer": "11111111111",
              "InntekterForBeregning": {
                "fom": "2018-01-11",
                "tom": "2020-12-30"
              }
            }""",
            assertResponse = { status, responseBody ->
                assertEquals(OK, status)
                assertJsonEquals("""{
                  "inntekter": [{
                    "inntektskilde": "999999999",
                    "fom": "2018-01-11",
                    "tom": "2018-01-20",
                    "daglig": 5772.115384615385
                  },
                  {
                    "inntektskilde": "999999999",
                    "fom": "2019-01-10",
                    "tom": "2019-01-20",
                    "daglig": 1123.12
                  },
                  {
                    "inntektskilde": "888888888",
                    "fom": "2019-01-15",
                    "tom": "2019-02-15",
                    "månedlig": 40500.50
                  },
                  {
                    "inntektskilde": "888888888",
                    "fom": "2020-12-01",
                    "tom": "2020-12-30",
                    "årlig": 666666.66
                  }]
                }""", responseBody)
            }
        )

        inntektsendringer(
            requestBody = Fjern,
            assertResponse = { status, responseBody ->
                assertEquals(Created, status)
                assertJsonEquals("""{"fom": "2018-01-10"}""", responseBody)
            }
        )

        inntekterForBeregning(
            requestBody = """
            { 
              "fødselsnummer": "11111111111",
              "InntekterForBeregning": {
                "fom": "2018-01-10",
                "tom": "2025-01-30"
              }
            }""",
            assertResponse = { status, responseBody ->
                assertEquals(OK, status)
                assertJsonEquals("""{
                  "inntekter": [{
                    "inntektskilde": "999999999",
                    "fom": "2025-01-01",
                    "tom": "2025-01-30",
                    "daglig": 0
                  },{
                    "inntektskilde": "999999999",
                    "fom": "2019-01-10",
                    "tom": "2019-01-10",
                    "daglig": 1123.12
                  },
                  {
                    "inntektskilde": "999999999",
                    "fom": "2019-01-20",
                    "tom": "2019-01-20",
                    "daglig": 1123.12
                  },
                  {
                    "inntektskilde": "888888888",
                    "fom": "2019-01-15",
                    "tom": "2019-02-15",
                    "månedlig": 40500.50
                  },
                  {
                    "inntektskilde": "888888888",
                    "fom": "2020-12-01",
                    "tom": "2020-12-31",
                    "årlig": 666666.66
                  }]
                }""", responseBody)
            }
        )
    }

    @Test
    fun `lagring og henting av inntekter med feil rolle`() = spesidalerTestApp {
        inntektsendringer(
            requestBody = LeggTil,
            accessToken = spesidalerAsyncAccessToken(rolle = "feil-rolle"),
            assertResponse = { status, _ ->
                assertEquals(Unauthorized, status)
            }
        )

        inntekterForBeregning(
            requestBody = """
            { 
              "fødselsnummer": "11111111111",
              "InntekterForBeregning": {
                "fom": "2018-01-10",
                "tom": "2018-01-20"
              }
            }""",
            accessToken = spesidalerAsyncAccessToken(rolle = null),
            assertResponse = { status, _ ->
                assertEquals(Unauthorized, status)
            }
        )
    }

    @Language("JSON")
    private val LeggTil = """
    {
      "fødselsnummer": "11111111111",
      "inntektsendringer": [
        {
          "inntektskilde": "999999999",
          "nullstill": [],
          "inntekter": [
            {
              "fom": "2019-01-10",
              "tom": "2019-01-20",
              "dagsbeløp": 1123.12
            },
            {
              "fom": "2018-01-10",
              "tom": "2018-01-20",
              "periodebeløp": 46000.0
            }
          ]
        },
        {
          "inntektskilde": "888888888",
          "nullstill": [],
          "inntekter": [
            {
              "fom": "2020-12-01",
              "tom": "2020-12-31",
              "årsbeløp": 666666.66
            },
            {
              "fom": "2019-01-15",
              "tom": "2019-02-15",
              "månedsbeløp": 40500.50
            }
          ]
        }
      ]
    }
    """

    @Language("JSON")
    private val Fjern = """
    {
      "fødselsnummer": "11111111111",
      "inntektsendringer": [
        {
          "inntektskilde": "999999999",
          "nullstill": [
            {
              "--kommentar--": "beholder snute & hale",
              "fom": "2019-01-11",
              "tom": "2019-01-19"
            }, 
            {
              "--kommentar--": "nullstiller hele",
              "fom": "2018-01-10",
              "tom": "2018-01-20"
            }
          ],
          "inntekter": [
            {
              "--kommentar--": "legger til en på 0.- samtidig bar for å være morsom",
              "fom": "2025-01-01",
              "tom": "2025-01-31",
              "periodebeløp": 0
            }
          ]
        }
      ]
    }
    """
}
