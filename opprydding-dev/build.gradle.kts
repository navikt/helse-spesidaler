private val cloudSqlVersion = "1.21.0"
private val postgresqlVersion = "42.7.7"
val hikariCPVersion = "6.3.0"
private val kotliqueryVersion = "1.9.0"

val rapidsAndRiversVersion: String by project
val tbdLibsVersion: String by project

dependencies {
    api("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")

    implementation("com.google.cloud.sql:postgres-socket-factory:$cloudSqlVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")
    implementation("com.zaxxer:HikariCP:$hikariCPVersion")

    testImplementation("com.github.navikt.tbd-libs:rapids-and-rivers-test:$tbdLibsVersion")
    testImplementation("com.github.navikt.tbd-libs:postgres-testdatabaser:${tbdLibsVersion}")
}
