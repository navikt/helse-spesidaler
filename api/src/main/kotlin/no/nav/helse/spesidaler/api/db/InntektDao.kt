package no.nav.helse.spesidaler.api.db

import com.github.navikt.tbd_libs.sql_dsl.intOrNull
import com.github.navikt.tbd_libs.sql_dsl.localDate
import com.github.navikt.tbd_libs.sql_dsl.localDateOrNull
import com.github.navikt.tbd_libs.sql_dsl.long
import com.github.navikt.tbd_libs.sql_dsl.mapNotNull
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.string
import java.sql.Connection
import org.intellij.lang.annotations.Language
import java.sql.PreparedStatement
import no.nav.helse.spesidaler.api.Inntektskilde
import no.nav.helse.spesidaler.api.Periode
import no.nav.helse.spesidaler.api.Personident
import no.nav.helse.spesidaler.api.ÅpenPeriode

internal object InntektDao {

    internal fun Connection.lagre(inntekt: Db.InntektInn) {
        @Language("PostgreSQL")
        val sql = "INSERT INTO inntekt (personident, kilde, beløp_ører, beløp_oppløsning, fom, tom) VALUES (:personident, :kilde, :beløpØrer, CAST(:beløpOppløsning AS oppløsning), :fom, :tom)"
        prepareStatementWithNamedParameters(sql) {
            withParameter("personident", inntekt.personident.id)
            withParameter("kilde", inntekt.kilde.id)
            withParameter("beløpØrer", inntekt.beløp.ører)
            withParameter("beløpOppløsning", inntekt.beløp.oppløsning)
            withParameter("fom", inntekt.periode.fom)
            if (inntekt.periode.tom == null) withNull("tom")
            else withParameter("tom", inntekt.periode.tom)
        }.use(PreparedStatement::execute)
    }

    internal fun Connection.fjern(fjernInntekt: Db.FjernInntekt) {
        @Language("PostgreSQL")
        val sql = "INSERT INTO inntekt (personident, kilde, beløp_ører, beløp_oppløsning, fom, tom) VALUES (:personident, :kilde, NULL, CAST('Daglig' AS oppløsning), :fom, :tom)"
        prepareStatementWithNamedParameters(sql) {
            withParameter("personident", fjernInntekt.personident.id)
            withParameter("kilde", fjernInntekt.kilde.id)
            withParameter("fom", fjernInntekt.periode.fom)
            if (fjernInntekt.periode.tom == null) withNull("tom")
            else withParameter("tom", fjernInntekt.periode.tom)
        }.use(PreparedStatement::execute)
    }

    internal fun Connection.hent(personident: Personident, periode: Periode): List<Db.InntektUt> {
        @Language("PostgreSQL")
        val sql = "SELECT * FROM inntekt WHERE personident = :personident AND fom >= :fom AND (tom IS NULL OR tom <= :tom)"

        return prepareStatementWithNamedParameters(sql) {
            withParameter("personident", personident.id)
            withParameter("fom", periode.start)
            withParameter("tom", periode.endInclusive)
        }.mapNotNull { row ->
            Db.InntektUt(
                løpenummer = row.long("løpenummer"),
                kilde = Inntektskilde(row.string("kilde")),
                beløp = row.intOrNull("beløp_ører")?.let { beløpIØrer -> Db.Beløp.gjenopprett(beløpIØrer, row.string("beløp_oppløsning")) },
                periode = ÅpenPeriode(
                    fom = row.localDate("fom"),
                    tom = row.localDateOrNull("tom")
                )
            )
        }
    }
}

