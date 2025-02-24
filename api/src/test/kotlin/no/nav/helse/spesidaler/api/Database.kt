package no.nav.helse.spesidaler.api

import com.github.navikt.tbd_libs.test_support.CleanupStrategy
import com.github.navikt.tbd_libs.test_support.DatabaseContainers
import javax.sql.DataSource

val databaseContainer = DatabaseContainers.container("spesidaler", CleanupStrategy.tables("inntekt"))
fun databaseTest(testblokk: (DataSource) -> Unit) {
    val testDataSource = databaseContainer.nyTilkobling()
    try {
        testblokk(testDataSource.ds)
    } finally {
        databaseContainer.droppTilkobling(testDataSource)
    }
}
