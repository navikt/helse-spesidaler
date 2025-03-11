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

internal class InntektsendringerRiver(
    rapidsConnection: RapidsConnection,
    private val spesidalerApiClient: SpesidalerApiClient
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireValue("@event_name", "inntektsendringer")
                it.forbid("inntektsendringFom") // Denne klasker Spesidaler på selv
            }
            validate {
                it.require("@id") { id -> UUID.fromString(id.asText()) }
                it.requireKey("fødselsnummer")
                it.requireArray("inntektsendringer") {
                    requireKey("inntektskilde")
                    requireArray("nullstill") {
                        require("fom", JsonNode::asLocalDate)
                        // Denne (tom) er optional i API'et, men required her. For å unngå å åpne for mange dører
                        require("tom", JsonNode::asLocalDate)
                    }
                    requireArray("inntekter") {
                        require("fom", JsonNode::asLocalDate)
                        // Denne (tom) er optional i API'et, men required her. For å unngå å åpne for mange dører
                        require("tom", JsonNode::asLocalDate)
                        interestedIn("periodebeløp", "dagsbeløp", "månedsbeløp", "årsbeløp")
                    }
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        sikkerlogg.info("Mottok inntektsendringer:\n\t${packet.toJson()}")
        val fødselsnummer = packet["fødselsnummer"].asText()

        val inntektsendringFom = inntektsendringerOrNull(packet) ?: return

        packet["inntektsendringFom"] = inntektsendringFom

        val json = packet.toJson()
        context.publish(fødselsnummer, json).also {
            sikkerlogg.info("Sender inntektsendringer med inntektsendringFom:\n\t${json}")
        }
    }

    private fun inntektsendringerOrNull(packet: JsonMessage) = try {
        spesidalerApiClient.inntektsendringer(packet)
    } catch (err: Exception) {
        sikkerlogg.error("Feil ved håndtering av inntektsendringer", err)
        null
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        sikkerlogg.error("Forstod ikke inntektsendringer:\n${problems.toExtendedReport()}")
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
