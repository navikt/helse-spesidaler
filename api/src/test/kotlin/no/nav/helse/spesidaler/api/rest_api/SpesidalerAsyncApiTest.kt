package no.nav.helse.spesidaler.api.rest_api

import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import kotlin.test.assertEquals
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class SpesidalerAsyncApiTest: RestApiTest() {

    @Test
    fun `lagring og henting av inntekter`() = spesidalerTestApp {
        inntektsendringer(
            requestBody = Inntektsendringer,
            assertResponse = { status, responseBody ->
                assertEquals(Created, status)
                assertJsonEquals("""{"fom": "2018-01-01"}""", responseBody)
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
            assertResponse = { status, responseBody ->
                assertEquals(OK, status)
                assertJsonEquals("""{
                  "inntekter": [{
                    "inntektskilde": "999999999",
                    "fom": "2018-01-10",
                    "tom": "2018-01-20",
                    "daglig": 5772.115384615385
                  }]
                }""", responseBody)
            }
        )
    }

    @Test
    fun `lagring og henting av inntekter med feil rolle`() = spesidalerTestApp {
        inntektsendringer(
            requestBody = Inntektsendringer,
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
    private val Inntektsendringer = """
    {
      "fødselsnummer": "11111111111",
      "inntektsendringer": [
        {
          "inntektskilde": "999999999",
          "nullstill": [{
            "fom": "2018-01-01",
            "tom": "2018-01-31"
          }],
          "inntekter": [
            {
              "fom": "2018-01-10",
              "tom": "2018-01-20",
              "periodebeløp": 46000.0
            }
          ]
        }
      ]
    }
    """
}
