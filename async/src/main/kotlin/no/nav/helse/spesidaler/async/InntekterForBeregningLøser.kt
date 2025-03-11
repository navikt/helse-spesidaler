package no.nav.helse.spesidaler.async

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory

internal class InntekterForBeregningLøser(
    rapidsConnection: RapidsConnection,
    private val spesidalerApiClient: SpesidalerApiClient
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition { it.requireValue("@event_name", "behov") }
            precondition { it.requireAll("@behov", listOf("InntekterForBeregning")) }
            precondition { it.forbid("@løsning") }
            validate { it.requireKey("fødselsnummer") }
            validate { it.require("InntekterForBeregning.fom", JsonNode::asLocalDate) }
            validate { it.require("InntekterForBeregning.tom", JsonNode::asLocalDate) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        sikkerlogg.info("Mottok behov for InntekterForBeregning:\n\t${packet.toJson()}")
        val fødselsnummer = packet["fødselsnummer"].asText()

        packet["@løsning"] = mapOf(
            "InntekterForBeregning" to mapOf(
                "inntekter" to spesidalerApiClient.inntekterForBeregning(packet)
            )
        )
        val json = packet.toJson()
        context.publish(fødselsnummer, json).also {
            sikkerlogg.info("Sender løsning for InntekterForBeregning:\n\t${json}")
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        sikkerlogg.error("Forstod ikke behovet: \n${problems.toExtendedReport()}")
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
