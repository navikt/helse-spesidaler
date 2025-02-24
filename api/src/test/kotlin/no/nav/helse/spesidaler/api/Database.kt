package no.nav.helse.spesidaler.api

import com.github.navikt.tbd_libs.test_support.CleanupStrategy
import com.github.navikt.tbd_libs.test_support.DatabaseContainers

val databaseContainer = DatabaseContainers.container("spesidaler", CleanupStrategy.tables("inntekt"))