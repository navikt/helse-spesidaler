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
import java.util.*
import org.slf4j.LoggerFactory

internal class InntekterForBeregningLøser(
    rapidsConnection: RapidsConnection,
    private val spesidalerApiClient: SpesidalerApiClient
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireValue("@event_name", "behov")
                it.requireAll("@behov", listOf("InntekterForBeregning"))
                it.forbid("@løsning")
            }
            validate {
                it.requireKey("fødselsnummer")
                it.require("@id") { id -> UUID.fromString(id.asText()) }
                it.require("InntekterForBeregning.fom", JsonNode::asLocalDate)
                it.require("InntekterForBeregning.tom", JsonNode::asLocalDate)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        sikkerlogg.info("Mottok behov for InntekterForBeregning:\n\t${packet.toJson()}")
        val fødselsnummer = packet["fødselsnummer"].asText()

        val inntekter = inntekterForBeregningOrNull(packet) ?: return

        packet["@løsning"] = mapOf(
            "InntekterForBeregning" to mapOf(
                "inntekter" to inntekter
            )
        )
        val json = packet.toJson()
        context.publish(fødselsnummer, json).also {
            sikkerlogg.info("Sender løsning for InntekterForBeregning:\n\t${json}")
        }
    }

    private fun inntekterForBeregningOrNull(packet: JsonMessage) = try {
        spesidalerApiClient.inntekterForBeregning(packet)
    } catch (err: Exception) {
        sikkerlogg.error("Feil ved håndtering av inntekterForBeregning", err)
        null
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        sikkerlogg.error("Forstod ikke behovet: \n${problems.toExtendedReport()}")
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
