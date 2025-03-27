val tbdLibsVersion: String by project
val logbackClassicVersion = "1.5.16"
val logbackEncoderVersion = "8.0"
val jacksonVersion = "2.18.3"
val ktorVersion = "3.1.2" // bør være samme som i <com.github.navikt.tbd-libs:naisful-app>
val mockkVersion = "1.13.17"
val flywayCoreVersion = "11.5.0"
val hikariCPVersion = "6.3.0"
val postgresqlVersion = "42.7.5"
val jsonAssertVersion = "1.5.3"

dependencies {
    api("ch.qos.logback:logback-classic:$logbackClassicVersion")
    api("net.logstash.logback:logstash-logback-encoder:$logbackEncoderVersion")

    api("io.ktor:ktor-server-auth:$ktorVersion")
    api("io.ktor:ktor-server-auth-jwt:$ktorVersion") {
        exclude(group = "junit")
    }

    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    api("com.github.navikt.tbd-libs:naisful-app:$tbdLibsVersion")
    api("org.flywaydb:flyway-database-postgresql:$flywayCoreVersion")
    implementation("com.zaxxer:HikariCP:$hikariCPVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.github.navikt.tbd-libs:sql-dsl:$tbdLibsVersion")

    testImplementation("com.github.navikt.tbd-libs:postgres-testdatabaser:$tbdLibsVersion")
    testImplementation("com.github.navikt.tbd-libs:naisful-test-app:$tbdLibsVersion")
    testImplementation("com.github.navikt.tbd-libs:signed-jwt-issuer-test:$tbdLibsVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonAssertVersion")
}

tasks {
    withType<Test> {
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
        systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
        systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "4")
    }
}
