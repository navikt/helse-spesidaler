plugins {
    id("no.nav.helse.sas.sas-deployable")
}

sasDeployable {
    mainClass = "no.nav.helse.spesidaler.opprydding_dev.AppKt"
    imageName = "helse-spesidaler-opprydding-dev"
}

dependencies {
    implementation(libs.rapids.and.rivers)
    implementation(libs.cloud.sql.postgres.socket.factory)
    implementation(libs.postgresql)
    implementation(libs.kotliquery)
    implementation(libs.hikaricp)

    testImplementation(libs.tbd.libs.rapids.and.rivers.test)
    testImplementation(libs.tbd.libs.postgres.testdatabaser)
}
