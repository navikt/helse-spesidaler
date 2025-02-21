package no.nav.helse.spesidaler.async

import com.github.navikt.tbd_libs.azure.createAzureTokenClientFromEnvironment
import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.LoggerFactory
import java.net.http.HttpClient

private val logg = LoggerFactory.getLogger("no.nav.helse.spesidaler.async.App")
private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

fun main() {
    val env = System.getenv()
    val httpClient = HttpClient.newHttpClient()

    val azure = createAzureTokenClientFromEnvironment(env)

    val kafkaConfig = AivenConfig.default
    val consumerProducerFactory = ConsumerProducerFactory(kafkaConfig)

    RapidApplication.create(env, consumerProducerFactory = consumerProducerFactory).start()
}
