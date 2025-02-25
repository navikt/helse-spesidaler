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

internal class InntekterLøser(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition { it.requireValue("@event_name", "behov") }
            precondition { it.requireAll("@behov", listOf("Inntekter")) }
            precondition { it.forbid("@løsning") }
            validate { it.requireKey("fødselsnummer") }
            validate { it.require("Inntekter.inntekterFom", JsonNode::asLocalDate) }
            validate { it.require("Inntekter.inntekterTom", JsonNode::asLocalDate) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        sikkerlogg.info("Mottok behov for Inntekter:\n\t${packet.toJson()}")
        val personIdent = packet["fødselsnummer"].asText()
        val fom = packet["Inntekter.inntekterFom"].asLocalDate()
        val tom = packet["Inntekter.inntekterTom"].asLocalDate()

        packet["@løsning"] = mapOf(
            "Inntekter" to mapOf(
                "inntekter" to emptyList<Nothing>()
            )
        )
        context.publish(personIdent, packet.toJson()).also {
            sikkerlogg.info("Sender løsning for Inntekter:\n\t${packet.toJson()}")
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        sikkerlogg.error("Forstod ikke behovet: \n${problems.toExtendedReport()}")
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
