package no.nav.helse.spesidaler.api

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

internal interface DataSourceBuilder {
    val dataSource: DataSource
    fun migrate()
}

internal class DefaultDataSourceBuilder(env: Map<String, String>): DataSourceBuilder {

    private val baseConnectionConfig = HikariConfig().apply {
        jdbcUrl = env.getValue("DATABASE_JDBC_URL")
    }

    private val migrationConfig = HikariConfig().apply {
        baseConnectionConfig.copyStateTo(this)
        maximumPoolSize = 2
    }
    private val appConfig = HikariConfig().apply {
        baseConnectionConfig.copyStateTo(this)
        maximumPoolSize = 2
    }

    override val dataSource by lazy { HikariDataSource(appConfig) }

    override fun migrate() {
        logger.info("Migrerer database")
        HikariDataSource(migrationConfig).use {
            Flyway.configure()
                .dataSource(it)
                .lockRetryCount(-1)
                .load()
                .migrate()
        }
        logger.info("Migrering ferdig!")
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(DataSourceBuilder::class.java)
    }
}
