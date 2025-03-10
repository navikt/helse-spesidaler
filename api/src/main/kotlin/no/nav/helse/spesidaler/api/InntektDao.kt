package no.nav.helse.spesidaler.api

import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.intOrNull
import com.github.navikt.tbd_libs.sql_dsl.localDate
import com.github.navikt.tbd_libs.sql_dsl.localDateOrNull
import com.github.navikt.tbd_libs.sql_dsl.long
import com.github.navikt.tbd_libs.sql_dsl.mapNotNull
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.string
import no.nav.helse.spesidaler.api.Beløp.Oppløsning.Periodisert
import org.intellij.lang.annotations.Language
import java.sql.PreparedStatement
import java.time.LocalDate
import javax.sql.DataSource

internal class InntektDao(private val dataSource: DataSource) {

    internal fun lagre(inntekt: InntektInn) {
        dataSource.connection {
            @Language("PostgreSQL")
            val sql = "INSERT INTO inntekt (personident, kilde, beløp_ører, beløp_oppløsning, fom, tom) VALUES (:personident, :kilde, :beløpØrer, CAST(:beløpOppløsning AS oppløsning), :fom, :tom)"
            prepareStatementWithNamedParameters(sql) {
                withParameter("personident", inntekt.personident)
                withParameter("kilde", inntekt.kilde)
                withParameter("beløpØrer", inntekt.beløp.ører)
                withParameter("beløpOppløsning", inntekt.beløp.oppløsning.name)
                withParameter("fom", inntekt.fom)
                if (inntekt.tom == null) withNull("tom")
                else withParameter("tom", inntekt.tom)
            }.use(PreparedStatement::execute)
        }
    }

    internal fun fjern(fjernInntekt: FjernInntekt) {
        dataSource.connection {
            @Language("PostgreSQL")
            val sql = "INSERT INTO inntekt (personident, kilde, beløp_ører, beløp_oppløsning, fom, tom) VALUES (:personident, :kilde, NULL, CAST('Daglig' AS oppløsning), :fom, :tom)"
            prepareStatementWithNamedParameters(sql) {
                withParameter("personident", fjernInntekt.personident)
                withParameter("kilde", fjernInntekt.kilde)
                withParameter("fom", fjernInntekt.fom)
                if (fjernInntekt.tom == null) withNull("tom")
                else withParameter("tom", fjernInntekt.tom)
            }.use(PreparedStatement::execute)
        }
    }

    internal fun hent(personident: String, periode: Periode): List<InntektUt> {
        return dataSource.connection {
            @Language("PostgreSQL")
            val sql = "SELECT * FROM inntekt WHERE personident = :personident AND fom >= :fom AND (tom IS NULL OR tom <= :tom)"

            prepareStatementWithNamedParameters(sql) {
                withParameter("personident", personident)
                withParameter("fom", periode.start)
                withParameter("tom", periode.endInclusive)
            }.mapNotNull { row ->
                InntektUt(
                    løpenummer = row.long("løpenummer"),
                    kilde = row.string("kilde"),
                    beløp = row.intOrNull("beløp_ører")?.let { beløpIØrer ->
                        Beløp(
                            ører = beløpIØrer,
                            oppløsning = Beløp.Oppløsning.valueOf(row.string("beløp_oppløsning"))
                        )
                    },
                    fom = row.localDate("fom"),
                    tom = row.localDateOrNull("tom")
                )
            }
        }
    }
}

internal data class FjernInntekt(
    val personident: String,
    val kilde: String,
    val fom: LocalDate,
    val tom: LocalDate?
) {
    init {
        require((tom ?: fom) >= fom) { "Ugyldig input $fom til $tom" }
    }
}

internal data class InntektInn(
    val personident: String,
    val kilde: String,
    val beløp: Beløp,
    val fom: LocalDate,
    val tom: LocalDate?,
) {
    init {
        require((tom ?: fom) >= fom) { "Ugyldig input $fom til $tom" }
        if (beløp.oppløsning == Periodisert) require(tom != null) { "For periodiserte inntekter må det settes en tom" }
        require(beløp.ører >= 0) { "Beløp kan ikke være mindre enn 0" }
    }
}

internal data class InntektUt(
    val løpenummer: Long,
    val kilde: String,
    val beløp: Beløp?,
    val fom: LocalDate,
    val tom: LocalDate?,
)

data class Beløp(
    val ører: Int,
    val oppløsning: Oppløsning
) {
    enum class Oppløsning {
        Daglig, Månedlig, Årlig, Periodisert
    }
}
