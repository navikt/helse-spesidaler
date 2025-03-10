package no.nav.helse.spesidaler.api

import com.auth0.jwk.JwkProvider
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

class AzureApp(
    private val jwkProvider: JwkProvider,
    private val issuer: String,
    private val clientId: String,
) {
    fun konfigurerJwtAuth(config: AuthenticationConfig) {
        config.endepunkt("inntektsendringer")
        config.endepunkt("inntekter-for-beregning")
    }
    private fun AuthenticationConfig.endepunkt(endpunkt: String) {
        jwt(endpunkt) {
            verifier(jwkProvider, issuer) {
                withAudience(clientId)
                withClaimPresence("azp_name")
                withArrayClaim("roles", endpunkt)
            }
            validate { credentials ->
                JWTPrincipal(credentials.payload)
            }
        }
    }
}
