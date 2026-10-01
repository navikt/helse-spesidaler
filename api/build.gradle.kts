plugins {
    id("no.nav.sykepenger.deployable")
}

sykepengerDeployable {
    mainClass = "no.nav.helse.spesidaler.api.AppKt"
    imageName = "helse-spesidaler-api"
}

dependencies {
    implementation(libs.bundles.logback)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt) {
        exclude(group = "junit")
    }
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.tbd.libs.naisful.app)
    implementation(libs.tbd.libs.sql.dsl)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.postgresql)

    testImplementation(libs.tbd.libs.postgres.testdatabaser)
    testImplementation(libs.tbd.libs.naisful.test.app)
    testImplementation(libs.tbd.libs.signed.jwt.issuer.test) {
        // Standard-wiremock kjører på Jetty 11, som ikke finnes i Jetty-BOM-en fra no.nav.sykepenger.kotlin
        exclude(group = "org.wiremock", module = "wiremock")
    }
    testImplementation(libs.wiremock)
    testImplementation(libs.jsonassert)
}
