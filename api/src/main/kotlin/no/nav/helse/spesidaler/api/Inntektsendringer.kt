package no.nav.helse.spesidaler.api

import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.transaction
import javax.sql.DataSource
import no.nav.helse.spesidaler.api.db.Db
import no.nav.helse.spesidaler.api.db.InntektDao.fjern
import no.nav.helse.spesidaler.api.db.InntektDao.lagre

internal class Inntektsendringer(personident: Personident, inntektsendringer: List<Inntektsendring>, dataSource: DataSource) {
    data class Inntektsendring(
        val kilde: Inntektskilde,
        val nullstill: List<ÅpenPeriode>,
        val inntekter: List<Inntektsperiode>
    )

    data class Inntektsperiode(
        val periode: ÅpenPeriode,
        val beløp: Beløp
    )

    init {
        dataSource.connection {
            transaction {
                inntektsendringer.forEach { inntektsendring ->
                    // Først fjerner vi alle inntekter som eventuelt skal nullstilles før vi tolker de nye inntektene
                    inntektsendring.nullstill.forEach { nullstillingsperiode ->
                        fjern(Db.FjernInntekt(
                            personident = personident,
                            kilde = inntektsendring.kilde,
                            periode = nullstillingsperiode
                        ))
                    }
                    // Også legger vi til eventuelle ny innteker
                    inntektsendring.inntekter.forEach { inntektsperiode ->
                        lagre(Db.InntektInn(
                            personident = personident,
                            kilde = inntektsendring.kilde,
                            periode = inntektsperiode.periode,
                            beløp = when (inntektsperiode.beløp) {
                                is Beløp.Daglig -> Db.Daglig(inntektsperiode.beløp.ører)
                                is Beløp.Månedlig -> Db.Månedlig(inntektsperiode.beløp.ører)
                                is Beløp.Årlig -> Db.Årlig(inntektsperiode.beløp.ører)
                                is Beløp.Periodisert -> Db.Periodisert(inntektsperiode.beløp.ører).also {
                                    check(inntektsperiode.periode.lukketPeriode() == inntektsperiode.beløp.periode) { "Dette må være en tøysete periodisert inntekt!" }
                                }
                            }
                        ))
                    }
                }
            }
        }
    }
}

