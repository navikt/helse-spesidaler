package no.nav.helse.spesidaler.api

import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.mapNotNull
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import org.intellij.lang.annotations.Language
import java.sql.Date
import java.time.LocalDate
import javax.sql.DataSource

internal class InntektDao(private val dataSource: DataSource) {

    internal fun lagre(inntekt: InntektInn) {
        dataSource.connection {
            @Language("PostgreSQL")
            val sql = "INSERT INTO inntekt (personident, kilde, daglig_beløp_ører, fom, tom) VALUES (:personident, :kilde, :dagligBeløpØrer, :fom, :tom)"
            prepareStatementWithNamedParameters(sql) {
                withParameter("personident", inntekt.personident)
                withParameter("kilde", inntekt.kilde)
                withParameter("dagligBeløpØrer", inntekt.dagligBeløpØrer)
                withParameter("fom") { columnIndex -> 
                    setDate(columnIndex, Date.valueOf(inntekt.fom))
                }
                if(inntekt.tom == null) {
                    withNull("tom")
                } else {
                    withParameter("tom") { columnIndex ->
                        setDate(columnIndex, Date.valueOf(inntekt.tom))
                    }    
                }
            }.use { 
                it.execute()
            }
        }
    }

    internal fun hent(personident: String, periode: Periode): List<InntektUt> {
        return dataSource.connection {
            @Language("PostgreSQL")
            val sql = "SELECT * FROM inntekt WHERE personident = :personident AND fom >= :fom AND (tom IS NULL OR tom <= :tom)"

            prepareStatementWithNamedParameters(sql) {
                withParameter("personident", personident)
                withParameter("fom") { columnIndex ->
                    setDate(columnIndex, Date.valueOf(periode.start))
                }
                withParameter("tom") { columnIndex ->
                    setDate(columnIndex, Date.valueOf(periode.endInclusive))
                }
            }.use {
                it
                    .executeQuery()
                    .use { rs ->
                        rs.mapNotNull { row ->
                            InntektUt(
                                løpenummer = row.getLong("løpenummer"),
                                kilde = row.getString("kilde"),
                                dagligBeløpØrer = row.getInt("daglig_beløp_ører"),
                                fom = row.getDate("fom").toLocalDate(),
                                tom = row.getDate("tom")?.toLocalDate()
                            )
                        }
                    }
            }
        }
        
    }
}

internal data class InntektInn(
    val personident: String,
    val kilde: String,
    val dagligBeløpØrer: Int,
    val fom: LocalDate,
    val tom: LocalDate?,
) {
    init {
        require((tom ?: fom) >= fom) {"Ugyldig input $fom til $tom"}
    }
}

internal data class InntektUt(
    val løpenummer: Long,
    val kilde: String,
    val dagligBeløpØrer: Int,
    val fom: LocalDate,
    val tom: LocalDate?,
)