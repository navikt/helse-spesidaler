package no.nav.helse.spesidaler.api.rest_api

import com.github.navikt.tbd_libs.signed_jwt_issuer_test.Issuer
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.intellij.lang.annotations.Language
import org.skyscreamer.jsonassert.JSONAssert

internal data class RestApiTestContext(
    private val maskinporten: Issuer,
    private val client: HttpClient
) {
    fun spesidalerAsyncAccessToken(rolle: String?) = maskinporten.accessToken {
        rolle?.let { withArrayClaim("roles", arrayOf(rolle)) }
        withClaim("azp_name", "spesidaler-async")
    }
    suspend fun inntektsendringer(
        @Language("JSON") requestBody: String,
        accessToken: String = spesidalerAsyncAccessToken("inntektsendringer"),
        assertResponse: (status: HttpStatusCode, respondeBody: String) -> Unit
    ) {
        val response = client.post("/inntektsendringer") {
            header("Authorization", "Bearer $accessToken")
            setBody(requestBody)
        }
        assertResponse(response.status, response.bodyAsText())
    }

    suspend fun inntekterForBeregning(
        @Language("JSON") requestBody: String,
        accessToken: String = spesidalerAsyncAccessToken("inntekter-for-beregning"),
        assertResponse: (status: HttpStatusCode, respondeBody: String) -> Unit
    ) {
        val response = client.post("/inntekter-for-beregning") {
            header("Authorization", "Bearer $accessToken")
            setBody(requestBody)
        }
        assertResponse(response.status, response.bodyAsText())
    }

    fun assertJsonEquals(@Language("JSON") forventet: String, faktisk: String) = JSONAssert.assertEquals(forventet, faktisk, true)
}
