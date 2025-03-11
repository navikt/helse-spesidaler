package no.nav.helse.spesidaler.async

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory

internal class InntektsendringerRiver(
    rapidsConnection: RapidsConnection,
    private val spesidalerApiClient: SpesidalerApiClient
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireValue("@event_name", "inntektsendringer")
                it.forbid("inntektsendringFom")
            }
            validate { it.requireKey("fødselsnummer") }
            // TODO: mer validering her da
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        sikkerlogg.info("Mottok inntektsendringer:\n\t${packet.toJson()}")
        val fødselsnummer = packet["fødselsnummer"].asText()

        packet["inntektsendringFom"] = spesidalerApiClient.inntektsendringer(packet)

        val json = packet.toJson()
        context.publish(fødselsnummer, json).also {
            sikkerlogg.info("Sender inntektsendringer med inntektsendringFom:\n\t${json}")
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        sikkerlogg.error("Forstod ikke inntektsendringer:\n${problems.toExtendedReport()}")
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
